import json
import logging
import redis

log = logging.getLogger(__name__)

REDIS_HOST = "localhost"
REDIS_PORT = 6379
REDIS_DB   = 0

# Key patterns
# active_worker:{worker_id}  → JSON worker record
# active_zones               → Redis Set of zone_ids that have at least one active worker

_client = None

def get_client():
    global _client
    if _client is None:
        _client = redis.Redis(
            host=REDIS_HOST,
            port=REDIS_PORT,
            db=REDIS_DB,
            decode_responses=True,
            socket_connect_timeout=3,
        )
    return _client


def is_available():
    try:
        get_client().ping()
        return True
    except Exception:
        return False


def register_worker(worker_id, name, persona, zone_id, lat, lon):
    """
    Store worker as active in Redis.
    Adds their zone to the active_zones set so the trigger engine picks it up.
    TTL of 12 hours — auto-expires if worker forgets to go offline.
    """
    r = get_client()
    record = {
        "worker_id": worker_id,
        "name":      name,
        "persona":   persona,
        "zone_id":   zone_id,
        "lat":       lat,
        "lon":       lon,
    }
    key = f"active_worker:{worker_id}"
    r.setex(key, 43200, json.dumps(record))   # 12 hour TTL
    r.sadd("active_zones", zone_id)
    log.info(f"Redis: worker {worker_id} registered in zone {zone_id}")


def deregister_worker(worker_id, zone_id):
    """
    Remove worker from Redis.
    If no other workers remain in that zone, remove zone from active_zones.
    """
    r = get_client()
    r.delete(f"active_worker:{worker_id}")

    # Check if any other workers are still in this zone
    remaining = get_workers_in_zone(zone_id)
    if not remaining:
        r.srem("active_zones", zone_id)
        log.info(f"Redis: zone {zone_id} removed from active_zones (no workers left)")

    log.info(f"Redis: worker {worker_id} deregistered from zone {zone_id}")


def get_active_zones():
    """Returns list of zone_ids that currently have active workers."""
    try:
        return list(get_client().smembers("active_zones"))
    except Exception as e:
        log.warning(f"Redis get_active_zones failed: {e}")
        return []


def get_workers_in_zone(zone_id):
    """Returns all active worker records for a given zone."""
    r = get_client()
    workers = []
    # Scan all active_worker keys — efficient for small sets
    for key in r.scan_iter("active_worker:*"):
        try:
            data = r.get(key)
            if data:
                worker = json.loads(data)
                if worker.get("zone_id") == zone_id:
                    workers.append(worker)
        except Exception:
            continue
    return workers


def get_all_active_workers():
    """Returns all active workers across all zones."""
    r = get_client()
    workers = []
    for key in r.scan_iter("active_worker:*"):
        try:
            data = r.get(key)
            if data:
                workers.append(json.loads(data))
        except Exception:
            continue
    return workers


def is_worker_active(worker_id):
    return get_client().exists(f"active_worker:{worker_id}") == 1
