-- Add ORDER_REJECTION issue type if not exists
INSERT INTO issue_types (id, issue_type_name, issue_category, is_active, created_at)
SELECT 
    gen_random_uuid(),
    'Khach hang tu choi nhan hang',
    'ORDER_REJECTION',
    true,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM issue_types 
    WHERE issue_category = 'ORDER_REJECTION' AND is_active = true
);

-- Also add DAMAGE issue type if not exists (for completeness)
INSERT INTO issue_types (id, issue_type_name, issue_category, is_active, created_at)
SELECT 
    gen_random_uuid(),
    'Hang hoa bi hu hong',
    'DAMAGE',
    true,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM issue_types 
    WHERE issue_category = 'DAMAGE' AND is_active = true
);
