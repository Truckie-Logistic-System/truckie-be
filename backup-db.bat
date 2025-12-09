@echo off
REM Backup PostgreSQL database before migration
REM Usage: Run this script to create a backup

SET PGPASSWORD=postgres
SET DB_NAME=capstone-project
SET DB_USER=postgres
SET DB_HOST=localhost
SET DB_PORT=5432
SET BACKUP_DIR=db-backups
SET TIMESTAMP=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
SET TIMESTAMP=%TIMESTAMP: =0%

REM Create backup directory if not exists
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo ========================================
echo Backing up database: %DB_NAME%
echo Timestamp: %TIMESTAMP%
echo ========================================

REM Full database backup
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -F c -b -v -f "%BACKUP_DIR%\%DB_NAME%_backup_%TIMESTAMP%.backup" %DB_NAME%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Backup completed successfully!
    echo File: %BACKUP_DIR%\%DB_NAME%_backup_%TIMESTAMP%.backup
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Backup failed!
    echo ========================================
    exit /b 1
)

REM Also create SQL dump for easy review
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -F p -b -v -f "%BACKUP_DIR%\%DB_NAME%_backup_%TIMESTAMP%.sql" %DB_NAME%

if %ERRORLEVEL% EQU 0 (
    echo SQL dump also created: %BACKUP_DIR%\%DB_NAME%_backup_%TIMESTAMP%.sql
)

pause
