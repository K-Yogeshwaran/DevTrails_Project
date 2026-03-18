# GigShield 🛡️
### AI-Powered Parametric Insurance for India's Gig Economy

> **Guidewire DEVTrails 2026 — University Hackathon**
> Sri Eshwar College of Engineering

![Python](https://img.shields.io/badge/Python-3776AB?style=flat&logo=python&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-000000?style=flat&logo=flask&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![React Native](https://img.shields.io/badge/React_Native-20232A?style=flat&logo=react&logoColor=61DAFB)
![XGBoost](https://img.shields.io/badge/XGBoost-FF6600?style=flat&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=flat&logo=postgresql&logoColor=white)

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Our Solution](#2-our-solution)
3. [System Architecture](#3-system-architecture)
4. [Parametric Trigger Engine](#4-parametric-trigger-engine)
5. [AI / ML Features](#5-ai--ml-features)
6. [Trigger → Payout Flow](#6-trigger--payout-flow)
7. [Complete Tech Stack](#7-complete-tech-stack)
8. [Build Status](#8-build-status)
9. [How to Run](#9-how-to-run)
10. [Repository Structure](#10-repository-structure)
11. [Team](#11-team)

---

## 1. Problem Statement

India has over **15 million** platform-based delivery workers employed by Zomato, Swiggy, Zepto, Blinkit, Amazon, and Flipkart. These workers are the backbone of India's digital economy — yet they have **zero financial protection** against income loss.

External disruptions such as extreme weather, hazardous air quality, city-wide curfews, or platform outages can reduce their working hours and cause them to lose **20–30% of their monthly earnings**. The cruel part — none of these disruptions are the worker's fault.

### Why Existing Solutions Fail

- Traditional insurance requires workers to file claims manually, submit proof, and wait days or weeks for approval
- Health and vehicle insurance exists but **income protection does not**
- No insurance product is priced on a **weekly basis** matching how gig workers actually earn
- Workers have no visibility into when a disruption qualifies for compensation

---

## 2. Our Solution

GigShield is an **AI-enabled parametric insurance platform** that automatically protects gig workers' income when external disruptions occur.

The key word is **parametric** — instead of the worker filing a claim, the system detects the disruption itself and triggers the payout automatically. The worker does nothing. Money just arrives.

### The Three Personas We Cover

| Persona | Platforms | Sensitivity | Why |
|---|---|---|---|
| Food Delivery | Zomato · Swiggy | 1.2× | Income stops instantly when it rains |
| Grocery / Q-Commerce | Zepto · Blinkit | 1.0× | Baseline income loss rate |
| E-Commerce | Amazon · Flipkart | 0.85× | Slight buffer — indoor pickups possible |

### Traditional vs GigShield

| Traditional Insurance | GigShield Parametric |
|---|---|
| Worker files a claim | No action needed from worker |
| Submit documents and proof | Trigger detected automatically |
| Wait days or weeks | Payout in under 5 minutes |
| Claim may be rejected | Objective threshold — no disputes |
| Complex pricing model | Simple weekly pricing |

---

## 3. System Architecture

![System Architecture](docs/architecture.svg)

GigShield is built across 4 layers, each with a clearly defined responsibility.

### Layer 1 — Parametric Trigger Engine
**Tech:** Python 3 · Flask 3.0 · APScheduler · Flask-SocketIO · Port 5001

The core innovation. A centralized server polls multiple external APIs every 5 minutes and monitors disruption conditions for all active workers simultaneously. When a threshold is crossed, it fires a trigger event automatically for every affected worker in that zone.

### Layer 2 — AI / ML Core
**Tech:** Python · XGBoost · scikit-learn · joblib · pandas · Port 5003

Three ML modules handle intelligent pricing, fraud detection, and predictive analytics — dynamic weekly premium calculation, Isolation Forest fraud detection, and Random Forest disruption forecasting.

### Layer 3 — Backend Services
**Tech:** Java 17 · Spring Boot 3 · Spring Data JPA · Spring Security · PostgreSQL · Port 8080

| Service | Responsibility |
|---|---|
| Worker Service | Onboarding, profile, zone registration, policy assignment |
| Policy Service | Weekly policy creation, tier management, coverage tracking |
| Claims Service | Picks up pending_payout events, validates policy, calculates payout |
| Payout Service | Dispatches payment via Razorpay sandbox / UPI simulator |

### Layer 4 — Frontend
**Tech:** React Native (worker app) · React (admin dashboard) · Socket.IO · Chart.js · Leaflet.js

- **React Native mobile app** — worker onboarding, policy status, payout history, real-time push notifications
- **React web dashboard** — insurer/admin view showing loss ratios, live zone map, fraud queue, predictive analytics

---

## 4. Parametric Trigger Engine

Instead of relying on individual worker phones to detect disruptions, GigShield uses a single **centralized server** that monitors conditions for all active workers simultaneously.

### The 5 Disruption Triggers

| Trigger | Threshold | Source API | Income Impact |
|---|---|---|---|
| Heavy Rainfall | > 30mm in 3hr | Open-Meteo (free, no key) | Bikes stop, roads flood |
| Hazardous AQI | AQI > 200 | CPCB via data.gov.in | Outdoor work unsafe |
| Extreme Heat | > 42°C | Open-Meteo temperature | NDMA heat advisory |
| Platform Downtime | > 45 min down | Mock Platform Status API | No orders possible |
| Curfew / Section 144 | Any active order | Govt alert feed (mock) | Cannot access routes |

### Why Centralized (Not Phone-Based)

- Worker **cannot manipulate** the trigger — they never touch it
- One server handles **all workers in all zones** simultaneously
- Consistent, verifiable data from **independent third-party APIs**
- **Dedup lock** — same trigger cannot fire twice within 1 hour for the same zone
- **Zone-based, not GPS** — workers check into a zone, protecting their privacy

### REST API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Health check — verify engine is running |
| GET | `/api/zones` | List all configured zones with lat/lon |
| GET | `/api/workers/active` | All active workers grouped by zone |
| POST | `/api/workers/register` | Worker goes online — starts coverage |
| POST | `/api/workers/deregister` | Worker goes offline — stops coverage |
| GET | `/api/triggers` | Last 100 trigger events (filterable) |
| POST | `/api/triggers/manual` | Force a trigger for demo / testing |

### Trigger Engine File Structure

```
Trigger_System/
├── app.py                ← Main server + scheduler + WebSocket + REST
├── triggers.py           ← All 5 disruption trigger checks
├── config.py             ← Thresholds, zones, API keys
├── mock_platform_api.py  ← Simulated platform status API (port 5002)
└── requirements.txt
```

---

## 5. AI / ML Features

### 5.1 Premium Calculator — XGBoost Model

![Data Flow](docs/dataflow.svg)

#### The Income-at-Risk Model

Instead of a flat price for everyone, GigShield uses an **Income-at-Risk model** — the premium is based on what the worker actually stands to lose.

| Factor | Example | Impact on Premium |
|---|---|---|
| Hourly Rate | ₹900/day ÷ 8hrs = ₹112/hr | Higher earner → higher premium |
| Zone Disruption Frequency | Velachery: 6.5 events/month | Risky zone → higher premium |
| Persona Sensitivity | Food: 1.2× · Grocery: 1.0× | Food delivery pays more |
| Season Risk | Monsoon: 1.6× · Summer: 0.8× | Monsoon season costs more |
| Experience Discount | 48 months → 15% discount | Experienced workers pay less |
| Shift Risk | Night: 1.25× · Morning: 0.90× | Night shift pays more |

#### Premium Formula

```
Weekly Premium = MAX(
    Expected_Weekly_Loss × 0.65,    ← actuarial (65% loss ratio)
    Weekly_Earnings × 0.018          ← affordability cap (1.8%)
)

Expected_Weekly_Loss = Hourly_Rate × Avg_Disrupted_Hours/Week
                       × Persona_Sensitivity × Shift_Risk

Example — Ravi Kumar (food, Velachery, ₹900/day, monsoon):
  Hourly rate       = ₹900 ÷ 8 = ₹112/hr
  Disruptions/week  = (6.5 × 1.6) ÷ 4.33 = 2.4/week
  Expected loss     = ₹112 × 4hrs × 2.4 × 1.2 = ₹1,290/week
  Actuarial premium = ₹1,290 × 0.65 = ₹839  (too high)
  Affordability cap = ₹5,400 × 0.018 = ₹97   ← this wins
  Final premium     ≈ ₹89/week (Standard tier)
```

#### Coverage Tiers

| Tier | Weekly Premium | Weekly Coverage Cap | Best For |
|---|---|---|---|
| Basic | ₹49 / week | ₹2,000 | Part-time workers (≤ ₹500/day) |
| Standard ⭐ | ₹89 / week | ₹4,500 | Full-time workers (₹700–1,100/day) |
| Premium | ₹149 / week | ₹8,000 | High earners (≥ ₹1,200/day) |

#### Model Details

- **Training data** — 5,000 synthetic worker records (Monte Carlo simulation)
- **Algorithm** — XGBoost Gradient Boosting Regressor (200 trees, max depth 6)
- **Accuracy** — MAE < ₹10 · R² score > 0.96
- **Top feature** — `daily_earnings` (0.48 importance) → model learned real-world logic
- **API** — POST `/api/premium/calculate` on port 5003

#### ML File Structure

```
ML_Premium/
├── config.py           ← All constants — zones, tiers, sensitivity values
├── data_generator.py   ← Generates 5,000 synthetic worker training records
├── ml_engine.py        ← Train, save, load, predict — all XGBoost logic
└── app.py              ← Flask REST API (port 5003) — no ML logic inside
```

### 5.2 Fraud Detection — Isolation Forest (Phase 2)

Three layers of fraud prevention:

**Layer 1 — Trigger-level (built)**
Disruption data comes from independent third-party APIs. The worker has no way to influence the trigger. A fake claim is structurally impossible because the worker never initiates it.

**Layer 2 — Claim-level rules (Phase 2)**
- Duplicate check: same worker cannot claim same trigger type twice in one day
- Zone validation: worker's registered zone must match the disrupted zone
- Velocity check: more than 3 claims in 7 days gets flagged for manual review

**Layer 3 — ML anomaly detection (Phase 3)**
Isolation Forest model trained on claim patterns flags statistical outliers — workers whose claim frequency or amount is significantly above their cohort average.

### 5.3 Predictive Disruption Forecasting (Phase 3)

Random Forest classifier trained on historical trigger data predicts next week's disruption probability per zone. Used by the admin dashboard to pre-warn insurers of high-claim weeks (e.g. monsoon season approaching Velachery).

---

## 6. Trigger → Payout Flow

Zero-touch parametric claim processing — from disruption detection to worker's UPI in under 5 minutes.

### Step-by-step walkthrough (Ravi Kumar, Velachery, flood event)

| Step | Action | Detail |
|---|---|---|
| 1 | Disruption detected | Rainfall reaches 42mm in 3hr — exceeds 30mm threshold |
| 2 | Policy check | Server confirms Ravi has active Standard tier policy this week |
| 3 | Fraud check | No duplicate claim today, zone matches, no anomaly detected |
| 4 | Payout calculated | ₹112/hr × 4 disrupted hrs × 1.2 persona = **₹538** |
| 5 | Coverage cap check | ₹538 < ₹4,500 weekly cap — full amount approved |
| 6 | Payout dispatched | ₹538 sent to Ravi's UPI via Razorpay sandbox |
| 7 | Claim saved | status = paid · admin dashboard updated · loss ratio recalculated |

### Payout Formula

```
Payout = (Daily_Earnings ÷ Active_Hours) × Disrupted_Hours × Persona_Factor

Ravi Kumar:
  Daily earnings  = ₹900     Active hours    = 8
  Disrupted hours = 4        Persona factor  = 1.2 (food)
  Payout          = (₹900 ÷ 8) × 4 × 1.2  = ₹538

Final payout = MIN(calculated_payout, remaining_weekly_cap)
```

---

## 7. Complete Tech Stack

![Tech Stack](docs/techstack.svg)

| Layer | Technology | Purpose | Port |
|---|---|---|---|
| Trigger Engine | Python 3 · Flask 3.0 | REST API server | 5001 |
| Trigger Engine | APScheduler | Background polling every 5 min | — |
| Trigger Engine | Flask-SocketIO | WebSocket real-time push to app | — |
| Trigger Engine | Requests | External API calls | — |
| Mock Platform API | Python · Flask | Simulated platform status | 5002 |
| ML — Premium | XGBoost | Gradient boosting regressor | — |
| ML — Premium | scikit-learn | LabelEncoder · train-test split | — |
| ML — Premium | pandas · numpy · joblib | Data processing · model persistence | — |
| ML API | Flask · flask-cors | Premium calculator REST API | 5003 |
| Backend | Java 17 · Spring Boot 3 | Core business logic microservices | 8080 |
| Backend | Spring Data JPA · PostgreSQL | Database ORM and persistence | — |
| Backend | Spring Security · JWT | Authentication and authorization | — |
| Backend | Razorpay SDK | Payment sandbox · UPI simulation | — |
| Mobile App | React Native 0.74 | Worker-facing Android + iOS app | — |
| Mobile App | Socket.IO Client | Real-time trigger notifications | — |
| Admin Dashboard | React 18 · Vite | Insurer/admin web dashboard | 3000 |
| Admin Dashboard | Chart.js · Leaflet.js | Loss ratio charts · live zone map | — |
| External API | Open-Meteo | Rainfall + temperature (free, no key) | — |
| External API | data.gov.in CPCB | Real-time AQI data (free key) | — |

---

## 8. Build Status

### Phase 1 — Ideation & Foundation (March 4–20) ✅

| Component | Status |
|---|---|
| Centralized parametric trigger engine | ✅ Done |
| 5 disruption triggers (rain, AQI, heat, platform, curfew) | ✅ Done |
| Zone-based worker registry | ✅ Done |
| Dedup lock — no double payouts | ✅ Done |
| REST API endpoints (7 routes) | ✅ Done |
| WebSocket real-time push | ✅ Done |
| Mock Platform Status API (port 5002) | ✅ Done |
| Manual trigger endpoint for demo | ✅ Done |
| XGBoost Premium Calculator (4 files) | ✅ Done |
| Synthetic training data generator (5000 records) | ✅ Done |
| System architecture diagrams (3 SVG files) | ✅ Done |

### Phase 2 — Automation & Protection (March 21 – April 4) ⏳

| Component | Status |
|---|---|
| Worker registration + policy creation (Spring Boot) | ⏳ Next |
| Claims service — auto-process pending_payout events | ⏳ Next |
| Payout dispatch via Razorpay sandbox | ⏳ Next |
| Fraud detection — Isolation Forest ML model | ⏳ Next |
| React Native worker mobile app | ⏳ Next |

### Phase 3 — Scale & Optimise (April 5–17) 🔜

| Component | Status |
|---|---|
| React admin dashboard | 🔜 Upcoming |
| Predictive disruption forecaster | 🔜 Upcoming |
| Advanced GPS spoof fraud detection | 🔜 Upcoming |
| Full end-to-end demo video | 🔜 Upcoming |

---

## 9. How to Run

### Prerequisites
- Python 3.11+ with pip
- Java 17+ with Maven
- Node.js 18+
- PostgreSQL 15

### Step 1 — Trigger Engine (Port 5001)

```bash
cd Trigger_System
python -m venv venv
venv\Scripts\activate          # Windows
source venv/bin/activate       # Mac/Linux
pip install -r requirements.txt
python app.py
```

Expected output:
```
[INFO] Starting GigShield Parametric Trigger Engine...
[INFO] Seeded 5 demo workers across zones
[INFO] Scheduler started: polling every 300s
[INFO] Server starting on http://localhost:5001
```

### Step 2 — Mock Platform API (Port 5002)

Open a second terminal:
```bash
cd Trigger_System
python mock_platform_api.py
```

Simulate a platform going down:
```bash
curl -X POST http://localhost:5002/api/status/zomato/down \
  -H "Content-Type: application/json" \
  -d "{\"reason\": \"Server overload\", \"affected_cities\": [\"Chennai\"]}"
```

### Step 3 — Premium Calculator ML API (Port 5003)

Open a third terminal:
```bash
cd ML_Premium
pip install xgboost scikit-learn pandas numpy joblib flask flask-cors
python app.py
```

The model trains automatically on first run (takes ~30 seconds).

### Testing All Endpoints

```bash
# Health checks
curl http://localhost:5001/api/health
curl http://localhost:5003/api/premium/health

# Active workers
curl http://localhost:5001/api/workers/active

# Recent triggers
curl http://localhost:5001/api/triggers

# Calculate premium for a worker
curl -X POST http://localhost:5003/api/premium/calculate \
  -H "Content-Type: application/json" \
  -d "{\"worker_id\":\"W001\",\"zone_id\":\"zone_chennai_velachery\",\"persona\":\"food\",\"daily_earnings\":900,\"active_hours\":8,\"shift\":\"afternoon\",\"season\":\"monsoon\",\"days_per_week\":6,\"experience_months\":12}"

# Fire a manual trigger (demo)
curl -X POST http://localhost:5001/api/triggers/manual \
  -H "Content-Type: application/json" \
  -d "{\"zone_id\": \"zone_chennai_velachery\", \"trigger_type\": \"rainfall\", \"value\": 45}"
```

---

## 10. Repository Structure

```
GigShield/
├── Trigger_System/           ← Parametric trigger engine (Python/Flask)
│   ├── app.py                ← Main server + scheduler + WebSocket + REST
│   ├── triggers.py           ← All 5 disruption trigger checks
│   ├── config.py             ← Thresholds, zones, API keys
│   ├── mock_platform_api.py  ← Simulated platform status API
│   └── requirements.txt
│
├── ML_Premium/               ← XGBoost premium calculator (Python)
│   ├── config.py             ← Constants, zone frequencies, tier definitions
│   ├── data_generator.py     ← Synthetic training data (5000 records)
│   ├── ml_engine.py          ← Train, save, load, predict
│   ├── app.py                ← Flask REST API (port 5003)
│   └── requirements_ml.txt
│
├── docs/                     ← Architecture diagrams (embed in GitHub README)
│   ├── architecture.svg      ← Full system architecture
│   ├── dataflow.svg          ← Trigger to payout pipeline
│   └── techstack.svg         ← Complete tech stack
│
├── Backend/                  ← Spring Boot microservices (Phase 2)
├── MobileApp/                ← React Native worker app (Phase 2)
├── AdminDashboard/           ← React web dashboard (Phase 3)
└── README.md
```

---

## 11. Team

| | |
|---|---|
| **College** | Sri Eshwar College of Engineering |
| **Hackathon** | Guidewire DEVTrails 2026 — University Hackathon |
| **Problem Statement** | AI-Powered Insurance for India's Gig Economy |
| **Phase 1 Deadline** | March 20, 2026 |
| **Phase 2 Deadline** | April 4, 2026 |
| **Phase 3 Deadline** | April 17, 2026 |

---

*GigShield — Protecting India's Gig Workers, One Week at a Time.*
