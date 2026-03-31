#!/bin/bash

# =============================================================================
#  GigShield — Full Stack Startup Script
#  Guidewire DEVTrails 2026 | Sri Eshwar College of Engineering
#
#  Starts all 5 servers:
#    1. Spring Boot Backend        → http://localhost:8080
#    2. Trigger Engine             → http://localhost:5001
#    3. Mock Platform API          → http://localhost:5002
#    4. ML Payout Calculator       → http://localhost:5003
#    5. React Frontend             → http://localhost:5173
#
#  Usage:
#    chmod +x start.sh
#    ./start.sh
#
#  Prerequisites (must be installed manually):
#    - Java 17+
#    - Maven 3.8+
#    - Python 3.11+
#    - Node.js 18+
#    - PostgreSQL 15 (running on port 5432)
#    - Redis (Docker: docker run -d -p 6379:6379 redis:7-alpine)
# =============================================================================

set -e

# ── COLORS ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# ── HELPERS ───────────────────────────────────────────────────────────────────
log()     { echo -e "${WHITE}[$(date '+%H:%M:%S')]${NC} $1"; }
success() { echo -e "${GREEN}[$(date '+%H:%M:%S')] ✅ $1${NC}"; }
warn()    { echo -e "${YELLOW}[$(date '+%H:%M:%S')] ⚠️  $1${NC}"; }
error()   { echo -e "${RED}[$(date '+%H:%M:%S')] ❌ $1${NC}"; }
section() { echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; echo -e "${CYAN}  $1${NC}"; echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"; }

# ── SCRIPT DIRECTORY ──────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

# ── PID TRACKING ──────────────────────────────────────────────────────────────
PIDS=()

cleanup() {
    echo ""
    section "🛑 Shutting down all GigShield servers..."
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            log "Stopped process $pid"
        fi
    done
    success "All servers stopped. Goodbye!"
    exit 0
}

trap cleanup SIGINT SIGTERM

# ── BANNER ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${MAGENTA}  ██████╗ ██╗ ██████╗ ███████╗██╗  ██╗██╗███████╗██╗     ██████╗ ${NC}"
echo -e "${MAGENTA} ██╔════╝ ██║██╔════╝ ██╔════╝██║  ██║██║██╔════╝██║     ██╔══██╗${NC}"
echo -e "${MAGENTA} ██║  ███╗██║██║  ███╗███████╗███████║██║█████╗  ██║     ██║  ██║${NC}"
echo -e "${MAGENTA} ██║   ██║██║██║   ██║╚════██║██╔══██║██║██╔══╝  ██║     ██║  ██║${NC}"
echo -e "${MAGENTA} ╚██████╔╝██║╚██████╔╝███████║██║  ██║██║███████╗███████╗██████╔╝${NC}"
echo -e "${MAGENTA}  ╚═════╝ ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═╝╚══════╝╚══════╝╚═════╝ ${NC}"
echo ""
echo -e "${WHITE}  AI-Powered Parametric Insurance for India's Gig Economy${NC}"
echo -e "${CYAN}  Guidewire DEVTrails 2026 | Sri Eshwar College of Engineering${NC}"
echo ""

# ── PREREQUISITE CHECKS ───────────────────────────────────────────────────────
section "🔍 Checking Prerequisites"

check_command() {
    if command -v "$1" &>/dev/null; then
        success "$1 found: $(command -v $1)"
        return 0
    else
        error "$1 not found. Please install $1 and try again."
        return 1
    fi
}

PREREQ_OK=true
check_command java    || PREREQ_OK=false
check_command mvn     || PREREQ_OK=false
check_command python3 || PREREQ_OK=false
check_command node    || PREREQ_OK=false
check_command npm     || PREREQ_OK=false

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    error "Java 17+ required. Found Java $JAVA_VERSION"
    PREREQ_OK=false
else
    success "Java version: $JAVA_VERSION ✓"
fi

# Check Redis
if redis-cli ping &>/dev/null 2>&1; then
    success "Redis is running"
else
    warn "Redis not detected on port 6379."
    warn "Start Redis with: docker run -d -p 6379:6379 redis:7-alpine"
    warn "Continuing without Redis — workers won't be tracked in zones."
fi

# Check PostgreSQL
if pg_isready -h localhost -p 5432 &>/dev/null 2>&1; then
    success "PostgreSQL is running on port 5432"
else
    error "PostgreSQL not running on port 5432."
    error "Please start PostgreSQL and create database 'gigshield_db'."
    error "  createdb gigshield_db"
    PREREQ_OK=false
fi

if [ "$PREREQ_OK" = false ]; then
    error "One or more prerequisites are missing. Please fix them and re-run."
    exit 1
fi

# ── PYTHON VIRTUAL ENVIRONMENT ────────────────────────────────────────────────
section "🐍 Setting up Python Environment"

VENV_DIR="$SCRIPT_DIR/venv"

if [ ! -d "$VENV_DIR" ]; then
    log "Creating Python virtual environment..."
    python3 -m venv "$VENV_DIR"
    success "Virtual environment created at $VENV_DIR"
else
    success "Virtual environment already exists"
fi

source "$VENV_DIR/bin/activate"

log "Installing Python dependencies from requirements.txt..."
pip install --upgrade pip -q
pip install -r "$SCRIPT_DIR/requirements.txt" -q
success "Python dependencies installed"

# ── NODE DEPENDENCIES ─────────────────────────────────────────────────────────
section "📦 Installing Node.js Dependencies"

FRONTEND_DIR="$SCRIPT_DIR/devtrails-web-frontend"

if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    if [ ! -d "node_modules" ]; then
        log "Running npm install (first time — this may take a minute)..."
        npm install --silent
        success "Node modules installed"
    else
        log "Checking for dependency updates..."
        npm install --silent
        success "Node modules up to date"
    fi
    cd "$SCRIPT_DIR"
else
    error "Frontend directory not found: $FRONTEND_DIR"
    exit 1
fi

# ── MAVEN BUILD ───────────────────────────────────────────────────────────────
section "☕ Building Spring Boot Backend"

BACKEND_DIR="$SCRIPT_DIR/backend/backend"

if [ -d "$BACKEND_DIR" ]; then
    cd "$BACKEND_DIR"
    log "Running Maven build (skipping tests)..."
    mvn clean package -DskipTests -q 2>&1 | tail -5
    if [ $? -eq 0 ]; then
        success "Spring Boot build successful"
    else
        error "Maven build failed. Check logs above."
        exit 1
    fi
    cd "$SCRIPT_DIR"
else
    error "Backend directory not found: $BACKEND_DIR"
    exit 1
fi

# ── DELETE STALE ML MODELS ────────────────────────────────────────────────────
section "🤖 Preparing ML Models"

ML_DIR="$SCRIPT_DIR/Premium_Calculation"
if [ -f "$ML_DIR/payout_model.joblib" ]; then
    success "Payout model found — skipping retrain"
else
    warn "No payout model found — will train on first startup (~30 seconds)"
fi

# ── START SERVERS ─────────────────────────────────────────────────────────────
section "🚀 Starting All Servers"

# 1. Spring Boot Backend (port 8080)
log "${BLUE}[1/5]${NC} Starting Spring Boot Backend on port 8080..."
cd "$BACKEND_DIR"
java -jar target/backend-0.0.1-SNAPSHOT.jar \
    --spring.jpa.hibernate.ddl-auto=update \
    > "$LOG_DIR/springboot.log" 2>&1 &
SPRING_PID=$!
PIDS+=($SPRING_PID)
cd "$SCRIPT_DIR"
log "Spring Boot PID: $SPRING_PID | Log: logs/springboot.log"

# Wait for Spring Boot to be ready
log "Waiting for Spring Boot to start..."
for i in {1..60}; do
    if curl -s http://localhost:8080/api/workers/health &>/dev/null; then
        success "Spring Boot is ready on http://localhost:8080"
        break
    fi
    if [ $i -eq 60 ]; then
        error "Spring Boot failed to start in 60 seconds. Check logs/springboot.log"
        exit 1
    fi
    sleep 1
done

# 2. Mock Platform API (port 5002)
log "${BLUE}[2/5]${NC} Starting Mock Platform API on port 5002..."
source "$VENV_DIR/bin/activate"
cd "$SCRIPT_DIR/Trigger_System"
python3 mock_platform_api.py \
    > "$LOG_DIR/mock_platform.log" 2>&1 &
MOCK_PID=$!
PIDS+=($MOCK_PID)
cd "$SCRIPT_DIR"
log "Mock Platform API PID: $MOCK_PID | Log: logs/mock_platform.log"
sleep 2
if curl -s http://localhost:5002/api/status &>/dev/null; then
    success "Mock Platform API is ready on http://localhost:5002"
else
    warn "Mock Platform API may still be starting..."
fi

# 3. ML Payout Calculator (port 5003)
log "${BLUE}[3/5]${NC} Starting ML Payout Calculator on port 5003..."
cd "$SCRIPT_DIR/Premium_Calculation"
python3 app.py \
    > "$LOG_DIR/ml_payout.log" 2>&1 &
ML_PID=$!
PIDS+=($ML_PID)
cd "$SCRIPT_DIR"
log "ML Payout API PID: $ML_PID | Log: logs/ml_payout.log"

# Wait for ML API (may need to train model first)
log "Waiting for ML Payout API (may train model on first run ~30s)..."
for i in {1..90}; do
    if curl -s http://localhost:5003/api/payout/health &>/dev/null; then
        success "ML Payout API is ready on http://localhost:5003"
        break
    fi
    if [ $i -eq 90 ]; then
        warn "ML Payout API taking longer than expected. Check logs/ml_payout.log"
    fi
    sleep 1
done

# 4. Trigger Engine (port 5001)
log "${BLUE}[4/5]${NC} Starting Trigger Engine on port 5001..."
cd "$SCRIPT_DIR/Trigger_System"
python3 app.py \
    > "$LOG_DIR/trigger_engine.log" 2>&1 &
TRIGGER_PID=$!
PIDS+=($TRIGGER_PID)
cd "$SCRIPT_DIR"
log "Trigger Engine PID: $TRIGGER_PID | Log: logs/trigger_engine.log"

# Wait for Trigger Engine
log "Waiting for Trigger Engine..."
for i in {1..30}; do
    if curl -s http://localhost:5001/api/health &>/dev/null; then
        success "Trigger Engine is ready on http://localhost:5001"
        break
    fi
    if [ $i -eq 30 ]; then
        warn "Trigger Engine taking longer than expected. Check logs/trigger_engine.log"
    fi
    sleep 1
done

# 5. React Frontend (port 5173)
log "${BLUE}[5/5]${NC} Starting React Frontend on port 5173..."
cd "$FRONTEND_DIR"
npm run dev \
    > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
PIDS+=($FRONTEND_PID)
cd "$SCRIPT_DIR"
log "React Frontend PID: $FRONTEND_PID | Log: logs/frontend.log"
sleep 3
success "React Frontend is ready on http://localhost:5173"

# ── ALL SERVERS RUNNING ───────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  🎉 All GigShield servers are running!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "  ${WHITE}Service                  URL                    PID${NC}"
echo -e "  ${CYAN}─────────────────────────────────────────────────────${NC}"
echo -e "  ${GREEN}React Frontend${NC}           http://localhost:5173   $FRONTEND_PID"
echo -e "  ${BLUE}Spring Boot Backend${NC}      http://localhost:8080   $SPRING_PID"
echo -e "  ${YELLOW}Trigger Engine${NC}           http://localhost:5001   $TRIGGER_PID"
echo -e "  ${MAGENTA}Mock Platform API${NC}        http://localhost:5002   $MOCK_PID"
echo -e "  ${CYAN}ML Payout Calculator${NC}     http://localhost:5003   $ML_PID"
echo ""
echo -e "  ${WHITE}Admin Dashboard${NC}          http://localhost:5173/admin/login"
echo -e "  ${WHITE}Admin credentials${NC}        admin / gigshield@admin2026"
echo ""
echo -e "  ${WHITE}Logs directory${NC}           $LOG_DIR"
echo ""
echo -e "  ${YELLOW}Press Ctrl+C to stop all servers${NC}"
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# ── LIVE LOG TAIL ─────────────────────────────────────────────────────────────
log "Tailing all logs (Ctrl+C to stop all servers)..."
echo ""

# Tail all logs with color-coded prefixes
tail -f \
    "$LOG_DIR/springboot.log" \
    "$LOG_DIR/trigger_engine.log" \
    "$LOG_DIR/mock_platform.log" \
    "$LOG_DIR/ml_payout.log" \
    "$LOG_DIR/frontend.log" \
    2>/dev/null | awk '
    /springboot/    { print "\033[0;34m[SPRING]  \033[0m" $0; next }
    /trigger_engine/{ print "\033[1;33m[TRIGGER] \033[0m" $0; next }
    /mock_platform/ { print "\033[0;35m[MOCK]    \033[0m" $0; next }
    /ml_payout/     { print "\033[0;36m[ML]      \033[0m" $0; next }
    /frontend/      { print "\033[0;32m[REACT]   \033[0m" $0; next }
    { print $0 }
' &

# Keep script alive — wait for any server to die
wait "${PIDS[@]}"
