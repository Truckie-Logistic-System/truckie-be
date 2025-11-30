-- Migration: Add new columns with clear unit names, copy data, drop old columns
-- Author: System
-- Date: 2025-01-19
-- Description: 
--   1. journey_segments: distance_meters (old) → distance_kilometers (new, stores km)
--   2. order_details: weight (old) → weight_tons (new, stores tons)

-- =====================================================
-- 1. Journey Segments: distance_meters → distance_kilometers
-- =====================================================

-- Step 1: Add new column
ALTER TABLE journey_segments 
ADD COLUMN distance_kilometers INTEGER;

-- Step 2: Copy data from old column to new column
UPDATE journey_segments 
SET distance_kilometers = distance_meters;

-- Step 3: Drop old column
ALTER TABLE journey_segments 
DROP COLUMN distance_meters;

-- =====================================================
-- 2. Order Details: weight → weight_tons
-- =====================================================

-- Step 1: Add new column
ALTER TABLE order_details 
ADD COLUMN weight_tons NUMERIC;

-- Step 2: Copy data from old column to new column
UPDATE order_details 
SET weight_tons = weight;

-- Step 3: Drop old column
ALTER TABLE order_details 
DROP COLUMN weight;

-- =====================================================
-- Verification queries (for manual checking)
-- =====================================================
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'journey_segments' AND column_name LIKE '%distance%';
--
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'order_details' AND column_name LIKE '%weight%';
