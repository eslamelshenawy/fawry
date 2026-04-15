-- =====================================================================
-- Fawry Payment Routing — initial schema
-- =====================================================================

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    biller_code     VARCHAR(64),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE biller (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gateway (
    id                       BIGSERIAL PRIMARY KEY,
    code                     VARCHAR(64)    NOT NULL UNIQUE,
    name                     VARCHAR(255)   NOT NULL,
    fixed_fee                NUMERIC(15, 2) NOT NULL,
    percentage_fee           NUMERIC(7, 4)  NOT NULL,
    daily_limit              NUMERIC(15, 2) NOT NULL,
    min_transaction          NUMERIC(15, 2) NOT NULL,
    max_transaction          NUMERIC(15, 2),
    processing_time_minutes  INTEGER        NOT NULL,
    available_24x7           BOOLEAN        NOT NULL DEFAULT TRUE,
    available_days           VARCHAR(64),
    available_from_hour      INTEGER,
    available_to_hour        INTEGER,
    active                   BOOLEAN        NOT NULL DEFAULT TRUE,
    version                  BIGINT         NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fees_non_negative
        CHECK (fixed_fee >= 0 AND percentage_fee >= 0),
    CONSTRAINT chk_min_max_transaction
        CHECK (max_transaction IS NULL OR max_transaction >= min_transaction),
    CONSTRAINT chk_processing_time_non_negative
        CHECK (processing_time_minutes >= 0),
    CONSTRAINT chk_hours_when_not_24x7
        CHECK (available_24x7 OR
              (available_from_hour BETWEEN 0 AND 23
               AND available_to_hour   BETWEEN 0 AND 23))
);

CREATE TABLE biller_quota_usage (
    id           BIGSERIAL PRIMARY KEY,
    biller_id    BIGINT         NOT NULL REFERENCES biller(id),
    gateway_id   BIGINT         NOT NULL REFERENCES gateway(id),
    usage_date   DATE           NOT NULL,
    amount_used  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_quota_usage UNIQUE (biller_id, gateway_id, usage_date)
);

CREATE INDEX idx_quota_usage_lookup
    ON biller_quota_usage (biller_id, gateway_id, usage_date);

CREATE TABLE transaction (
    id                BIGSERIAL PRIMARY KEY,
    biller_id         BIGINT         NOT NULL REFERENCES biller(id),
    gateway_id        BIGINT         NOT NULL REFERENCES gateway(id),
    amount            NUMERIC(15, 2) NOT NULL,
    commission        NUMERIC(15, 2) NOT NULL,
    total_charged     NUMERIC(15, 2) NOT NULL,
    status            VARCHAR(32)    NOT NULL,
    urgency           VARCHAR(16)    NOT NULL,
    split_group_id    UUID,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tx_amount_positive    CHECK (amount > 0),
    CONSTRAINT chk_tx_commission_non_neg CHECK (commission >= 0)
);

CREATE INDEX idx_tx_biller_date
    ON transaction (biller_id, created_at DESC);

CREATE INDEX idx_tx_split_group
    ON transaction (split_group_id);
