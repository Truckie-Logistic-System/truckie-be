@echo off
REM Generate diff changelog between reference DB and current DB

echo ========================================
echo Generating Diff Changelog
echo Reference: capstone_reference (from entities)
echo Target: capstone-project (current DB)
echo ========================================

REM Create diff directory if not exists
if not exist "src\main\resources\db\changelog\diff" mkdir "src\main\resources\db\changelog\diff"

REM Generate diff using Liquibase CLI
liquibase ^
    --changeLogFile=src/main/resources/db/changelog/diff/db.changelog-diff.xml ^
    --url=jdbc:postgresql://localhost:5432/capstone-project ^
    --username=postgres ^
    --password=postgres ^
    --referenceUrl=jdbc:postgresql://localhost:5432/capstone_reference ^
    --referenceUsername=postgres ^
    --referencePassword=postgres ^
    diffChangeLog

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Diff changelog generated successfully!
    echo File: src/main/resources/db/changelog/diff/db.changelog-diff.xml
    echo ========================================
    echo.
    echo IMPORTANT: Review this file carefully!
    echo - Look for dropTable, dropColumn (RISKY)
    echo - Separate safe changes from risky ones
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to generate diff!
    echo.
    echo Make sure:
    echo 1. Reference DB exists and has schema
    echo 2. Liquibase CLI is installed
    echo 3. Both databases are accessible
    echo ========================================
)

pause
