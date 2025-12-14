@echo off
REM Verify that truckie database is in sync with changelog

echo ========================================
echo Verifying Truckie Database Sync
echo Target: jdbc:postgresql://14.225.253.8:5432/truckie
echo ========================================

echo.
echo Checking database status...
echo.

.\gradlew.bat status -PrunList=syncTruckie

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Database verification completed!
    echo ========================================
    echo.
    echo If you see "0 change sets have not been applied", 
    echo then the database is 100%% in sync with entities.
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to verify database!
    echo ========================================
)

pause
