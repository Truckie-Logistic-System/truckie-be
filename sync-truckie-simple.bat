@echo off
REM Simple approach: Use Liquibase update directly with current changelog
REM This will sync the truckie DB with the current db.changelog-master.xml

echo ========================================
echo Syncing Truckie DB with Liquibase
echo Target: jdbc:postgresql://14.225.253.8:5432/truckie
echo ========================================

echo.
echo This will apply all pending changesets from db.changelog-master.xml
echo.
pause

liquibase ^
    --changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml ^
    --url=jdbc:postgresql://14.225.253.8:5432/truckie ^
    --username=postgres ^
    --password=123 ^
    --driver=org.postgresql.Driver ^
    update

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Database sync completed successfully!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to sync database!
    echo ========================================
)

pause
