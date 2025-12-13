@echo off
echo Verifying database schema...
echo.

set PGPASSWORD=npg_bUGrWvZEF86T
psql -U neondb_owner -d neondb -h ep-shiny-mountain-aeqi6kj5-pooler.c-2.us-east-2.aws.neon.tech -c "SELECT column_name FROM information_schema.columns WHERE table_name = 'order_detail' ORDER BY column_name;"
