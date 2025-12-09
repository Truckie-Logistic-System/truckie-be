@echo off
REM Install Liquibase CLI using Chocolatey

echo ========================================
echo Installing Liquibase CLI
echo ========================================

REM Check if Chocolatey is installed
where choco >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Chocolatey not found. Installing Chocolatey first...
    echo.
    echo Please run this command in PowerShell as Administrator:
    echo Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    echo.
    echo After installing Chocolatey, run this script again.
    pause
    exit /b 1
)

REM Install Liquibase
echo Installing Liquibase...
choco install liquibase -y

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Liquibase installed successfully!
    echo ========================================
    echo.
    echo Verify installation:
    liquibase --version
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to install Liquibase!
    echo ========================================
    echo.
    echo Alternative: Download manually from:
    echo https://github.com/liquibase/liquibase/releases
)

pause
