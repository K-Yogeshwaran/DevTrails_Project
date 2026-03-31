import logging
import time
from datetime import datetime, timezone
from flask import Flask, jsonify, request
from flask_socketio import SocketIO, emit
from flask_cors import CORS
from apscheduler.schedulers.background import BackgroundScheduler

from config import ZONES, POLL_INTERVAL_SECONDS, DEDUP_WINDOW_SECONDS
from zones_india import find_nearest_zone, get_zone
from trigger import run_all_checks
import redis_store
import backend_client

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

app = Flask(__name__)
app.config["SECRET_KEY"] = "gigshield-dev-secret-2026"
CORS(app, resources={r"/api/*": {"origins": "*", "methods": ["GET", "POST", "OPTIONS"], "allow_headers": "*"}})

socketio = SocketIO(app, cors_allowed_origins="*", async_mode="threading")

# In-memory dedup store — maps "zone_id:trigger_type" → unix timestamp
dedup_store = {}

# In-memory trigger log — last 100 events
trigger_log = []


# ── DEDUP ─────────────────────────────────────────────────────
def is_duplicate(zone_id, trigger_type):
    key = f"{zone_id}:{trigger_type}"
    now = time.time()
    if key in dedup_store:
        if now - dedup_store[key] < DEDUP_WINDOW_SECONDS:
            return True
    return False


def record_trigger(zone_id, trigger_type):
    dedup_store[f"{zone_id}:{trigger_type}"] = time.time()


# active_triggers: maps "zone_id:trigger_type" → event_id
# Tracks which disruptions are currently ongoing so we can detect when they end
active_triggers = {}


# ── POLL ──────────────────────────────────────────────────────
def poll_all_zones():
    log.info("── Poll cycle starting ──────────────────────────")

    if redis_store.is_available():
        active_zone_ids = redis_store.get_active_zones()
    else:
        log.warning("Redis unavailable — skipping poll")
        active_zone_ids = []

    if not active_zone_ids:
        log.info("No active zones in Redis — nothing to poll")
        return

    log.info(f"Polling {len(active_zone_ids)} active zone(s)")

    for zone_id in active_zone_ids:
        zone = ZONES.get(zone_id)
        if not zone:
            continue

        zone = dict(zone)
        zone["zone_id"] = zone_id
        results = run_all_checks(zone)

        for result in results:
            trigger_type = result["type"]
            active_key   = f"{zone_id}:{trigger_type}"

            if result["triggered"]:
                # ── NEW DISRUPTION ────────────────────────────
                if active_key not in active_triggers:
                    event_id = f"{zone_id}:{trigger_type}:{int(time.time())}"
                    active_triggers[active_key] = event_id

                    log.info(f"DISRUPTION STARTED: {trigger_type} in {zone['name']} | value={result['value']}")

                    # Save to DB via Spring Boot
                    backend_client.create_trigger_event(
                        event_id     = event_id,
                        trigger_type = trigger_type,
                        zone_id      = zone_id,
                        zone_name    = zone["name"],
                        trigger_value= result["value"] if isinstance(result["value"], (int, float)) else 0,
                    )

                    # Notify workers via WebSocket
                    workers = redis_store.get_workers_in_zone(zone_id) if redis_store.is_available() else []
                    for worker in workers:
                        event = {
                            "event_id":    event_id,
                            "trigger_type": trigger_type,
                            "zone_id":     zone_id,
                            "zone_name":   zone["name"],
                            "worker_id":   worker["worker_id"],
                            "worker_name": worker["name"],
                            "persona":     worker["persona"],
                            "triggered_at": datetime.now(timezone.utc).isoformat(),
                            "value":       result["value"],
                            "threshold":   result["threshold"],
                            "unit":        result["unit"],
                            "message":     result["message"],
                            "status":      "active",
                        }
                        trigger_log.append(event)
                        if len(trigger_log) > 100:
                            trigger_log.pop(0)
                        socketio.emit("trigger_fired", event)
                else:
                    log.info(f"Disruption still active: {trigger_type} in {zone['name']}")

            else:
                # ── CONDITION NORMALISED ──────────────────────
                if active_key in active_triggers:
                    event_id = active_triggers.pop(active_key)
                    log.info(f"DISRUPTION ENDED: {trigger_type} in {zone['name']}")

                    # Get workers still online — only they get the payout
                    workers = redis_store.get_workers_in_zone(zone_id) if redis_store.is_available() else []
                    live_worker_ids = [w["worker_id"] for w in workers]

                    # Resolve in DB — triggers claim creation
                    backend_client.resolve_trigger_event(event_id, live_worker_ids)

                    # Notify workers disruption ended
                    for worker in workers:
                        socketio.emit("trigger_resolved", {
                            "event_id":    event_id,
                            "trigger_type": trigger_type,
                            "zone_id":     zone_id,
                            "zone_name":   zone["name"],
                            "worker_id":   worker["worker_id"],
                            "message":     f"{trigger_type.replace('_',' ').title()} has ended in {zone['name']}. Claim is being processed.",
                        })

    log.info("── Poll cycle complete ──────────────────────────")


