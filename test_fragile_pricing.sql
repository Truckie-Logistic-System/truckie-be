-- Test script to verify FRAGILE pricing fix
-- Run this in PostgreSQL to verify migrations were applied correctly

-- 1. Check if FRAGILE basing_prices exist and match NORMAL prices
SELECT 
    sr_normal.size_rule_name as normal_vehicle,
    sr_fragile.size_rule_name as fragile_vehicle,
    bp_normal.base_price as normal_base_price,
    bp_fragile.base_price as fragile_base_price,
    dr.from_km || '-' || COALESCE(dr.to_km::text, '∞') as distance_range,
    CASE WHEN bp_fragile.base_price = bp_normal.base_price THEN '✅ MATCH' ELSE '❌ DIFFERENT' END as price_match
FROM basing_prices bp_normal
JOIN size_rules sr_normal ON bp_normal.size_rule_id = sr_normal.id
JOIN size_rules sr_fragile ON sr_normal.vehicle_type_id = sr_fragile.vehicle_type_id
JOIN basing_prices bp_fragile ON bp_fragile.size_rule_id = sr_fragile.id AND bp_normal.distance_rule_id = bp_fragile.distance_rule_id
JOIN distance_rules dr ON bp_normal.distance_rule_id = dr.id
WHERE sr_normal.category_id = '11111111-1111-1111-1111-111111111111'
  AND sr_fragile.category_id = '22222222-2222-2222-2222-222222222222'
ORDER BY sr_normal.size_rule_name, dr.from_km;

-- 2. Check category_pricing_detail for correct multipliers
SELECT 
    c.category_name,
    cpd.price_multiplier,
    cpd.extra_fee,
    CASE 
        WHEN c.category_name = 'Hàng thông thường' AND cpd.price_multiplier = 1.0 AND cpd.extra_fee = 0 THEN '✅ CORRECT'
        WHEN c.category_name = 'Hàng dễ vỡ' AND cpd.price_multiplier = 1.2 AND cpd.extra_fee = 20000 THEN '✅ CORRECT'
        ELSE '❌ INCORRECT'
    END as status
FROM categories c
LEFT JOIN category_pricing_detail cpd ON c.id = cpd.category_id
WHERE c.category_name IN ('Hàng thông thường', 'Hàng dễ vỡ');
