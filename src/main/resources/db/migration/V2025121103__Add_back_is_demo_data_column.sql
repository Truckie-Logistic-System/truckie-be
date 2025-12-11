-- Migration: Add back is_demo_data column that was accidentally dropped
-- This column is part of BaseEntity and required by all entities

ALTER TABLE vehicle_service_record ADD COLUMN IF NOT EXISTS is_demo_data BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN vehicle_service_record.is_demo_data IS 'Flag to mark demo/test data for easy cleanup';
