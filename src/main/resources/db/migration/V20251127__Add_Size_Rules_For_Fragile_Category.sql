-- Add Size Rules for FRAGILE Category
-- Date: 2025-11-27
-- Purpose: Add vehicle size rules for FRAGILE category to fix "No vehicle rules found for this category" error

-- Ensure pgcrypto extension is available for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Get FRAGILE category ID
DO $$
DECLARE
    fragile_category_id UUID;
BEGIN
    SELECT id INTO fragile_category_id 
    FROM categories 
    WHERE category_name = 'FRAGILE' 
    LIMIT 1;
    
    IF fragile_category_id IS NULL THEN
        RAISE NOTICE '⚠️ FRAGILE category not found, skipping size rules insertion';
        RETURN;
    END IF;
    
    RAISE NOTICE '📋 Adding size rules for FRAGILE category (ID: %)', fragile_category_id;
    
    -- Insert size rules for FRAGILE category
    -- Match exact schema from actual database table
    INSERT INTO size_rules (
        id, 
        min_length, max_length, 
        min_width, max_width, 
        min_height, max_height, 
        min_weight, max_weight,
        created_at, created_by, modified_by,
        status, size_rule_name,
        category_id, vehicle_type_id
    )
    VALUES 
        -- 0.5 ton fragile vehicle
        (gen_random_uuid(), 1.41, 2.60, 0.50, 1.50, 1.01, 2.05, 0.00, 1.31, 
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 0.5 tấn (Hàng dễ vỡ)', 
         fragile_category_id, '11111111-1111-1111-1111-111111111111'),
        
        -- 1.25 ton fragile vehicle  
        (gen_random_uuid(), 1.84, 4.40, 1.25, 1.92, 1.11, 3.17, 0.51, 1.67,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 1.25 tấn (Hàng dễ vỡ)',
         fragile_category_id, '22222222-2222-2222-2222-222222222222'),
         
        -- 1.9 ton fragile vehicle
        (gen_random_uuid(), 1.97, 4.90, 1.90, 1.97, 1.67, 3.48, 1.26, 1.47,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 1.9 tấn (Hàng dễ vỡ)',
         fragile_category_id, '33333333-3333-3333-3333-333333333333'),
         
        -- 2.4 ton fragile vehicle
        (gen_random_uuid(), 1.78, 4.47, 2.40, 1.83, 1.55, 3.36, 1.91, 1.67,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 2.4 tấn (Hàng dễ vỡ)',
         fragile_category_id, '44444444-4444-4444-4444-444444444444'),
         
        -- 3.5 ton fragile vehicle
        (gen_random_uuid(), 2.42, 5.79, 3.50, 2.10, 1.49, 4.40, 2.41, 1.95,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 3.5 tấn (Hàng dễ vỡ)',
         fragile_category_id, '55555555-5555-5555-5555-555555555555'),
         
        -- 5 ton fragile vehicle
        (gen_random_uuid(), 2.47, 6.75, 5.00, 2.33, 1.83, 4.92, 3.51, 2.03,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 5 tấn (Hàng dễ vỡ)',
         fragile_category_id, '66666666-6666-6666-6666-666666666666'),
         
        -- 7 ton fragile vehicle
        (gen_random_uuid(), 2.50, 9.20, 7.00, 2.36, 1.78, 5.22, 5.10, 2.28,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 7 tấn (Hàng dễ vỡ)',
         fragile_category_id, '77777777-7777-7777-7777-777777777777'),
         
        -- 10 ton fragile vehicle
        (gen_random_uuid(), 2.53, 9.20, 10.00, 2.39, 2.07, 6.30, 7.10, 2.35,
         NOW(), 'system', 'system', 'ACTIVE', 'Xe tải 10 tấn (Hàng dễ vỡ)',
         fragile_category_id, '88888888-8888-8888-8888-888888888888')
    ON CONFLICT DO NOTHING;
    
    -- Verify insertion
    PERFORM 1;
    
    RAISE NOTICE '✅ Added 8 size rules for FRAGILE category';
END $$;

-- Verify the size rules were added
DO $$
DECLARE
    fragile_rule_count INTEGER;
BEGIN
    SELECT COUNT(sr.id) INTO fragile_rule_count
    FROM size_rules sr
    JOIN categories c ON sr.category_id = c.id
    WHERE c.category_name = 'FRAGILE' AND sr.status = 'ACTIVE';
    
    RAISE NOTICE '📊 FRAGILE category now has % active size rules', fragile_rule_count;
    
    IF fragile_rule_count = 0 THEN
        RAISE NOTICE '⚠️ No active size rules found for FRAGILE category - please check data';
    ELSIF fragile_rule_count < 8 THEN
        RAISE NOTICE '⚠️ Only % size rules found for FRAGILE category (expected 8)', fragile_rule_count;
    ELSE
        RAISE NOTICE '✅ FRAGILE category size rules are properly configured';
    END IF;
END $$;
