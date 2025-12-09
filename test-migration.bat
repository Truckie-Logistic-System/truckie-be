@echo off
REM Test migration on cloned database

echo ========================================
echo Testing Migration on Test Database
echo ========================================
echo.
echo Running Spring Boot with 'test' profile...
echo Liquibase will apply migrations to capstone_test DB
echo.

REM Run Spring Boot with test profile (Liquibase will run automatically)
gradlew bootRun --args="--spring.profiles.active=test"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration test SUCCESSFUL!
    echo ========================================
    echo.
    echo You can now apply to production DB
) else (
    echo.
    echo ========================================
    echo Migration test FAILED!
    echo ========================================
    echo.
    echo Fix issues before applying to production
)

pause
