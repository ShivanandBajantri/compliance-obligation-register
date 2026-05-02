CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    old_data TEXT,
    new_data TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);