-- Migration: Remove legacy damage_* columns from issues table
-- These fields are now stored in issue_compensation_assessment table
-- Date: 2025-12-13

-- Drop legacy damage compensation columns from issues table
ALTER TABLE issues DROP COLUMN IF EXISTS damage_assessment_percent;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_has_documents;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_declared_value;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_estimated_market_value;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_freight_fee;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_legal_limit;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_estimated_loss;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_policy_compensation;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_final_compensation;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_compensation_case;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_adjust_reason;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_handler_note;
ALTER TABLE issues DROP COLUMN IF EXISTS damage_compensation_status;

-- Note: Compensation data is now unified in issue_compensation_assessment table
-- which supports both DAMAGE and OFF_ROUTE issue types
