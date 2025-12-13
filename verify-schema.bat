@echo off
REM Verify schema matches entities

echo ========================================
echo Verifying Schema vs Entities
echo ========================================

REM Temporarily set ddl-auto to validate
echo Testing with validate mode...

REM Create temporary properties file
echo spring.jpa.hibernate.ddl-auto=validate > temp-validate.properties
echo spring.liquibase.enabled=false >> temp-validate.properties

REM Run app with validate mode
gradlew bootRun --args="--spring.config.additional-location=file:./temp-validate.properties"

REM Clean up
del temp-validate.properties

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Schema validation PASSED!
    echo Database is in sync with entities
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Schema validation FAILED!
    echo There are mismatches between DB and entities
    echo ========================================
)

pause
