@echo off
setlocal enabledelayedexpansion

:: =============================================================================
::  GigShield — Full Stack Startup Script (Windows)
::  Guidewire DEVTrails 2026 | Sri Eshwar College of Engineering
::
::  Starts all 5 servers:
::    1. Spring Boot Backend        -> http://localhost:8080
::    2. Trigger Engine             -> http://localhost:5001
::    3. Mock Platform API          -> http://localhost:5002
::    4. ML Payout Calculator       -> http://localhost:5003
::    5. React Frontend             -> http://localhost:5173
::
::  Usage: Double-click start.bat OR run from terminal
::
::  Prerequisites:
::    - Java 17+  (java -version)
::    - Maven 3.8+ (mvn -version)
::    - Python 3.11+ (python --version)
::    - Node.js 18+ (node --version)
::    - PostgreSQL 15 running on port 5432
::    - Redis (docker run -d -p 6379:6379 redis:7-alpine)
:: =============================================================================

title GigShield Startup

echo.
echo  ============================================================
echo   GigShield - AI-Powered Parametric Insurance
echo   Guidewire DEVTrails 2026 ^| Sri Eshwar College of Engineering
echo  ============================================================
echo.

:: ── CREATE LOGS DIRECTORY ────────────────────────────────────────────────────
set SCRIPT_DIR=%~dp0
set LOG_DIR=%SCRIPT_DIR%logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: ── PREREQUISITE CHECKS ──────────────────────────────────────────────────────
echo [CHECK] Verifying prerequisites...
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Install Java 17+ from https://adoptium.net
    pause & exit /b 1
)
echo [OK] Java found

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Install Maven from https://maven.apache.org
    pause & exit /b 1
)
echo [OK] Maven found

where python >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Install Python 3.11+ from https://python.org
    pause & exit /b 1
)
echo [OK] Python found

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Install Node.js 18+ from https://nodejs.org
    pause & exit /b 1
)
echo [OK] Node.js found

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm not found.
    pause & exit /b 1
)
echo [OK] npm found

echo.
echo [CHECK] All prerequisites satisfied!
echo.

:: ── PYTHON VIRTUAL ENVIRONMENT ───────────────────────────────────────────────
echo [SETUP] Setting up Python virtual environment...

set VENV_DIR=%SCRIPT_DIR%venv

if not exist "%VENV_DIR%" (
    echo [SETUP] Creating virtual environment...
    python -m venv "%VENV_DIR%"
    echo [OK] Virtual environment created
) else (
    echo [OK] Virtual environment already exists
)

call "%VENV_DIR%\Scripts\activate.bat"

echo [SETUP] Installing Python dependencies...
pip install -r "%SCRIPT_DIR%requirements.txt" -q
echo [OK] Python dependencies installed
echo.

:: ── NODE DEPENDENCIES ────────────────────────────────────────────────────────
echo [SETUP] Installing Node.js dependencies...
cd "%SCRIPT_DIR%devtrails-web-frontend"
call npm install --silent
echo [OK] Node modules installed
cd "%SCRIPT_DIR%"
echo.

:: ── MAVEN BUILD ──────────────────────────────────────────────────────────────
echo [BUILD] Building Spring Boot backend...
cd "%SCRIPT_DIR%backend\backend"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed. Check output above.
    pause & exit /b 1
)
echo [OK] Spring Boot build successful
cd "%SCRIPT_DIR%"
echo.

:: ── START ALL SERVERS IN SEPARATE WINDOWS ────────────────────────────────────
echo [START] Launching all 5 servers...
echo.

:: 1. Spring Boot Backend
echo [1/5] Starting Spring Boot Backend (port 8080)...
start "GigShield - Spring Boot [8080]" cmd /k "cd /d %SCRIPT_DIR%backend\backend && java -jar target\backend-0.0.1-SNAPSHOT.jar --spring.jpa.hibernate.ddl-auto=update > %LOG_DIR%\springboot.log 2>&1 & type %LOG_DIR%\springboot.log"
timeout /t 3 /nobreak >nul

:: 2. Mock Platform API
echo [2/5] Starting Mock Platform API (port 5002)...
start "GigShield - Mock Platform API [5002]" cmd /k "cd /d %SCRIPT_DIR%Trigger_System && call %VENV_DIR%\Scripts\activate.bat && python mock_platform_api.py"
timeout /t 2 /nobreak >nul

:: 3. ML Payout Calculator
echo [3/5] Starting ML Payout Calculator (port 5003)...
start "GigShield - ML Payout API [5003]" cmd /k "cd /d %SCRIPT_DIR%Premium_Calculation && call %VENV_DIR%\Scripts\activate.bat && python app.py"
timeout /t 2 /nobreak >nul

:: 4. Trigger Engine
echo [4/5] Starting Trigger Engine (port 5001)...
start "GigShield - Trigger Engine [5001]" cmd /k "cd /d %SCRIPT_DIR%Trigger_System && call %VENV_DIR%\Scripts\activate.bat && python app.py"
timeout /t 2 /nobreak >nul

:: 5. React Frontend
echo [5/5] Starting React Frontend (port 5173)...
start "GigShield - React Frontend [5173]" cmd /k "cd /d %SCRIPT_DIR%devtrails-web-frontend && npm run dev"
timeout /t 3 /nobreak >nul

:: ── DONE ─────────────────────────────────────────────────────────────────────
echo.
echo  ============================================================
echo   All GigShield servers are starting up!
echo  ============================================================
echo.
echo   Service                  URL
echo   ─────────────────────────────────────────────────────────
echo   React Frontend           http://localhost:5173
echo   Spring Boot Backend      http://localhost:8080
echo   Trigger Engine           http://localhost:5001
echo   Mock Platform API        http://localhost:5002
echo   ML Payout Calculator     http://localhost:5003
echo.
echo   Admin Dashboard          http://localhost:5173/admin/login
echo   Admin credentials        admin / gigshield@admin2026
echo.
echo   Logs saved to:           %LOG_DIR%
echo.
echo   NOTE: Spring Boot and ML API may take 30-60 seconds to fully start.
echo   NOTE: Each server runs in its own terminal window.
echo   Close all terminal windows to stop the servers.
echo  ============================================================
echo.

:: Open browser after a delay
echo [INFO] Opening browser in 10 seconds...
timeout /t 10 /nobreak >nul
start http://localhost:5173

pause
