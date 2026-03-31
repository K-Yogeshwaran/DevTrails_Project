# GigShield — Quick Start Guide

## Prerequisites

Install these before running the startup script:

| Tool | Version | Download |
|---|---|---|
| Java | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| Python | 3.11+ | https://python.org |
| Node.js | 18+ | https://nodejs.org |
| PostgreSQL | 15 | https://postgresql.org |
| Docker | Any | https://docker.com (for Redis) |

---

## One-Time Database Setup

```sql
-- Run in psql or pgAdmin
CREATE DATABASE gigshield_db;
```

Then run the migration script:
```bash
psql -U postgres -d gigshield_db -f backend/migration.sql
```

---

## Start Redis (Docker)

```bash
docker run -d --name redis-gigshield -p 6379:6379 redis:7-alpine
```

---

## Run Everything

### On Mac / Linux:
```bash
chmod +x start.sh
./start.sh
```

### On Windows:
```
Double-click start.bat
OR
start.bat
```

---

## What starts

| Service | URL | Description |
|---|---|---|
| React Frontend | http://localhost:5173 | Worker app + Admin dashboard |
| Spring Boot | http://localhost:8080 | Main backend API |
| Trigger Engine | http://localhost:5001 | Disruption detection + WebSocket |
| Mock Platform API | http://localhost:5002 | Simulates Swiggy/Zomato/Zepto |
| ML Payout API | http://localhost:5003 | XGBoost payout calculator |

---

## Admin Dashboard

URL: http://localhost:5173/admin/login

| Username | Password |
|---|---|
| admin | gigshield@admin2026 |
| devtrails | devtrails@2026 |
| yogesh | yogesh@admin |
| sriram | sriram@admin |

---

## Logs

All server logs are saved to the `logs/` directory:

```
logs/
├── springboot.log
├── trigger_engine.log
├── mock_platform.log
├── ml_payout.log
└── frontend.log
```

---

## Troubleshooting

**Spring Boot fails to start**
- Check PostgreSQL is running: `pg_isready -h localhost -p 5432`
- Check `logs/springboot.log` for errors
- Verify `backend/backend/src/main/resources/application.properties` has correct DB credentials

**ML API takes long to start**
- First run trains the XGBoost model (~30 seconds) — this is normal
- Check `logs/ml_payout.log`

**Trigger Engine shows "Redis unavailable"**
- Start Redis: `docker run -d -p 6379:6379 redis:7-alpine`
- Workers can still register but zone tracking won't persist

**Port already in use**
```bash
# Find and kill process on a port (Mac/Linux)
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```
