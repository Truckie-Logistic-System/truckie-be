-- Fix CategoryPricingDetail for existing categories with fixed IDs
-- The original migration used gen_random_uuid() but actual categories use fixed IDs:
-- NORMAL: 11111111-1111-1111-1111-111111111111
-- FRAGILE: 22222222-2222-2222-2222-222222222222

-- Step 1: Delete any existing category_pricing_detail for these categories
DELETE FROM category_pricing_detail 
WHERE category_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222'
);

-- Step 2: Insert correct pricing for NORMAL category (multiplier=1.0, extraFee=0)
INSERT INTO category_pricing_detail (id, category_id, price_multiplier, extra_fee, created_at, modified_at, created_by, modified_by)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    1.0,
    0,
    NOW(),
    NOW(),
    'system',
    'system'
);

-- Step 3: Insert correct pricing for FRAGILE category (multiplier=1.2, extraFee=20000)
INSERT INTO category_pricing_detail (id, category_id, price_multiplier, extra_fee, created_at, modified_at, created_by, modified_by)
VALUES (
    gen_random_uuid(),
    '22222222-2222-2222-2222-222222222222',
    1.2,
    20000,
    NOW(),
    NOW(),
    'system',
    'system'
);

-- Verify:
-- NORMAL: multiplier=1.0, extraFee=0 → price stays the same
-- FRAGILE: multiplier=1.2, extraFee=20000 → price × 1.2 + 20,000 VND
