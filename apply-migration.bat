@echo off
REM Apply migration to production database

echo ========================================
echo PRODUCTION MIGRATION - PHASE 1 (SAFE)
echo ========================================
echo.
echo This will:
echo 1. Archive data from tables/columns to be dropped
echo 2. Add new constraints
echo 3. Modify data types
echo.
echo WARNING: This will modify production database!
echo Make sure you have:
echo 1. Backed up the database
echo 2. Tested migration on clone database
echo 3. Reviewed all changesets
echo.

set /p CONFIRM="Type 'YES' to continue: "

if /i NOT "%CONFIRM%"=="YES" (
    echo.
    echo Migration cancelled.
    pause
    exit /b 0
)

echo.
echo ========================================
echo Applying migration...
echo ========================================

REM Apply using Spring Boot with migrate profile
gradlew bootRun --args="--spring.profiles.active=migrate"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration applied successfully!
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Verify database schema
    echo 2. Test application
    echo 3. Update ddl-auto to 'validate'
) else (
    echo.
    echo ========================================
    echo ERROR: Migration failed!
    echo ========================================
    echo.
    echo Check logs and database state
    echo You may need to rollback
)

pause
