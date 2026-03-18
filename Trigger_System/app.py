import logging
import time
from datetime import datetime, timezone
from flask import Flask, jsonify, request
from flask_socketio import SocketIO, emit
from flask_cors import CORS
from apscheduler.schedulers.background import BackgroundScheduler


from config import ZONES, POLL_INTERVAL_SECONDS, DEDUP_WINDOW_SECONDS
from trigger import run_all_checks


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

app = Flask(__name__)
app.config["SECRET_KEY"] = "gigshield-dev-secret-2026"

CORS(app)


socketio = SocketIO(app, cors_allowed_origins="*", async_mode="threading")

# active_workers: maps zone_id → list of worker dicts
# Example:
#   { "zone_chennai_velachery": [
#       {"worker_id": "W001", "name": "Ravi", "persona": "food"},
#       {"worker_id": "W002", "name": "Priya", "persona": "grocery"},
#   ]}
active_workers = {}

# dedup_store: maps "zone_id:trigger_type" → unix timestamp of last fire
# Example:
#   { "zone_chennai_velachery:rainfall": 1710758400 }
# We use this to prevent the same trigger firing twice
# within DEDUP_WINDOW_SECONDS for the same zone.
dedup_store = {}

# trigger_log: keeps the last 100 trigger events in memory.
# The REST endpoint /api/triggers reads from this.
# Oldest entries are removed when length exceeds 100.
trigger_log = []


# ── DEDUP LOGIC ──────────────────────────────────────────────

def is_duplicate(zone_id, trigger_type):
    key = f"{zone_id}:{trigger_type}"
    now = time.time()

    if key in dedup_store:
        last_fired = dedup_store[key]
        seconds_since = now - last_fired

        if seconds_since < DEDUP_WINDOW_SECONDS:
            log.info(
                f"Dedup: skipping {key} "
                f"(fired {int(seconds_since)}s ago, "
                f"window={DEDUP_WINDOW_SECONDS}s)"
            )
            return True   

    return False  


def record_trigger(zone_id, trigger_type):
    key = f"{zone_id}:{trigger_type}"
    dedup_store[key] = time.time()

def poll_all_zones():
    log.info("── Poll cycle starting ──────────────────────────")

    for zone_id, zone in ZONES.items():
        zone["zone_id"] = zone_id

        log.info(f"Checking zone: {zone['name']} ({zone_id})")

        results = run_all_checks(zone)

        for result in results:
            trigger_type = result["type"]
            if not result["triggered"]:
                continue

            if is_duplicate(zone_id, trigger_type):
                continue
            
            record_trigger(zone_id, trigger_type)

            log.info(
                f"TRIGGER FIRED: {trigger_type} in {zone['name']} "
                f"| value={result['value']} threshold={result['threshold']}"
            )

            affected_workers = active_workers.get(zone_id, [])

            if not affected_workers:
                log.info(
                    f"No active workers in {zone['name']} — "
                    f"trigger logged but no payouts."
                )

            for worker in affected_workers:
                event = {
                    "event_id": f"{zone_id}:{trigger_type}:{int(time.time())}",
                    "trigger_type": trigger_type,
                    "zone_id": zone_id,
                    "zone_name": zone["name"],
                    "worker_id": worker["worker_id"],
                    "worker_name": worker["name"],
                    "persona": worker["persona"],
                    "triggered_at": datetime.now(timezone.utc).isoformat(),
                    "value": result["value"],
                    "threshold": result["threshold"],
                    "unit": result["unit"],
                    "message": result["message"],
                    "status": "pending_payout",  
                }

                trigger_log.append(event)
                if len(trigger_log) > 100:
                    trigger_log.pop(0)  
                socketio.emit("trigger_fired", event)
                log.info(
                    f"  → WebSocket emitted for worker {worker['worker_id']}"
                )

    log.info("── Poll cycle complete ──────────────────────────")

@app.route("/api/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "active_zones": len(ZONES),
        "active_workers": sum(len(w) for w in active_workers.values()),
    })


@app.route("/api/zones", methods=["GET"])
def get_zones():
    return jsonify(ZONES)


@app.route("/api/workers/register", methods=["POST"])
def register_worker():
    data = request.get_json()

    required = ["worker_id", "name", "zone_id", "persona"]
    for field in required:
        if field not in data:
            return jsonify({"error": f"Missing field: {field}"}), 400

    zone_id = data["zone_id"]

    if zone_id not in ZONES:
        return jsonify({"error": f"Unknown zone_id: {zone_id}"}), 400

    if zone_id not in active_workers:
        active_workers[zone_id] = []

    existing_ids = [w["worker_id"] for w in active_workers[zone_id]]
    if data["worker_id"] not in existing_ids:
        active_workers[zone_id].append({
            "worker_id": data["worker_id"],
            "name": data["name"],
            "persona": data["persona"],
            "registered_at": datetime.now(timezone.utc).isoformat(),
        })
        log.info(f"Worker registered: {data['name']} → {zone_id}")

    return jsonify({
        "message": "Worker registered successfully",
        "zone": ZONES[zone_id]["name"],
        "active_in_zone": len(active_workers[zone_id]),
    }), 201


