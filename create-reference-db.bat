@echo off
REM Create reference database from scratch

SET PGPASSWORD=postgres
SET DB_NAME=capstone_reference
SET DB_USER=postgres
SET DB_HOST=localhost
SET DB_PORT=5432

echo ========================================
echo Creating reference database: %DB_NAME%
echo ========================================

REM Drop if exists (để tạo mới hoàn toàn)
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "DROP DATABASE IF EXISTS %DB_NAME%;"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to drop existing database
    pause
    exit /b 1
)

REM Create new database
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %DB_NAME%;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Database created successfully!
    echo ========================================
    echo.
    echo Next step: Run Spring Boot with 'ref' profile
    echo Command: gradlew bootRun --args='--spring.profiles.active=ref'
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to create database!
    echo ========================================
)

pause
