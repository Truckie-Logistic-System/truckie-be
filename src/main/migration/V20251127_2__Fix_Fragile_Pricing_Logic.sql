-- Fix FRAGILE pricing logic - FRAGILE should be MORE expensive than NORMAL, not cheaper
-- Issue: Large vehicles (3.5T+) FRAGILE prices are cheaper than NORMAL
-- Expected: FRAGILE should be 20% more expensive due to careful handling requirements

-- IMPORTANT: The base prices for NORMAL need to be applied to FRAGILE size rules
-- FRAGILE pricing = NORMAL pricing (since category adjustment is applied separately via CategoryPricingDetail)
-- The UnifiedPricingService applies categoryMultiplier and categoryExtraFee AFTER base price lookup

-- Step 1: Delete existing FRAGILE basing_price records
DELETE FROM basing_price 
WHERE size_rule_id IN (
    SELECT id FROM size_rules WHERE category_id = '22222222-2222-2222-2222-222222222222'
);

-- Step 2: Copy NORMAL prices to FRAGILE size rules
-- This uses vehicle_type_id to match corresponding vehicle types
-- The category adjustment (multiplier + extra fee) is applied in code, not in basing_price

INSERT INTO basing_price (id, base_price, size_rule_id, distance_rule_id, created_at, created_by, updated_by)
SELECT 
    gen_random_uuid() as id,
    bp_normal.base_price as base_price,  -- Same base price, adjustment applied in code
    sr_fragile.id as size_rule_id,
    bp_normal.distance_rule_id,
    NOW() as created_at,
    'system' as created_by,
    'system' as updated_by
FROM basing_price bp_normal
JOIN size_rules sr_normal ON bp_normal.size_rule_id = sr_normal.id
JOIN size_rules sr_fragile ON sr_normal.vehicle_type_id = sr_fragile.vehicle_type_id
WHERE sr_normal.category_id = '11111111-1111-1111-1111-111111111111'
  AND sr_fragile.category_id = '22222222-2222-2222-2222-222222222222';

-- Expected results after fix (distance_rule_id = 11111111):
-- FRAGILE 0.5T gets same base price as NORMAL 0.5T: 150,000
-- FRAGILE 1.25T gets same base price as NORMAL 1.25T: 200,000
-- etc.
-- Then CategoryPricingDetail multiplier (1.2) and extra fee (20,000) are applied in code
