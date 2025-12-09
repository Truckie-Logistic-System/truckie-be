-- Verify archive tables
\echo '=== Archive Tables ==='
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name LIKE '%archive%' 
ORDER BY table_name;

\echo ''
\echo '=== Row Counts ==='
SELECT 'issue_damage_assessment_archive' as table_name, COUNT(*) as rows 
FROM issue_damage_assessment_archive
UNION ALL
SELECT 'issue_off_route_assessment_archive', COUNT(*) 
FROM issue_off_route_assessment_archive
UNION ALL
SELECT 'signature_requests_archive', COUNT(*) 
FROM signature_requests_archive
UNION ALL
SELECT 'archived_columns', COUNT(*) 
FROM archived_columns;
