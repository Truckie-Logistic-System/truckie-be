-- Migration script to synchronize database with code
-- Created on 2025-12-13

-- Drop FCM tokens table if it exists (already handled in previous migration, but added for safety)
DROP TABLE IF EXISTS fcm_tokens CASCADE;

-- Add unique constraint to refunds table
ALTER TABLE refunds ADD CONSTRAINT IF NOT EXISTS refunds_issue_id_key UNIQUE (issue_id);

-- Drop obsolete tables
DROP TABLE IF EXISTS issue_damage_assessment CASCADE;
DROP TABLE IF EXISTS issue_off_route_assessment CASCADE;
DROP TABLE IF EXISTS signature_requests CASCADE;

-- Drop obsolete columns
ALTER TABLE contract_settings DROP COLUMN IF EXISTS expired_deposit_date;
ALTER TABLE contract_settings DROP COLUMN IF EXISTS insurance_rate;
ALTER TABLE orders DROP COLUMN IF EXISTS total_price;
ALTER TABLE contract_rules DROP COLUMN IF EXISTS vehicle_rule_id;

-- Drop obsolete indexes
DROP INDEX IF EXISTS idx_issues_trip_status_at_report;
DROP INDEX IF EXISTS idx_order_seals_seal_code;
DROP INDEX IF EXISTS idx_packing_proof_vehicle_assignment;
DROP INDEX IF EXISTS idx_penalty_history_driver_id;
DROP INDEX IF EXISTS idx_penalty_history_penalty_date;
DROP INDEX IF EXISTS idx_penalty_history_vehicle_assignment_id;
DROP INDEX IF EXISTS idx_transaction_issue_id;
DROP INDEX IF EXISTS idx_vehicle_maintenance_status;

-- Fix data types and constraints
ALTER TABLE packing_proof_images ALTER COLUMN created_at TYPE timestamp;
ALTER TABLE packing_proof_images ALTER COLUMN modified_at TYPE timestamp;
ALTER TABLE packing_proof_images ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE packing_proof_images ALTER COLUMN id DROP DEFAULT;
ALTER TABLE off_route_events ALTER COLUMN grace_period_extension_count DROP DEFAULT;

-- Add foreign key constraints
ALTER TABLE issues 
  DROP CONSTRAINT IF EXISTS fk9i550gtrldhvv8x8sj6irfm6g,
  ADD CONSTRAINT fk9i550gtrldhvv8x8sj6irfm6g 
  FOREIGN KEY (return_journey_id) REFERENCES journey_history(id);

ALTER TABLE packing_proof_images 
  DROP CONSTRAINT IF EXISTS fkopr088kl99xifo5sy2glwgw6c,
  ADD CONSTRAINT fkopr088kl99xifo5sy2glwgw6c 
  FOREIGN KEY (vehicle_assignment_id) REFERENCES vehicle_assignments(id);

-- Remove comments as they're causing issues with the migration
-- We'll focus on the structural changes first
