-- Migration to synchronize database structure with entity code
-- This migration handles all the changes found in the diff changelog

-- PHASE 1: Archive data before making changes
-- Create archive tables for tables that will be dropped or modified
DO $$
BEGIN
    -- Archive issue_damage_assessment if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'issue_damage_assessment') THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS issue_damage_assessment_archive AS 
                SELECT *, NOW() as archived_at 
                FROM issue_damage_assessment';
    END IF;

    -- Archive issue_off_route_assessment if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'issue_off_route_assessment') THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS issue_off_route_assessment_archive AS 
                SELECT *, NOW() as archived_at 
                FROM issue_off_route_assessment';
    END IF;

    -- Archive signature_requests if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'signature_requests') THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS signature_requests_archive AS 
                SELECT *, NOW() as archived_at 
                FROM signature_requests';
    END IF;

    -- Create archived_columns table if it doesn't exist
    CREATE TABLE IF NOT EXISTS archived_columns (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        table_name VARCHAR(100),
        column_name VARCHAR(100),
        record_id UUID,
        column_value TEXT,
        archived_at TIMESTAMP DEFAULT NOW()
    );

    -- Archive contract_settings.expired_deposit_date if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_settings' AND column_name = 'expired_deposit_date') THEN
        EXECUTE 'INSERT INTO archived_columns (table_name, column_name, record_id, column_value)
                SELECT ''contract_settings'', ''expired_deposit_date'', id, expired_deposit_date::TEXT
                FROM contract_settings
                WHERE expired_deposit_date IS NOT NULL';
    END IF;

    -- Archive contract_settings.insurance_rate if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_settings' AND column_name = 'insurance_rate') THEN
        EXECUTE 'INSERT INTO archived_columns (table_name, column_name, record_id, column_value)
                SELECT ''contract_settings'', ''insurance_rate'', id, insurance_rate::TEXT
                FROM contract_settings
                WHERE insurance_rate IS NOT NULL';
    END IF;

    -- Archive orders.total_price if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'orders' AND column_name = 'total_price') THEN
        EXECUTE 'INSERT INTO archived_columns (table_name, column_name, record_id, column_value)
                SELECT ''orders'', ''total_price'', id, total_price::TEXT
                FROM orders
                WHERE total_price IS NOT NULL';
    END IF;

    -- Archive contract_rules.vehicle_rule_id if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_rules' AND column_name = 'vehicle_rule_id') THEN
        EXECUTE 'INSERT INTO archived_columns (table_name, column_name, record_id, column_value)
                SELECT ''contract_rules'', ''vehicle_rule_id'', id, vehicle_rule_id::TEXT
                FROM contract_rules
                WHERE vehicle_rule_id IS NOT NULL';
    END IF;
END $$;

-- PHASE 2: Drop foreign key constraints
DO $$
DECLARE
    constraint_exists BOOLEAN;
BEGIN
    -- Drop foreign key constraints from issue_off_route_assessment
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk8ujol3f53oafdabhaxg5dt053' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issue_off_route_assessment DROP CONSTRAINT IF EXISTS fk8ujol3f53oafdabhaxg5dt053;
    END IF;

    -- Drop foreign key constraints from issue_damage_assessment
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkda4relosee251cm1f17nk312i' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issue_damage_assessment DROP CONSTRAINT IF EXISTS fkda4relosee251cm1f17nk312i;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkh6svii373y1rr060gb73v75xe' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issue_damage_assessment DROP CONSTRAINT IF EXISTS fkh6svii373y1rr060gb73v75xe;
    END IF;

    -- Drop foreign key constraint from contract_rules
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkinaprh3r4vbce65un3p5pqvd4' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE contract_rules DROP CONSTRAINT IF EXISTS fkinaprh3r4vbce65un3p5pqvd4;
    END IF;

    -- Drop foreign key constraints from signature_requests
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkjtd6jdhd710q0myaogxg4xh4i' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE signature_requests DROP CONSTRAINT IF EXISTS fkjtd6jdhd710q0myaogxg4xh4i;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkowq7kaqlr6bn1elt07jm2uxf3' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE signature_requests DROP CONSTRAINT IF EXISTS fkowq7kaqlr6bn1elt07jm2uxf3;
    END IF;

    -- Drop foreign key constraint from issues
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk9i550gtrldhvv8x8sj6irfm6g' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issues DROP CONSTRAINT IF EXISTS fk9i550gtrldhvv8x8sj6irfm6g;
    END IF;

    -- Drop foreign key constraint from packing_proof_images
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkopr088kl99xifo5sy2glwgw6c' 
        AND constraint_type = 'FOREIGN KEY'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE packing_proof_images DROP CONSTRAINT IF EXISTS fkopr088kl99xifo5sy2glwgw6c;
    END IF;
END $$;

-- PHASE 3: Add unique constraint to refunds.issue_id if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'refunds_issue_id_key' 
        AND constraint_type = 'UNIQUE'
    ) THEN
        ALTER TABLE refunds ADD CONSTRAINT refunds_issue_id_key UNIQUE (issue_id);
    END IF;
END $$;

-- PHASE 4: Drop unique constraints
DO $$
DECLARE
    constraint_exists BOOLEAN;
