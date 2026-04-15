ALTER TABLE gateway
    ADD COLUMN created_by VARCHAR(64),
    ADD COLUMN updated_by VARCHAR(64);

ALTER TABLE biller
    ADD COLUMN created_by VARCHAR(64);

CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(64)  NOT NULL,
    entity_id    VARCHAR(64)  NOT NULL,
    action       VARCHAR(16)  NOT NULL,
    actor        VARCHAR(64)  NOT NULL,
    details      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_actor  ON audit_log (actor, created_at DESC);
