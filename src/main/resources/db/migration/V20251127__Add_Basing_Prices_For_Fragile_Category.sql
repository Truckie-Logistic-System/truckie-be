-- Add Basing Prices for FRAGILE Category
-- Date: 2025-11-27
-- Purpose: Add basing price data for FRAGILE category to support pricing calculations

-- Ensure pgcrypto extension is available for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Insert 32 FRAGILE basing prices (8 size rules × 4 distance rules)
-- Prices are 20% higher than NORMAL category
INSERT INTO basing_prices (id, base_price, created_at, created_by, modified_by, size_rule_id, distance_rule_id) VALUES
-- Distance 0-4km (Base: 150K → 180K for FRAGILE)
(gen_random_uuid(), 180000.00 * 1.0, NOW(), 'system', 'system', '3f8d67cc-d7be-4a8c-9473-c93fa25ad642', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.167, NOW(), 'system', 'system', 'a557f9cf-0f8f-46db-93f3-44798573ce91', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.100, NOW(), 'system', 'system', '1e978f6e-64f7-445e-8312-44e10acd327b', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.080, NOW(), 'system', 'system', 'c2a11ef6-6450-4af8-8ec6-6e964190969c', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.067, NOW(), 'system', 'system', '7bdae0c8-7db0-446b-a1fd-761615d9f4f6', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.057, NOW(), 'system', 'system', '4904c508-3742-4131-8139-c12b39637a35', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.048, NOW(), 'system', 'system', '28eed299-5d89-4545-9ba8-21ab09046ebb', '11111111-1111-1111-1111-111111111111'),
(gen_random_uuid(), 180000.00 * 0.043, NOW(), 'system', 'system', 'da20b324-3372-40a9-9bbc-752fbeca625f', '11111111-1111-1111-1111-111111111111'),

-- Distance 4-15km (Base: 200K → 240K for FRAGILE)
(gen_random_uuid(), 240000.00 * 1.0, NOW(), 'system', 'system', '3f8d67cc-d7be-4a8c-9473-c93fa25ad642', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.167, NOW(), 'system', 'system', 'a557f9cf-0f8f-46db-93f3-44798573ce91', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.100, NOW(), 'system', 'system', '1e978f6e-64f7-445e-8312-44e10acd327b', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.080, NOW(), 'system', 'system', 'c2a11ef6-6450-4af8-8ec6-6e964190969c', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.067, NOW(), 'system', 'system', '7bdae0c8-7db0-446b-a1fd-761615d9f4f6', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.057, NOW(), 'system', 'system', '4904c508-3742-4131-8139-c12b39637a35', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.048, NOW(), 'system', 'system', '28eed299-5d89-4545-9ba8-21ab09046ebb', '22222222-2222-2222-2222-222222222222'),
(gen_random_uuid(), 240000.00 * 0.043, NOW(), 'system', 'system', 'da20b324-3372-40a9-9bbc-752fbeca625f', '22222222-2222-2222-2222-222222222222'),

-- Distance 15-100km (Base: 230K → 276K for FRAGILE)
(gen_random_uuid(), 276000.00 * 1.0, NOW(), 'system', 'system', '3f8d67cc-d7be-4a8c-9473-c93fa25ad642', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.167, NOW(), 'system', 'system', 'a557f9cf-0f8f-46db-93f3-44798573ce91', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.100, NOW(), 'system', 'system', '1e978f6e-64f7-445e-8312-44e10acd327b', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.080, NOW(), 'system', 'system', 'c2a11ef6-6450-4af8-8ec6-6e964190969c', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.067, NOW(), 'system', 'system', '7bdae0c8-7db0-446b-a1fd-761615d9f4f6', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.057, NOW(), 'system', 'system', '4904c508-3742-4131-8139-c12b39637a35', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.048, NOW(), 'system', 'system', '28eed299-5d89-4545-9ba8-21ab09046ebb', '33333333-3333-3333-3333-333333333333'),
(gen_random_uuid(), 276000.00 * 0.043, NOW(), 'system', 'system', 'da20b324-3372-40a9-9bbc-752fbeca625f', '33333333-3333-3333-3333-333333333333'),

-- Distance 100km+ (Base: 260K → 312K for FRAGILE)
(gen_random_uuid(), 312000.00 * 1.0, NOW(), 'system', 'system', '3f8d67cc-d7be-4a8c-9473-c93fa25ad642', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.167, NOW(), 'system', 'system', 'a557f9cf-0f8f-46db-93f3-44798573ce91', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.100, NOW(), 'system', 'system', '1e978f6e-64f7-445e-8312-44e10acd327b', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.080, NOW(), 'system', 'system', 'c2a11ef6-6450-4af8-8ec6-6e964190969c', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.067, NOW(), 'system', 'system', '7bdae0c8-7db0-446b-a1fd-761615d9f4f6', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.057, NOW(), 'system', 'system', '4904c508-3742-4131-8139-c12b39637a35', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.048, NOW(), 'system', 'system', '28eed299-5d89-4545-9ba8-21ab09046ebb', '44444444-4444-4444-4444-444444444444'),
(gen_random_uuid(), 312000.00 * 0.043, NOW(), 'system', 'system', 'da20b324-3372-40a9-9bbc-752fbeca625f', '44444444-4444-4444-4444-444444444444')
ON CONFLICT DO NOTHING;

-- Verify the basing prices were added
DO $$
DECLARE
    fragile_price_count INTEGER;
BEGIN
    -- Count basing prices for FRAGILE category via size rules
    SELECT COUNT(bp.id) INTO fragile_price_count
    FROM basing_prices bp
    JOIN size_rules sr ON bp.size_rule_id = sr.id
    JOIN categories c ON sr.category_id = c.id
    WHERE c.category_name = 'FRAGILE';
    
    RAISE NOTICE '📊 FRAGILE category now has % basing prices', fragile_price_count;
    
    IF fragile_price_count = 0 THEN
        RAISE NOTICE '⚠️ No basing prices found for FRAGILE category - please check data';
    ELSIF fragile_price_count < 32 THEN
        RAISE NOTICE '⚠️ Only % basing prices found for FRAGILE category (expected 32)', fragile_price_count;
    ELSE
        RAISE NOTICE '✅ FRAGILE category basing prices are properly configured';
    END IF;
END $$;