@app.route("/api/workers/deregister", methods=["POST"])
def deregister_worker():
    data = request.get_json()
    worker_id = data.get("worker_id")
    zone_id = data.get("zone_id")

    if zone_id in active_workers:
        active_workers[zone_id] = [
            w for w in active_workers[zone_id]
            if w["worker_id"] != worker_id
        ]

    log.info(f"Worker deregistered: {worker_id} from {zone_id}")
    return jsonify({"message": "Worker deregistered"})


@app.route("/api/workers/active", methods=["GET"])
def get_active_workers():
    return jsonify({
        "zones": {
            zone_id: {
                "zone_name": ZONES[zone_id]["name"],
                "workers": workers,
                "count": len(workers),
            }
            for zone_id, workers in active_workers.items()
        },
        "total_active": sum(len(w) for w in active_workers.values()),
    })


@app.route("/api/triggers", methods=["GET"])
def get_triggers():
    zone_filter = request.args.get("zone_id")
    type_filter = request.args.get("type")

    filtered = trigger_log
    if zone_filter:
        filtered = [t for t in filtered if t["zone_id"] == zone_filter]
    if type_filter:
        filtered = [t for t in filtered if t["trigger_type"] == type_filter]

    # Return newest first — reverse() flips the list order in-place
    return jsonify(list(reversed(filtered)))


@app.route("/api/triggers/manual", methods=["POST"])
def manual_trigger():
    data = request.get_json()
    zone_id = data.get("zone_id")
    trigger_type = data.get("trigger_type", "rainfall")
    value = data.get("value", 45)

    if zone_id not in ZONES:
        return jsonify({"error": "Unknown zone_id"}), 400

    zone = ZONES[zone_id]
    affected = active_workers.get(zone_id, [])
    events_fired = []

    for worker in affected:
        event = {
            "event_id": f"manual:{zone_id}:{trigger_type}:{int(time.time())}",
            "trigger_type": trigger_type,
            "zone_id": zone_id,
            "zone_name": zone["name"],
            "worker_id": worker["worker_id"],
            "worker_name": worker["name"],
            "persona": worker["persona"],
            "triggered_at": datetime.now(timezone.utc).isoformat(),
            "value": value,
            "threshold": "manual override",
            "message": f"Manual trigger: {trigger_type} in {zone['name']}.",
            "status": "pending_payout",
            "source": "manual",  
        }
        trigger_log.append(event)
        socketio.emit("trigger_fired", event)
        events_fired.append(event)

    log.info(
        f"Manual trigger: {trigger_type} in {zone['name']} "
        f"→ {len(events_fired)} workers notified"
    )

    return jsonify({
        "message": f"Manual trigger fired for {len(events_fired)} workers",
        "events": events_fired,
    })

@socketio.on("connect")
def on_connect():
    
    log.info(f"WebSocket client connected: {request.sid}")
    emit("connected", {
        "message": "Connected to GigShield trigger engine",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    })


@socketio.on("disconnect")
def on_disconnect():
    log.info(f"WebSocket client disconnected: {request.sid}")


@socketio.on("subscribe_worker")
def on_subscribe(data):
    worker_id = data.get("worker_id", "unknown")
    log.info(f"Worker {worker_id} subscribed to triggers")
    emit("subscribed", {"worker_id": worker_id, "status": "listening"})


def start_scheduler():
    scheduler = BackgroundScheduler()

    scheduler.add_job(
        func=poll_all_zones,       
        trigger="interval",       
        seconds=POLL_INTERVAL_SECONDS,
        id="poll_all_zones",      
        replace_existing=True,   
    )

    scheduler.start()
    log.info(
        f"Scheduler started: polling every {POLL_INTERVAL_SECONDS}s "
        f"({POLL_INTERVAL_SECONDS//60} min)"
    )
    return scheduler


def seed_demo_workers():
    demo_workers = [
        {"worker_id": "W001", "name": "Ravi Kumar",
        "zone_id": "zone_chennai_velachery", "persona": "food"},
        {"worker_id": "W002", "name": "Priya Selvan",
        "zone_id": "zone_chennai_velachery", "persona": "grocery"},
        {"worker_id": "W003", "name": "Arjun Nair",
        "zone_id": "zone_chennai_adyar", "persona": "ecommerce"},
        {"worker_id": "W004", "name": "Meena Devi",
        "zone_id": "zone_mumbai_bandra", "persona": "food"},
        {"worker_id": "W005", "name": "Karthik Raja",
        "zone_id": "zone_bengaluru_koramangala", "persona": "grocery"},
    ]

    for w in demo_workers:
        zone_id = w["zone_id"]
        if zone_id not in active_workers:
            active_workers[zone_id] = []
        active_workers[zone_id].append({
            "worker_id": w["worker_id"],
            "name": w["name"],
            "persona": w["persona"],
            "registered_at": datetime.now(timezone.utc).isoformat(),
        })

    log.info(f"Seeded {len(demo_workers)} demo workers across zones")


if __name__ == "__main__":
    log.info("Starting GigShield Parametric Trigger Engine...")
    seed_demo_workers()
    start_scheduler()
    log.info("Running initial poll cycle on startup...")
    poll_all_zones()
    log.info("Server starting on http://localhost:5001")
    socketio.run(app, host="0.0.0.0", port=5001, debug=False, allow_unsafe_werkzeug=True)