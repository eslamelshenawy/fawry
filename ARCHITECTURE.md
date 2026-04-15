# Fawry Payment Routing Engine — Architecture

## 1. Overview

A backend service that recommends the optimal payment gateway for a given payment
request, tracks per-biller daily quota, persists every routed transaction, and
optionally splits over-limit payments into multiple chunks on the same gateway.

The system is exposed as a Spring Boot REST API consumed by an Angular SPA.

## 2. High-Level Architecture

```
┌──────────────────┐        ┌────────────────────────────────────┐
│   Angular SPA    │  HTTP  │           Spring Boot API           │
│ (Auth, Tx List)  │ <────> │  Controllers → Services → Repos     │
└──────────────────┘        │  JWT filter, Validation, Mapping    │
                            └──────────────────┬─────────────────┘
                                               │ JPA
                                       ┌───────▼────────┐
                                       │   PostgreSQL   │
                                       │ (Flyway-managed)│
                                       └────────────────┘
```

### Layers

| Layer        | Responsibility                                               |
|--------------|--------------------------------------------------------------|
| `controller` | HTTP boundary, request/response DTOs, validation triggers    |
| `service`    | Business orchestration, transactions, security checks        |
| `domain`     | Entities + value objects + enums (rich domain model)         |
| `repository` | Data access (Spring Data JPA + custom queries)               |
| `mapper`     | DTO ↔ Entity conversion (MapStruct)                          |
| `security`   | JWT issuance/validation, authentication filter               |
| `exception`  | Domain exceptions + `@ControllerAdvice` global handler       |
| `config`     | Spring beans, security, OpenAPI, properties binding          |

## 3. Package Structure

```
com.fawry.routing
├── PaymentRoutingApplication
├── config
│   ├── SecurityConfig
│   ├── JwtProperties
│   └── OpenApiConfig
├── domain
│   ├── entity   (Gateway, Biller, Transaction, BillerQuotaUsage, AppUser)
│   ├── enums    (Urgency, TransactionStatus, GatewaySpeed)
│   └── vo       (CommissionRule, AvailabilityWindow)
├── repository
├── dto
│   ├── request  (RecommendRequest, SplitRequest, GatewayRequest, …)
│   └── response (RecommendResponse, TransactionHistoryResponse, …)
├── mapper       (MapStruct interfaces)
├── controller   (PaymentController, GatewayController, TransactionController, AuthController)
├── service
│   ├── GatewayService
│   ├── TransactionService
│   ├── QuotaService
│   ├── routing
│   │   ├── RoutingService            (orchestrator)
│   │   ├── strategy                  (Strategy pattern)
│   │   │   ├── RoutingStrategy
│   │   │   ├── CostOptimizedStrategy   (CAN_WAIT)
│   │   │   ├── SpeedFirstStrategy      (INSTANT)
│   │   │   └── RoutingStrategyFactory
│   │   ├── specification             (Specification pattern)
│   │   │   ├── GatewaySpecification
│   │   │   ├── AmountWithinLimitsSpec
│   │   │   ├── GatewayAvailableSpec
│   │   │   └── QuotaAvailableSpec
│   │   ├── commission
│   │   │   └── CommissionCalculator
│   │   └── splitter
│   │       └── PaymentSplitter
│   └── auth
├── security     (JwtTokenProvider, JwtAuthenticationFilter, AppUserDetailsService)
└── exception    (domain exceptions + GlobalExceptionHandler)
```

## 4. Design Patterns Applied

| Pattern                   | Where                                | Why                                                   |
|---------------------------|--------------------------------------|-------------------------------------------------------|
| Strategy                  | `RoutingStrategy` per `Urgency`      | Swap ranking algorithm without touching callers       |
| Factory                   | `RoutingStrategyFactory`             | Resolve strategy from request urgency                 |
| Specification             | `GatewaySpecification` chain         | Composable, testable filtering of viable gateways     |
| Repository                | Spring Data JPA repos                | Decouple persistence                                  |
| Builder                   | DTOs + complex responses (Lombok)    | Readable construction                                 |
| Template Method           | `AbstractRoutingStrategy`            | Shared filter/score skeleton, custom score per impl   |
| Singleton (Spring)        | All `@Service` / `@Component` beans  | Stateless services managed by container               |

## 5. Domain Model

### Entities

