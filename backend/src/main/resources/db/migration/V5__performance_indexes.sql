-- Performance optimization: Add additional indexes for better query performance
-- V5__performance_indexes.sql

-- Composite index for status and due_date (common query pattern)
CREATE INDEX idx_status_due_date ON compliance_obligation(status, due_date);

-- Index for created_at (used in sorting and filtering)
CREATE INDEX idx_created_at ON compliance_obligation(created_at);

-- Index for alert_sent (used in scheduler queries)
CREATE INDEX idx_alert_sent ON compliance_obligation(alert_sent);

-- Composite index for status and alert_sent (used in scheduler)
CREATE INDEX idx_status_alert_sent ON compliance_obligation(status, alert_sent);

-- Index for category (if used in filtering)
CREATE INDEX idx_category ON compliance_obligation(category);