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

:: Check Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Installing Java 17+...
    echo [INFO] Downloading and installing Java 17...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://download.java.net/java/GA/jdk17/latest/jdk-17_windows-x64_bin.zip' -OutFile 'jdk17.zip'; Expand-Archive 'jdk17.zip' -DestinationPath 'C:\Program Files\Java\jdk17'}"
    setx JAVA_HOME "C:\Program Files\Java\jdk17"
    setx PATH "%PATH%;C:\Program Files\Java\jdk17\bin"
    echo [OK] Java 17 installed
) else (
    echo [OK] Java found
)

:: Check Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Installing Maven...
    echo [INFO] Downloading and installing Maven...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.zip' -OutFile 'maven.zip'; Expand-Archive 'maven.zip' -DestinationPath 'C:\Program Files\Apache\Maven'}"
    setx MAVEN_HOME "C:\Program Files\Apache\Maven\apache-maven-3.9.4"
    setx PATH "%PATH%;C:\Program Files\Apache\Maven\apache-maven-3.9.4\bin"
    echo [OK] Maven installed
) else (
    echo [OK] Maven found
)

:: Check Python
where python >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Installing Python 3.11+...
    echo [INFO] Downloading and installing Python...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://www.python.org/ftp/python/3.11.9/python-3.11.9-amd64.exe' -OutFile 'python-installer.exe'; Start-Process 'python-installer.exe' -Wait}"
    echo [OK] Python installed
) else (
    echo [OK] Python found
)

:: Check Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Installing Node.js 18+...
    echo [INFO] Downloading and installing Node.js...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://nodejs.org/dist/v18.18.0/node-v18.18.0-x64.msi' -OutFile 'nodejs-installer.msi'; Start-Process 'nodejs-installer.msi' -Wait}"
    echo [OK] Node.js installed
) else (
    echo [OK] Node.js found
)

:: Check npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] npm not found. Installing npm...
    powershell -Command "npm install -g npm@latest"
    echo [OK] npm installed
) else (
    echo [OK] npm found
)

:: Check PostgreSQL
netstat -an | findstr :5432 >nul
if %errorlevel% neq 0 (
    echo [WARNING] PostgreSQL not running on port 5432
    echo [INFO] Please start PostgreSQL service or install PostgreSQL 15+
    echo [INFO] Download link: https://www.postgresql.org/download/windows/
)

:: Check Redis
netstat -an | findstr :6379 >nul
if %errorlevel% neq 0 (
    echo [WARNING] Redis not running on port 6379
    echo [INFO] Starting Redis using Docker...
    docker run -d -p 6379:6379 --name gigshield-redis redis:7-alpine
    echo [OK] Redis started
)

echo.
echo [CHECK] Prerequisites check completed!
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
if exist "%SCRIPT_DIR%requirements.txt" (
    pip install -r "%SCRIPT_DIR%requirements.txt" -q
    echo [OK] Python dependencies installed
) else (
    echo [WARNING] requirements.txt not found, skipping Python dependencies
)
echo.

:: ── NODE DEPENDENCIES ────────────────────────────────────────────────────────
echo [SETUP] Installing Node.js dependencies...
if exist "%SCRIPT_DIR%devtrails-web-frontend\package.json" (
    cd "%SCRIPT_DIR%devtrails-web-frontend"
    call npm install --silent
    echo [OK] Node modules installed
    cd "%SCRIPT_DIR%"
) else (
    echo [WARNING] package.json not found in frontend directory
)
echo.

:: ── MAVEN BUILD ──────────────────────────────────────────────────────────────
echo [BUILD] Building Spring Boot backend...
if exist "%SCRIPT_DIR%backend\backend\pom.xml" (
    cd "%SCRIPT_DIR%backend\backend"
    call mvn clean package -DskipTests -q
    if %errorlevel% neq 0 (
        echo [ERROR] Maven build failed. Check output above.
        pause & exit /b 1
    )
    echo [OK] Spring Boot build successful
    cd "%SCRIPT_DIR%"
) else (
    echo [WARNING] pom.xml not found in backend directory
)
echo.

:: ── START ALL SERVERS IN SEPARATE WINDOWS ────────────────────────────────────
echo [START] Launching all 5 servers...
echo.

:: 1. Spring Boot Backend
echo [1/5] Starting Spring Boot Backend (port 8080)...
if exist "%SCRIPT_DIR%backend\backend\target\backend-0.0.1-SNAPSHOT.jar" (
    start "GigShield - Spring Boot [8080]" cmd /k "cd /d %SCRIPT_DIR%backend\backend && java -jar target\backend-0.0.1-SNAPSHOT.jar --spring.jpa.hibernate.ddl-auto=update > %LOG_DIR%\springboot.log 2>&1"
    timeout /t 3 /nobreak >nul
) else (
    echo [WARNING] Backend JAR not found, trying to run with Maven...
    start "GigShield - Spring Boot [8080]" cmd /k "cd /d %SCRIPT_DIR%backend\backend && mvnw spring-boot:run > %LOG_DIR%\springboot.log 2>&1"
    timeout /t 3 /nobreak >nul
)

:: 2. Mock Platform API
echo [2/5] Starting Mock Platform API (port 5002)...
if exist "%SCRIPT_DIR%Trigger_System\mock_platform_api.py" (
    start "GigShield - Mock Platform API [5002]" cmd /k "cd /d %SCRIPT_DIR%Trigger_System && call %VENV_DIR%\Scripts\activate.bat && python mock_platform_api.py"
    timeout /t 2 /nobreak >nul
) else (
    echo [WARNING] mock_platform_api.py not found
)

:: 3. ML Payout Calculator
echo [3/5] Starting ML Payout Calculator (port 5003)...
if exist "%SCRIPT_DIR%Premium_Calculation\app.py" (
    start "GigShield - ML Payout API [5003]" cmd /k "cd /d %SCRIPT_DIR%Premium_Calculation && call %VENV_DIR%\Scripts\activate.bat && python app.py"
    timeout /t 2 /nobreak >nul
) else (
    echo [WARNING] ML Payout Calculator app.py not found
)

:: 4. Trigger Engine
echo [4/5] Starting Trigger Engine (port 5001)...
if exist "%SCRIPT_DIR%Trigger_System\app.py" (
    start "GigShield - Trigger Engine [5001]" cmd /k "cd /d %SCRIPT_DIR%Trigger_System && call %VENV_DIR%\Scripts\activate.bat && python app.py"
    timeout /t 2 /nobreak >nul
) else (
    echo [WARNING] Trigger Engine app.py not found
)

:: 5. React Frontend
echo [5/5] Starting React Frontend (port 5173)...
if exist "%SCRIPT_DIR%devtrails-web-frontend\package.json" (
    start "GigShield - React Frontend [5173]" cmd /k "cd /d %SCRIPT_DIR%devtrails-web-frontend && npm run dev"
    timeout /t 3 /nobreak >nul
) else (
    echo [WARNING] React Frontend package.json not found
)

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
echo   Close all terminal windows to stop servers.
echo  ============================================================
echo.

:: Open browser after a delay
echo [INFO] Opening browser in 10 seconds...
timeout /t 10 /nobreak >nul
start http://localhost:5173

pause
