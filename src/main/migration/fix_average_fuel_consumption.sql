-- =====================================================
-- FIX: Average Fuel Consumption Values in vehicle_types
-- =====================================================
-- PROBLEM: Current values are 100x smaller than reality
-- Example: 0.085 L/100km should be 8.5 L/100km
--
-- SOLUTION: Multiply all values by 100 to get realistic values
-- =====================================================

-- Backup current values (optional, for safety)
-- CREATE TABLE vehicle_types_backup_fuel AS SELECT * FROM vehicle_types;

-- ✅ Fix average_fuel_consumption_l_per_100km values
-- Multiply by 100 to get realistic fuel consumption rates

UPDATE vehicle_types
SET average_fuel_consumption_l_per_100km = average_fuel_consumption_l_per_100km * 100
WHERE average_fuel_consumption_l_per_100km IS NOT NULL
  AND average_fuel_consumption_l_per_100km < 1.0; -- Only update clearly wrong values

-- Verify the fix (optional check query)
-- SELECT 
--     id,
--     vehicle_type_name,
--     weight_limit_ton,
--     average_fuel_consumption_l_per_100km as fuel_consumption,
--     CASE 
--         WHEN weight_limit_ton <= 1.0 THEN 'Small (8-10 L/100km expected)'
--         WHEN weight_limit_ton <= 3.0 THEN 'Medium-Small (10-14 L/100km expected)'
--         WHEN weight_limit_ton <= 5.0 THEN 'Medium (14-18 L/100km expected)'
--         WHEN weight_limit_ton <= 7.0 THEN 'Large (18-20 L/100km expected)'
--         ELSE 'Very Large (20-25 L/100km expected)'
--     END as category
-- FROM vehicle_types
-- ORDER BY weight_limit_ton;

-- Expected Results After Fix:
-- | Weight Limit | Before (WRONG) | After (CORRECT) | Expected Range |
-- |--------------|----------------|-----------------|----------------|
-- | 0.5 ton      | 0.065          | 6.5             | 8-10 L/100km   |
-- | 1.25 ton     | 0.075          | 7.5             | 10-12 L/100km  |
-- | 1.9 ton      | 0.085          | 8.5             | 11-13 L/100km  |
-- | 2.4 ton      | 0.095          | 9.5             | 12-14 L/100km  |
-- | 3.5 ton      | 0.11           | 11.0            | 14-16 L/100km  |
-- | 5 ton        | 0.135          | 13.5            | 16-18 L/100km  |
-- | 7 ton        | 0.165          | 16.5            | 19-21 L/100km  |
-- | 10 ton       | 0.2            | 20.0            | 22-25 L/100km  |

-- Note: These are still on the LOW side of realistic values
-- You may want to increase them further based on actual vehicle data:
-- UPDATE vehicle_types SET average_fuel_consumption_l_per_100km = average_fuel_consumption_l_per_100km * 1.2
-- WHERE weight_limit_ton > 5; -- Increase large trucks by 20% more

COMMIT;
