@echo off
REM Clone current DB for testing migration

SET PGPASSWORD=postgres
SET SOURCE_DB=capstone-project
SET TEST_DB=capstone_test
SET DB_USER=postgres
SET DB_HOST=localhost
SET DB_PORT=5432

echo ========================================
echo Cloning database for migration test
echo Source: %SOURCE_DB%
echo Target: %TEST_DB%
echo ========================================

REM Drop test DB if exists
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "DROP DATABASE IF EXISTS %TEST_DB%;"

REM Create empty test DB
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %TEST_DB% OWNER %DB_USER%;"

REM Clone data using pg_dump
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d "%SOURCE_DB%" | psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %TEST_DB%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Test database cloned successfully!
    echo ========================================
    echo.
    echo You can now test migration on: %TEST_DB%
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to clone database!
    echo ========================================
)

pause
