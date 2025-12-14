@echo off
REM Sync database jdbc:postgresql://14.225.253.8:5432/truckie with entities
REM This script uses Liquibase to generate and apply changes

echo ========================================
echo Database Sync Process for Truckie DB
echo Target: jdbc:postgresql://14.225.253.8:5432/truckie
echo ========================================

REM Step 1: Generate diff changelog
echo.
echo Step 1: Generating diff changelog...
echo ========================================

liquibase ^
    --changeLogFile=src/main/resources/db/changelog/diff/db.changelog-sync-truckie.xml ^
    --url=jdbc:postgresql://14.225.253.8:5432/truckie ^
    --username=postgres ^
    --password=123 ^
    --referenceUrl=hibernate:spring:capstone_project?dialect=org.hibernate.dialect.PostgreSQLDialect^&hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy ^
    --referenceUsername= ^
    --referencePassword= ^
    diffChangeLog

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR: Failed to generate diff!
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo Diff changelog generated successfully!
echo File: src/main/resources/db/changelog/diff/db.changelog-sync-truckie.xml
echo ========================================

REM Step 2: Review changes
echo.
echo Step 2: Please review the generated changelog
echo ========================================
echo.
echo IMPORTANT: Review this file carefully!
echo - Look for dropTable, dropColumn (RISKY)
echo - Verify all changes are expected
echo.
pause

REM Step 3: Apply changes
echo.
echo Step 3: Applying changes to database...
echo ========================================

liquibase ^
    --changeLogFile=src/main/resources/db/changelog/diff/db.changelog-sync-truckie.xml ^
    --url=jdbc:postgresql://14.225.253.8:5432/truckie ^
    --username=postgres ^
    --password=123 ^
    update

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Database sync completed successfully!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to apply changes!
    echo ========================================
)

pause