- **Gateway** — `id, code, name, fixedFee, percentageFee, dailyLimit, minTransaction, maxTransaction (nullable), processingTimeMinutes, available24x7, availableDays, availableFromHour, availableToHour, active, version`
- **Biller** — `id, code, name, active`
- **Transaction** — `id, biller, gateway, amount, commission, totalCharged, status, urgency, splitGroupId (nullable), createdAt`
- **BillerQuotaUsage** — `id, biller, gateway, usageDate, amountUsed, version` (unique on biller+gateway+date)
- **AppUser** — `id, username, passwordHash, role, billerId (nullable)`

### Concurrency Strategy

`BillerQuotaUsage` uses **JPA optimistic locking** (`@Version`). Quota is reserved
inside a `@Transactional` block before persisting the transaction; on conflict the
caller retries with a fresh quota snapshot.

### Daily Reset

Quota is naturally per-`usageDate`; "reset at midnight" means simply selecting
today's row, no scheduled job required.

## 6. Routing Algorithm

```
1. Load all active gateways for biller's tenant.
2. Apply Specification chain (filter):
     amountWithinLimits ∧ gatewayAvailableNow ∧ quotaSufficient
3. Pick RoutingStrategy by urgency:
     INSTANT  → SpeedFirstStrategy   (rank by processingTime asc, tie: commission)
     CAN_WAIT → CostOptimizedStrategy (rank by commission asc, tie: speed)
4. Top result → recommendedGateway, rest → alternatives[].
5. If no gateway passes filters → 422 NoEligibleGatewayException.
```

Commission: `fixedFee + amount * percentageFee` (BigDecimal, HALF_UP, scale 2).

## 7. Payment Splitting (Bonus)

```
1. Run routing to pick gateway.
2. If amount ≤ maxTransaction → no split.
3. Else build chunks: floor(amount / max) full chunks + remainder.
4. If remainder > 0 ∧ remainder < minTransaction → rebalance:
     move (minTransaction - remainder) from last full chunk to remainder.
   If rebalance impossible → 422 SplitNotPossibleException.
5. Sum commission per chunk, validate quota covers the full sum.
```

## 8. Security

- Stateless JWT (HS256), 1h access token.
- `SecurityFilterChain`: `/api/auth/**` and `/v3/api-docs/**` permitAll, rest authenticated.
- `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) parses Bearer token,
  populates `SecurityContextHolder`.
- Roles: `ROLE_ADMIN` (gateway CRUD), `ROLE_BILLER` (recommend/split, own history).
- Method-level guard with `@PreAuthorize` on sensitive endpoints.
- Passwords hashed with BCrypt (strength 10).
- Secrets read from env vars, never committed.

## 9. Error Handling

All exceptions flow through `GlobalExceptionHandler` returning a uniform shape:

```json
{ "timestamp": "...", "status": 422, "code": "NO_ELIGIBLE_GATEWAY", "message": "...", "path": "..." }
```

Domain exceptions: `GatewayNotFoundException`, `BillerNotFoundException`,
`NoEligibleGatewayException`, `SplitNotPossibleException`, `QuotaExceededException`,
`InvalidCredentialsException`.

## 10. Persistence & Migrations

- Database: PostgreSQL 15+.
- Schema managed by Flyway (`db/migration/V1__init.sql`, `V2__seed.sql`).
- All amounts stored as `NUMERIC(15,2)`.
- Indexes on `(biller_id, created_at)`, `(biller_id, gateway_id, usage_date)`.

## 11. Testing Strategy

- Unit tests for: `CommissionCalculator`, each `GatewaySpecification`,
  each `RoutingStrategy`, `PaymentSplitter`.
- Slice tests (`@WebMvcTest`) for controllers.
- Integration test for `RoutingService` with H2 / Testcontainers.

## 12. API Surface

| Method | Path                                       | Role        |
|--------|--------------------------------------------|-------------|
| POST   | `/api/auth/login`                          | public      |
| POST   | `/api/auth/register`                       | public      |
| GET    | `/api/gateways`                            | authenticated |
| POST   | `/api/gateways`                            | ADMIN       |
| PUT    | `/api/gateways/{id}`                       | ADMIN       |
| DELETE | `/api/gateways/{id}`                       | ADMIN       |
| POST   | `/api/payments/recommend`                  | BILLER      |
| POST   | `/api/payments/split`                      | BILLER      |
| GET    | `/api/billers/{billerId}/transactions`     | BILLER/ADMIN|
