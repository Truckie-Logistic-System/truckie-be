-- Create issue_damage_assessment table
CREATE TABLE IF NOT EXISTS issue_damage_assessment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE REFERENCES issues(id) ON DELETE CASCADE,
    
    -- Document assessment
    has_documents BOOLEAN NOT NULL DEFAULT false,
    document_value DECIMAL(15,2),
    
    -- Damage assessment
    damage_rate DECIMAL(5,4) NOT NULL DEFAULT 0, -- 0 to 1 (0% to 100%)
    
    -- Compensation calculation
    compensation_by_policy DECIMAL(15,2) NOT NULL,
    final_compensation DECIMAL(15,2) NOT NULL,
    
    -- Staff notes
    staff_notes TEXT,
    
    -- Audit
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add index for quick lookup by issue
CREATE INDEX idx_damage_assessment_issue_id ON issue_damage_assessment(issue_id);

-- Add source tracking to refunds table
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS source_id UUID;

-- Add index for refund source tracking
CREATE INDEX IF NOT EXISTS idx_refunds_source ON refunds(source_type, source_id);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_damage_assessment_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_damage_assessment_updated_at
BEFORE UPDATE ON issue_damage_assessment
FOR EACH ROW
EXECUTE FUNCTION update_damage_assessment_updated_at();
