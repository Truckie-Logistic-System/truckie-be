-- Kiểm tra data trong các bảng sẽ bị drop
SELECT 'issue_damage_assessment' as table_name, COUNT(*) as row_count FROM issue_damage_assessment
UNION ALL
SELECT 'issue_off_route_assessment', COUNT(*) FROM issue_off_route_assessment
UNION ALL
SELECT 'signature_requests', COUNT(*) FROM signature_requests;

-- Kiểm tra data trong các column sẽ bị drop
SELECT 'contract_settings.expired_deposit_date' as column_name, 
       COUNT(*) as total_rows,
       COUNT(expired_deposit_date) as non_null_rows
FROM contract_settings
UNION ALL
SELECT 'contract_settings.insurance_rate',
       COUNT(*),
       COUNT(insurance_rate)
FROM contract_settings
UNION ALL
SELECT 'orders.total_price',
       COUNT(*),
       COUNT(total_price)
FROM orders
UNION ALL
SELECT 'contract_rules.vehicle_rule_id',
       COUNT(*),
       COUNT(vehicle_rule_id)
FROM contract_rules;
