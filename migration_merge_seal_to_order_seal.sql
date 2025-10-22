-- =====================================================
-- SQL Migration Script: Merge Seal Entity into OrderSeal
-- =====================================================
-- This script migrates data from 'seals' table to 'order_seals' table
-- and removes the 'seals' table completely.
--
-- IMPORTANT: Backup your database before running this script!
-- =====================================================

-- Step 1: Add seal_code column to order_seals table
ALTER TABLE order_seals 
ADD COLUMN IF NOT EXISTS seal_code VARCHAR(255);

-- Step 2: Migrate seal_code from seals to order_seals
-- Update existing order_seals records with seal_code from related seals
UPDATE order_seals os
SET seal_code = s.seal_code
FROM seals s
WHERE os.seal_id = s.id
  AND os.seal_code IS NULL;

-- Step 3: For order_seals without a seal_id, generate a unique seal_code
-- This handles any orphaned records
UPDATE order_seals
SET seal_code = 'MIGRATED-' || id::text
WHERE seal_code IS NULL;

-- Step 4: Verify data migration
-- Check if all order_seals have seal_code
DO $$
DECLARE
    missing_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO missing_count
    FROM order_seals
    WHERE seal_code IS NULL;
    
    IF missing_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % order_seals records still have NULL seal_code', missing_count;
    ELSE
        RAISE NOTICE 'Data migration successful: All order_seals have seal_code';
    END IF;
END $$;

-- Step 5: Remove the foreign key constraint from order_seals to seals
ALTER TABLE order_seals 
DROP CONSTRAINT IF EXISTS fk_order_seals_seal_id;

-- Step 6: Drop the seal_id column from order_seals
ALTER TABLE order_seals 
DROP COLUMN IF EXISTS seal_id;

-- Step 7: Drop the seals table
-- First, drop any foreign key constraints referencing seals table
ALTER TABLE seals 
DROP CONSTRAINT IF EXISTS fk_seals_vehicle_assignment;

-- Now drop the seals table
DROP TABLE IF EXISTS seals;

-- Step 8: Add index on seal_code for better query performance
CREATE INDEX IF NOT EXISTS idx_order_seals_seal_code 
ON order_seals(seal_code);

-- Step 9: Add unique constraint on seal_code to prevent duplicates
-- Note: Only add this if you want to enforce uniqueness
-- ALTER TABLE order_seals 
-- ADD CONSTRAINT uk_order_seals_seal_code UNIQUE (seal_code);

-- =====================================================
-- Verification Queries
-- =====================================================

-- Check the structure of order_seals table
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'order_seals'
ORDER BY ordinal_position;

-- Check if seals table still exists
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'seals'
) AS seals_table_exists;

-- Count records in order_seals
SELECT 
    COUNT(*) as total_records,
    COUNT(seal_code) as records_with_seal_code,
    COUNT(*) - COUNT(seal_code) as records_without_seal_code
FROM order_seals;

-- Sample data from order_seals
SELECT 
    id,
    seal_code,
    description,
    status,
    seal_date,
    vehicle_assignment_id,
    created_at
FROM order_seals
LIMIT 10;

-- =====================================================
-- Rollback Script (Use only if needed to revert changes)
-- =====================================================
/*
-- WARNING: This rollback script should only be used if you have a backup
-- and need to revert the migration. It will NOT restore the original data.

-- Recreate seals table
CREATE TABLE IF NOT EXISTS seals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seal_code VARCHAR(255),
    description TEXT,
    status VARCHAR(20),
    vehicle_assignment_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE
);

-- Add back seal_id column to order_seals
ALTER TABLE order_seals 
ADD COLUMN seal_id UUID;

-- Add back foreign key constraint
ALTER TABLE order_seals 
ADD CONSTRAINT fk_order_seals_seal_id 
FOREIGN KEY (seal_id) REFERENCES seals(id);

-- Remove seal_code column from order_seals
ALTER TABLE order_seals 
DROP COLUMN seal_code;

-- Note: You will need to restore data from backup to fully rollback
*/
