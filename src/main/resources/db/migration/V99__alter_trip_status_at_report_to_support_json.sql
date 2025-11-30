-- ✅ CRITICAL: Alter trip_status_at_report column to support JSON format
-- Change from VARCHAR(20) to VARCHAR(500) to store JSON map of OrderDetail statuses
-- Format: {"orderDetailId1":"STATUS1","orderDetailId2":"STATUS2"}
-- This is needed for combined issue reports where different packages have different statuses

ALTER TABLE issues 
MODIFY COLUMN trip_status_at_report VARCHAR(500);

-- Add comment explaining the format
ALTER TABLE issues 
MODIFY COLUMN trip_status_at_report VARCHAR(500) 
COMMENT 'JSON map of OrderDetail statuses at time of report. Format: {"uuid1":"STATUS1","uuid2":"STATUS2"}';
