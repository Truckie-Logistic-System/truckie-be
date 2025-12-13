-- Adjust order sizes for better bin packing efficiency
-- Problem: LONG_ITEM maxLength (3.0m) is too long for most trucks
-- Solution: Reduce to more practical dimensions

-- Update LONG_ITEM: Reduce maxLength from 3.0m to 2.5m
-- This makes it fit better in smaller trucks while still representing long items
UPDATE order_sizes 
SET max_length = 2.5
WHERE description = 'LONG_ITEM' 
  AND status = 'ACTIVE'
  AND max_length = 3.0;

-- Update SMALL_PARCEL: Slightly increase max dimensions for more variety
-- Current: 0.05-0.4m, New: 0.05-0.5m
UPDATE order_sizes 
SET max_length = 0.5, max_width = 0.4, max_height = 0.4
WHERE description = 'SMALL_PARCEL' 
  AND status = 'ACTIVE'
  AND max_length = 0.4;

-- Update MEDIUM_BOX: Increase max dimensions slightly
-- Current: 0.41-0.8m, New: 0.4-0.9m  
UPDATE order_sizes 
SET min_length = 0.4, max_length = 0.9,
    max_width = 0.7, max_height = 0.7
WHERE description = 'MEDIUM_BOX' 
  AND status = 'ACTIVE'
  AND max_length = 0.8;

-- Update LARGE_BOX: Adjust to be more cubic for better stacking
-- Current: 0.81-1.2m × 0.61-0.8m × 0.61-1.0m
-- New: 0.8-1.2m × 0.8-1.0m × 0.8-1.2m (more cubic)
UPDATE order_sizes 
SET min_length = 0.8, max_length = 1.2,
    min_width = 0.8, max_width = 1.0,
    min_height = 0.8, max_height = 1.2
WHERE description = 'LARGE_BOX' 
  AND status = 'ACTIVE'
  AND min_length = 0.81;

-- Notes:
-- 1. Order sizes now better match Vietnamese logistics standards
-- 2. LONG_ITEM reduced to 2.5m max (fits in more trucks)
-- 3. Boxes made more cubic for better stacking efficiency
-- 4. All changes maintain backward compatibility with existing orders
