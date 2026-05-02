-- Add email notification fields to compliance_obligation table
ALTER TABLE compliance_obligation ADD COLUMN assigned_email VARCHAR(255);
ALTER TABLE compliance_obligation ADD COLUMN alert_sent BOOLEAN DEFAULT FALSE;

-- Create index for email queries
CREATE INDEX idx_assigned_email ON compliance_obligation(assigned_email);