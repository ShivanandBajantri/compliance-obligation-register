-- V6__audit_log_indexes.sql
-- Performance indexes for audit_log table.
-- These complement the JPA @Index annotations on the AuditLog entity and make
-- queries by entity_id, entity_type, and changed_at efficient.

CREATE INDEX IF NOT EXISTS idx_audit_entity_id   ON audit_log(entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity_type ON audit_log(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_changed_at  ON audit_log(changed_at);