BEGIN
    -- Drop unique constraint from issue_off_route_assessment
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'uk678ht8rdr5x4278o2iuq4mbv3' 
        AND constraint_type = 'UNIQUE'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issue_off_route_assessment DROP CONSTRAINT IF EXISTS uk678ht8rdr5x4278o2iuq4mbv3;
    END IF;

    -- Drop unique constraint from issue_damage_assessment
    SELECT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'uklkq75tpwxy3u0nk6tacst6hft' 
        AND constraint_type = 'UNIQUE'
    ) INTO constraint_exists;
    
    IF constraint_exists THEN
        ALTER TABLE issue_damage_assessment DROP CONSTRAINT IF EXISTS uklkq75tpwxy3u0nk6tacst6hft;
    END IF;
END $$;

-- PHASE 5: Drop tables
DO $$
BEGIN
    -- Drop issue_damage_assessment if it exists
    DROP TABLE IF EXISTS issue_damage_assessment;
    
    -- Drop issue_off_route_assessment if it exists
    DROP TABLE IF EXISTS issue_off_route_assessment;
    
    -- Drop signature_requests if it exists
    DROP TABLE IF EXISTS signature_requests;
END $$;

-- PHASE 6: Drop columns
DO $$
BEGIN
    -- Drop expired_deposit_date from contract_settings if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_settings' AND column_name = 'expired_deposit_date') THEN
        ALTER TABLE contract_settings DROP COLUMN expired_deposit_date;
    END IF;

    -- Drop insurance_rate from contract_settings if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_settings' AND column_name = 'insurance_rate') THEN
        ALTER TABLE contract_settings DROP COLUMN insurance_rate;
    END IF;

    -- Drop total_price from orders if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'orders' AND column_name = 'total_price') THEN
        ALTER TABLE orders DROP COLUMN total_price;
    END IF;

    -- Drop vehicle_rule_id from contract_rules if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'contract_rules' AND column_name = 'vehicle_rule_id') THEN
        ALTER TABLE contract_rules DROP COLUMN vehicle_rule_id;
    END IF;
END $$;

-- PHASE 7: Drop indexes
DO $$
BEGIN
    -- Drop index idx_issues_trip_status_at_report if it exists
    DROP INDEX IF EXISTS idx_issues_trip_status_at_report;
    
    -- Drop index idx_order_seals_seal_code if it exists
    DROP INDEX IF EXISTS idx_order_seals_seal_code;
    
    -- Drop index idx_packing_proof_vehicle_assignment if it exists
    DROP INDEX IF EXISTS idx_packing_proof_vehicle_assignment;
    
    -- Drop index idx_penalty_history_driver_id if it exists
    DROP INDEX IF EXISTS idx_penalty_history_driver_id;
    
    -- Drop index idx_penalty_history_penalty_date if it exists
    DROP INDEX IF EXISTS idx_penalty_history_penalty_date;
    
    -- Drop index idx_penalty_history_vehicle_assignment_id if it exists
    DROP INDEX IF EXISTS idx_penalty_history_vehicle_assignment_id;
    
    -- Drop index idx_transaction_issue_id if it exists
    DROP INDEX IF EXISTS idx_transaction_issue_id;
    
    -- Drop index idx_vehicle_maintenance_status if it exists
    DROP INDEX IF EXISTS idx_vehicle_maintenance_status;
END $$;

-- PHASE 8: Add foreign key constraints
DO $$
BEGIN
    -- Add foreign key constraint to issues.return_journey_id
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'issues' AND column_name = 'return_journey_id') 
       AND EXISTS (SELECT 1 FROM information_schema.tables 
                  WHERE table_name = 'journey_history') THEN
        ALTER TABLE issues 
        ADD CONSTRAINT fk9i550gtrldhvv8x8sj6irfm6g 
        FOREIGN KEY (return_journey_id) 
        REFERENCES journey_history(id);
    END IF;

    -- Add foreign key constraint to packing_proof_images.vehicle_assignment_id
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'packing_proof_images' AND column_name = 'vehicle_assignment_id') 
       AND EXISTS (SELECT 1 FROM information_schema.tables 
                  WHERE table_name = 'vehicle_assignments') THEN
        ALTER TABLE packing_proof_images 
        ADD CONSTRAINT fkopr088kl99xifo5sy2glwgw6c 
        FOREIGN KEY (vehicle_assignment_id) 
        REFERENCES vehicle_assignments(id);
    END IF;
END $$;

-- PHASE 9: Modify data types and drop default values
DO $$
BEGIN
    -- Modify data type of packing_proof_images.created_at to timestamp
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'packing_proof_images' AND column_name = 'created_at') THEN
        ALTER TABLE packing_proof_images ALTER COLUMN created_at TYPE timestamp;
        ALTER TABLE packing_proof_images ALTER COLUMN created_at DROP DEFAULT;
    END IF;

    -- Modify data type of packing_proof_images.modified_at to timestamp
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'packing_proof_images' AND column_name = 'modified_at') THEN
        ALTER TABLE packing_proof_images ALTER COLUMN modified_at TYPE timestamp;
    END IF;

    -- Drop default value from off_route_events.grace_period_extension_count
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'off_route_events' AND column_name = 'grace_period_extension_count') THEN
        ALTER TABLE off_route_events ALTER COLUMN grace_period_extension_count DROP DEFAULT;
    END IF;

    -- Drop default value from packing_proof_images.id
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'packing_proof_images' AND column_name = 'id') THEN
        ALTER TABLE packing_proof_images ALTER COLUMN id DROP DEFAULT;
    END IF;
END $$;
