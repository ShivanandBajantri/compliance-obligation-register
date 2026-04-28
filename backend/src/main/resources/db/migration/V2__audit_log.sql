CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    entity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);