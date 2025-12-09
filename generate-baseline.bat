@echo off
REM Generate baseline changelog from current database

echo ========================================
echo Generating Baseline Changelog
echo From: capstone-project (current DB)
echo ========================================

REM Create baseline directory if not exists
if not exist "src\main\resources\db\changelog\baseline" mkdir "src\main\resources\db\changelog\baseline"

REM Generate baseline using Liquibase
gradlew liquibaseGenerateChangelog ^
    -PrunList=generateBaseline ^
    -PliquibaseUrl=jdbc:postgresql://localhost:5432/capstone-project ^
    -PliquibaseUsername=postgres ^
    -PliquibasePassword=postgres ^
    -PliquibaseChangeLogFile=src/main/resources/db/changelog/baseline/db.changelog-baseline.xml

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Baseline generated successfully!
    echo File: src/main/resources/db/changelog/baseline/db.changelog-baseline.xml
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to generate baseline!
    echo ========================================
)

pause
