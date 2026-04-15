# Fawry Payment Routing Engine

A smart payment routing service that recommends the optimal payment gateway for a
given biller payment, tracks per-biller daily quota, persists every routed
transaction, and splits over-limit payments across multiple chunks on the same
gateway. Exposed as a Spring Boot REST API and consumed by an Angular 17 SPA.

## Tech Stack

| Layer      | Technology                      |
|------------|---------------------------------|
| Backend    | Java 17, Spring Boot 3.2        |
| Persistence| PostgreSQL 15, Flyway, JPA      |
| Security   | Spring Security + JWT (HS256)   |
| Frontend   | Angular 17 (standalone components) |
| Docs       | springdoc OpenAPI / Swagger UI  |

## Repository Layout

```
.
├── ARCHITECTURE.md          architecture + design-pattern breakdown
├── backend/                 Spring Boot API
├── frontend/                Angular 17 SPA
└── postman/                 Postman collection + environment
```

## Prerequisites

- JDK 17
- Maven 3.9+
- Node.js 18+ and npm 9+
- Docker / Docker Compose (recommended)

## One-command setup (Docker Compose)

```bash
docker compose up --build
```

Brings up PostgreSQL, the Spring Boot API (`:8080`), and the Angular SPA (`:4200`).

## Running components individually

### PostgreSQL only

```bash
docker run --name fawry-pg -p 5432:5432 \
  -e POSTGRES_DB=fawry_routing \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -d postgres:15
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

Flyway applies four migrations on first start (`V1__init`, `V2__seed_data`,
`V3__idempotency`, `V4__audit_columns`). The seed loads three gateways
(GW1 / GW2 / GW3), two demo billers, and two users:

| Username | Password        | Role   | Biller      |
|----------|-----------------|--------|-------------|
| admin    | `Password123!`  | ADMIN  | —           |
| biller1  | `Password123!`  | BILLER | BILL_12345  |

### Configuration

Override through environment variables:

| Variable        | Default                                  |
|-----------------|------------------------------------------|
| `DB_URL`        | `jdbc:postgresql://localhost:5432/fawry_routing` |
| `DB_USERNAME`   | `postgres`                               |
| `DB_PASSWORD`   | `postgres`                               |
| `JWT_SECRET`    | dev-only placeholder                     |
| `SERVER_PORT`   | `8080`                                   |

> Generate a production JWT secret (≥ 256 bits). Never commit it.

### API Documentation

Once the backend is running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Frontend Setup

```bash
cd frontend
npm install
npm start
```

The SPA is served at <http://localhost:4200> and talks to the backend on
`http://localhost:8080/api` (see `frontend/src/environments/environment.ts`).

## Core API Endpoints

| Method | Path                                         | Role         | Purpose                                  |
|--------|----------------------------------------------|--------------|------------------------------------------|
| POST   | `/api/auth/login`                            | public       | Obtain JWT                               |
| POST   | `/api/auth/register`                         | public       | Register a user                          |
| GET    | `/api/gateways`                              | authenticated| List all gateways                        |
| POST   | `/api/gateways`                              | ADMIN        | Create a gateway                         |
| PUT    | `/api/gateways/{id}`                         | ADMIN        | Update a gateway                         |
| DELETE | `/api/gateways/{id}`                         | ADMIN        | Soft-delete a gateway                    |
| POST   | `/api/payments/recommend`                    | ADMIN/BILLER | Recommend + execute a single transaction |
| POST   | `/api/payments/split`                        | ADMIN/BILLER | Recommend + split over-limit payment     |
| GET    | `/api/billers/{billerId}/transactions?date=` | ADMIN/BILLER | Daily transaction history per biller     |

## Routing Algorithm (summary)

1. Load all active gateways.
2. Filter with a composable **Specification** chain:
   `AmountWithinLimitsSpec ∧ GatewayAvailableSpec ∧ QuotaAvailableSpec`.
3. Pick a `RoutingStrategy` via `RoutingStrategyFactory`:
   - `INSTANT`   → `SpeedFirstStrategy`    (speed first, cost tie-breaker)
   - `CAN_WAIT`  → `CostOptimizedStrategy` (cost first, speed tie-breaker)
4. Top gateway becomes `recommendedGateway`; the rest are `alternatives[]`.
5. Reserve daily quota (optimistic locking), persist the transaction.

Commission = `fixedFee + amount × percentageFee`, `BigDecimal`, `HALF_UP`, 2 decimals.

> **Endpoint semantics** — `/api/payments/recommend` is a *recommend-and-execute*
> call: it picks the best gateway, reserves quota, and persists the transaction
> in one atomic unit so the history log and daily quota stay consistent with the
> response. Pure "dry-run" ranking is not exposed as a separate endpoint; the
> transaction history endpoint (`GET /api/billers/{id}/transactions`) is the
> audit surface. Business days roll at midnight `Africa/Cairo`.

See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for the full breakdown.

## Payment Splitting (bonus)

`POST /api/payments/split` runs the routing algorithm, then chunks the amount
into `maxTransaction`-sized pieces with a remainder that still satisfies
`minTransaction` (rebalancing automatically when necessary), validates total
quota, and persists each chunk under the same `splitGroupId`.

## Postman Collection

`postman/fawry-payment-routing.postman_collection.json` covers every endpoint
with sample requests. Import the collection along with the environment
(`postman/fawry-local.postman_environment.json`) and run the `Login` request
first — the test script stores the JWT into `{{accessToken}}`.

## Testing

```bash
cd backend
mvn test                 # unit tests
mvn verify               # + integration tests (requires Docker for Testcontainers)
```

- **Unit tests** cover the commission calculator, payment splitter, and each routing strategy.
- **Controller slice tests** (`@WebMvcTest`) cover validation, security, and error shape.
- **Integration tests** (`@SpringBootTest` + PostgreSQL Testcontainer) exercise the full HTTP flow end to end.

## Production Considerations (implemented)

- **Idempotency** — `POST /api/payments/recommend` and `/api/payments/split` accept an optional
  `Idempotency-Key` header. Repeated requests with the same key replay the stored response;
  same key with a different payload is rejected with `409 CONFLICT`.
- **Optimistic locking + retry** — `BillerQuotaUsage` uses JPA `@Version`; payment methods are
  annotated with `@Retryable` (exponential backoff, 3 attempts) so concurrent quota updates
  survive without surfacing `OptimisticLockingFailureException` to the client.
- **Rate limiting** — Bucket4j filter enforces 10 req/min on `/auth/login` per IP and 60 req/min
  on `/payments/**` per authenticated principal. Returns `429` + `Retry-After` when exhausted.
- **Correlation IDs** — every response carries `X-Request-Id` (echoed from the request when valid,
  otherwise generated). The ID is placed in SLF4J `MDC` and appears in every log line for that
  request.
- **Audit trail** — all mutations to `Gateway` emit an entry in the `audit_log` table capturing
  actor, action (CREATE/UPDATE/DELETE), entity, details, and timestamp. JPA auditing also
  populates `created_by` / `updated_by` columns.
- **Pagination** — transaction history supports `?page=`, `?size=`, and `?sort=`; response
  contains a `page` object with `totalElements`, `totalPages`, and `hasNext`.

## CI

`.github/workflows/ci.yml` runs backend `mvn verify` and a frontend production build on every
push / PR against `main` or `develop`.