# ── REST ENDPOINTS ────────────────────────────────────────────

@app.route("/api/health", methods=["GET"])
def health():
    return jsonify({
        "status":         "ok",
        "timestamp":      datetime.now(timezone.utc).isoformat(),
        "total_zones":    len(ZONES),
        "active_zones":   len(redis_store.get_active_zones()) if redis_store.is_available() else 0,
        "active_workers": len(redis_store.get_all_active_workers()) if redis_store.is_available() else 0,
        "redis":          "connected" if redis_store.is_available() else "unavailable",
    })


@app.route("/api/zones", methods=["GET"])
def get_zones():
    return jsonify(ZONES)


@app.route("/api/zones/active", methods=["GET"])
def get_active_zones():
    if not redis_store.is_available():
        return jsonify({"error": "Redis unavailable"}), 503
    zone_ids = redis_store.get_active_zones()
    result = {zid: ZONES[zid] for zid in zone_ids if zid in ZONES}
    return jsonify(result)


@app.route("/api/workers/register", methods=["POST"])
def register_worker():
    data = request.get_json()

    required = ["worker_id", "name", "persona"]
    for field in required:
        if field not in data:
            return jsonify({"error": f"Missing field: {field}"}), 400

    lat     = data.get("lat")
    lon     = data.get("lon")
    zone_id = data.get("zone_id")

    # Resolve zone from GPS if provided, otherwise use supplied zone_id
    if lat is not None and lon is not None:
        zone_id = find_nearest_zone(float(lat), float(lon))
        log.info(f"GPS ({lat},{lon}) → nearest zone: {zone_id}")
    elif not zone_id:
        return jsonify({"error": "Provide either lat/lon or zone_id"}), 400

    if zone_id not in ZONES:
        return jsonify({"error": f"Unknown zone_id: {zone_id}"}), 400

    if redis_store.is_available():
        redis_store.register_worker(
            worker_id=data["worker_id"],
            name=data["name"],
            persona=data["persona"],
            zone_id=zone_id,
            lat=lat or ZONES[zone_id]["lat"],
            lon=lon or ZONES[zone_id]["lon"],
        )
    else:
        log.warning("Redis unavailable — worker registration not persisted")

    zone = ZONES[zone_id]
    return jsonify({
        "message":  "Worker registered successfully",
        "zone_id":  zone_id,
        "zone_name": zone["name"],
        "city":     zone["city"],
    }), 201


@app.route("/api/workers/deregister", methods=["POST"])
def deregister_worker():
    data      = request.get_json()
    worker_id = data.get("worker_id")
    zone_id   = data.get("zone_id")

    if not worker_id:
        return jsonify({"error": "worker_id required"}), 400

    if redis_store.is_available():
        # If zone_id not provided, look it up from Redis
        if not zone_id:
            all_workers = redis_store.get_all_active_workers()
            for w in all_workers:
                if w["worker_id"] == worker_id:
                    zone_id = w["zone_id"]
                    break
        if zone_id:
            redis_store.deregister_worker(worker_id, zone_id)

    log.info(f"Worker deregistered: {worker_id}")
    return jsonify({"message": "Worker deregistered"})


@app.route("/api/workers/active", methods=["GET"])
def get_active_workers():
    if not redis_store.is_available():
        return jsonify({"error": "Redis unavailable"}), 503

    workers = redis_store.get_all_active_workers()
    by_zone = {}
    for w in workers:
        zid = w["zone_id"]
        if zid not in by_zone:
            by_zone[zid] = {
                "zone_name": ZONES.get(zid, {}).get("name", zid),
                "city":      ZONES.get(zid, {}).get("city", ""),
                "workers":   [],
                "count":     0,
            }
        by_zone[zid]["workers"].append(w)
        by_zone[zid]["count"] += 1

    return jsonify({
        "zones":        by_zone,
        "total_active": len(workers),
    })


@app.route("/api/workers/resolve-zone", methods=["POST"])
def resolve_zone():
    """
    Given lat/lon, returns the nearest zone_id and zone details.
    Called by the frontend before going online.
    """
    data = request.get_json()
    lat  = data.get("lat")
    lon  = data.get("lon")

    if lat is None or lon is None:
        return jsonify({"error": "lat and lon required"}), 400

    zone_id = find_nearest_zone(float(lat), float(lon))
    zone    = ZONES[zone_id]

    return jsonify({
        "zone_id":   zone_id,
        "zone_name": zone["name"],
        "city":      zone["city"],
        "lat":       zone["lat"],
        "lon":       zone["lon"],
    })


