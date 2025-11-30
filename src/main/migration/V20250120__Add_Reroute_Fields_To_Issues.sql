-- Migration to add REROUTE specific fields to issues table
-- For rerouting flow when driver encounters problem on a specific journey segment

ALTER TABLE issues
ADD COLUMN IF NOT EXISTS affected_segment_id UUID,
ADD COLUMN IF NOT EXISTS rerouted_journey_id UUID;

-- Add foreign key constraints
ALTER TABLE issues
ADD CONSTRAINT fk_issues_affected_segment
    FOREIGN KEY (affected_segment_id)
    REFERENCES journey_segments(id)
    ON DELETE SET NULL;

ALTER TABLE issues
ADD CONSTRAINT fk_issues_rerouted_journey
    FOREIGN KEY (rerouted_journey_id)
    REFERENCES journey_history(id)
    ON DELETE SET NULL;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_issues_affected_segment ON issues(affected_segment_id);
CREATE INDEX IF NOT EXISTS idx_issues_rerouted_journey ON issues(rerouted_journey_id);

-- Add comments
COMMENT ON COLUMN issues.affected_segment_id IS 'Journey segment ID that has problem (for REROUTE category)';
COMMENT ON COLUMN issues.rerouted_journey_id IS 'New journey created after rerouting (for REROUTE category)';
