-- Fix FRAGILE size rules where columns were swapped during initial data migration
-- Issue: max_weight <-> min_width and max_height <-> min_length were swapped

-- First, let's verify we're targeting the right records
-- FRAGILE category has ID: 22222222-2222-2222-2222-222222222222

-- Swap max_weight with min_width, and max_height with min_length for FRAGILE records
UPDATE size_rules 
SET 
    max_weight = min_width,
    min_width = max_weight,
    max_height = min_length,
    min_length = max_height
WHERE category_id = '22222222-2222-2222-2222-222222222222';

-- Verify the fix by logging the results
-- Expected: 10 ton truck should have max_weight = 10.00, not 2.35
