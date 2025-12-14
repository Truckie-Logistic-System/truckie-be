@echo off
REM Generate diff between reference DB (from entities) and truckie DB
REM This will show what changes are needed to make truckie match entities 100%

echo ========================================
echo Step 1: Creating reference database from entities
echo ========================================

REM Create reference DB
call create-reference-db.bat

echo.
echo ========================================
echo Step 2: Starting Spring Boot to populate reference schema
echo ========================================
echo.
echo Please wait while Spring Boot creates schema from entities...
echo This will take about 30 seconds...
echo.

REM Start Spring Boot with ref profile in background
start /B gradlew.bat bootRun --args="--spring.profiles.active=ref" > ref-boot.log 2>&1

REM Wait for application to start and create schema
timeout /t 35 /nobreak

REM Kill the Spring Boot process
taskkill /F /FI "WINDOWTITLE eq Gradle*" /T >nul 2>&1

echo.
echo ========================================
echo Step 3: Generating diff changelog
echo ========================================

REM Now generate diff using Gradle
gradlew.bat diffChangeLog -PrunList=diffTruckie

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Diff generated successfully!
    echo File: src/main/resources/db/changelog/diff/db.changelog-diff-truckie.xml
    echo ========================================
    echo.
    echo Review this file to see what changes are needed.
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to generate diff!
    echo ========================================
)

pause
