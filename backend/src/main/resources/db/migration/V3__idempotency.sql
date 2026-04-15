CREATE TABLE idempotency_record (
    key_hash         VARCHAR(128) NOT NULL,
    scope            VARCHAR(64)  NOT NULL,
    principal        VARCHAR(64)  NOT NULL,
    request_hash     VARCHAR(128) NOT NULL,
    response_status  INTEGER,
    response_json    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (key_hash, scope)
);

CREATE INDEX idx_idempotency_expiry ON idempotency_record (expires_at);
