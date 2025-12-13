@echo off
REM Run Spring Boot with 'ref' profile to generate reference schema

echo ========================================
echo Running Spring Boot with 'ref' profile
echo This will create schema in capstone_reference DB
echo ========================================
echo.
echo Press Ctrl+C to stop after schema is created
echo (Wait for "Started CapstoneProjectApplication" message)
echo.

gradlew bootRun --args="--spring.profiles.active=ref"

pause
