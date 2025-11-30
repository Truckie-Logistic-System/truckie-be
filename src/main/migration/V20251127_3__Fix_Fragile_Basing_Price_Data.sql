-- Fix FRAGILE basing_prices data - FRAGILE should use same base price as NORMAL
-- The category price adjustment (multiplier + extra fee) is applied in code via UnifiedPricingService
-- This migration ensures FRAGILE size rules have proper basing_prices records

-- Step 1: Delete existing incorrect FRAGILE basing_prices records
DELETE FROM basing_prices 
WHERE size_rule_id IN (
    SELECT id FROM size_rules WHERE category_id = '22222222-2222-2222-2222-222222222222'
);

-- Step 2: Copy NORMAL prices to FRAGILE size rules using vehicle_type_id to match
INSERT INTO basing_prices (id, base_price, size_rule_id, distance_rule_id, created_at, created_by, modified_by)
SELECT 
    gen_random_uuid() as id,
    bp_normal.base_price as base_price,
    sr_fragile.id as size_rule_id,
    bp_normal.distance_rule_id,
    NOW() as created_at,
    'system' as created_by,
    'system' as modified_by
FROM basing_prices bp_normal
JOIN size_rules sr_normal ON bp_normal.size_rule_id = sr_normal.id
JOIN size_rules sr_fragile ON sr_normal.vehicle_type_id = sr_fragile.vehicle_type_id
WHERE sr_normal.category_id = '11111111-1111-1111-1111-111111111111'
  AND sr_fragile.category_id = '22222222-2222-2222-2222-222222222222';

-- Expected: FRAGILE 5T truck will now have same base prices as NORMAL 5T truck:
-- distance 0-3.99km: 350,000 VND (was 10,260)
-- distance 4-14.99km: 26,000 VND/km (was 13,680)
-- Then CategoryPricingDetail (multiplier=1.2, extraFee=20,000) is applied in code