@app.route("/api/triggers", methods=["GET"])
def get_triggers():
    zone_filter = request.args.get("zone_id")
    type_filter = request.args.get("type")
    filtered    = trigger_log
    if zone_filter:
        filtered = [t for t in filtered if t["zone_id"] == zone_filter]
    if type_filter:
        filtered = [t for t in filtered if t["trigger_type"] == type_filter]
    return jsonify(list(reversed(filtered)))


@app.route("/api/triggers/mark-processed", methods=["POST"])
def mark_processed():
    data      = request.get_json() or {}
    event_ids = data.get("event_ids", [])
    if not event_ids:
        return jsonify({"error": "event_ids list is required"}), 400
    updated = 0
    for event in trigger_log:
        if event["event_id"] in event_ids and event["status"] == "pending_payout":
            event["status"] = "processed"
            updated += 1
    log.info(f"Marked {updated} trigger events as processed")
    return jsonify({"message": f"{updated} events marked as processed"})


@app.route("/api/triggers/manual", methods=["POST"])
def manual_trigger():
    data         = request.get_json()
    zone_id      = data.get("zone_id")
    trigger_type = data.get("trigger_type", "rainfall")
    value        = data.get("value", 45)

    if zone_id not in ZONES:
        return jsonify({"error": "Unknown zone_id"}), 400

    zone     = ZONES[zone_id]
    workers  = redis_store.get_workers_in_zone(zone_id) if redis_store.is_available() else []
    event_id = f"manual:{zone_id}:{trigger_type}:{int(time.time())}"

    # Save to DB as active trigger event
    backend_client.create_trigger_event(
        event_id      = event_id,
        trigger_type  = trigger_type,
        zone_id       = zone_id,
        zone_name     = zone["name"],
        trigger_value = value if isinstance(value, (int, float)) else 0,
    )

    # Track in active_triggers so poll cycle can detect resolution
    active_key = f"{zone_id}:{trigger_type}"
    active_triggers[active_key] = event_id

    events_fired = []
    for worker in workers:
        event = {
            "event_id":     event_id,
            "trigger_type": trigger_type,
            "zone_id":      zone_id,
            "zone_name":    zone["name"],
            "worker_id":    worker["worker_id"],
            "worker_name":  worker["name"],
            "persona":      worker["persona"],
            "triggered_at": datetime.now(timezone.utc).isoformat(),
            "value":        value,
            "threshold":    "manual override",
            "message":      f"Manual trigger: {trigger_type.replace('_',' ')} in {zone['name']}.",
            "status":       "active",
            "source":       "manual",
        }
        trigger_log.append(event)
        socketio.emit("trigger_fired", event)
        events_fired.append(event)

    log.info(f"Manual trigger: {trigger_type} in {zone['name']} → {len(events_fired)} workers notified")
    return jsonify({
        "message":  f"Manual trigger fired for {len(events_fired)} workers",
        "event_id": event_id,
        "events":   events_fired,
    })


# ── WEBSOCKET ─────────────────────────────────────────────────
@socketio.on("connect")
def on_connect():
    log.info(f"WebSocket client connected: {request.sid}")
    emit("connected", {
        "message":   "Connected to GigShield trigger engine",
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


# ── SCHEDULER ─────────────────────────────────────────────────
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
    log.info(f"Scheduler started: polling every {POLL_INTERVAL_SECONDS}s")
    return scheduler


if __name__ == "__main__":
    log.info("Starting GigShield Parametric Trigger Engine...")
    if redis_store.is_available():
        log.info("Redis connected ✓")
    else:
        log.warning("Redis not available — workers must be in Redis to receive triggers")

    # Restore active_triggers from Spring Boot DB on startup
    # This handles the case where the trigger engine was restarted
    # but active events still exist in the DB
    try:
        import requests as req_lib
        from config import BACKEND_API_URL
        res = req_lib.get(f"{BACKEND_API_URL}/api/trigger-events/active", timeout=3)
        if res.status_code == 200:
            active_events = res.json()
            for ev in active_events:
                key = f"{ev['zoneId']}:{ev['triggerType']}"
                active_triggers[key] = ev['eventId']
                log.info(f"Restored active trigger: {key} -> {ev['eventId']}")
            log.info(f"Restored {len(active_events)} active triggers from DB")
    except Exception as e:
        log.warning(f"Could not restore active triggers from DB: {e}")

    start_scheduler()
    log.info("Running initial poll cycle on startup...")
    poll_all_zones()
    log.info(f"Server starting on http://localhost:5001 | {len(ZONES)} zones loaded")
    socketio.run(app, host="0.0.0.0", port=5001, debug=False, allow_unsafe_werkzeug=True)
