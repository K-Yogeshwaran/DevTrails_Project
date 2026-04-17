import json
import logging
import os
from dotenv import load_dotenv
import requests

# Load environment variables from .env file
load_dotenv()

log = logging.getLogger(__name__)

# Redis Configuration


UPSTASH_URL = os.getenv("UPSTASH_REDIS_REST_URL")
UPSTASH_TOKEN = os.getenv("UPSTASH_REDIS_REST_TOKEN")

def upstash_request(command, *args):
    try:
        # Convert args to URL path
        path = "/".join([command] + [str(arg) for arg in args])
        url = f"{UPSTASH_URL}/{path}"

        headers = {
            "Authorization": f"Bearer {UPSTASH_TOKEN}"
        }

        response = requests.post(url, headers=headers, timeout=3)

        if response.status_code != 200:
            log.error(f"Upstash HTTP error: {response.status_code} {response.text}")
            return {"result": None}

        data = response.json()
        log.info(f"Upstash {command} → {data}")

        return data

    except Exception as e:
        log.error(f"Upstash request failed: {e}")
        return {"result": None}


def is_available():
    res = upstash_request("ping")

    result = res.get("result")

    # Normalize result
    if isinstance(result, str):
        result = result.strip().upper()

    return result in ["PONG", "[]"] or result == []

def register_worker(worker_id, name, persona, zone_id, lat, lon):
    record = {
        "worker_id": worker_id,
        "name": name,
        "persona": persona,
        "zone_id": zone_id,
        "lat": lat,
        "lon": lon,
    }

    key = f"active_worker:{worker_id}"

    # SET with TTL (EX = seconds)
    upstash_request("set", key, json.dumps(record), "EX", 43200)

    # Add zone to set
    upstash_request("sadd", "active_zones", zone_id)

    log.info(f"Upstash: worker {worker_id} registered in zone {zone_id}")


def deregister_worker(worker_id, zone_id):
    upstash_request("del", f"active_worker:{worker_id}")

    remaining = get_workers_in_zone(zone_id)

    if not remaining:
        upstash_request("srem", "active_zones", zone_id)
        log.info(f"Upstash: zone {zone_id} removed")

    log.info(f"Upstash: worker {worker_id} deregistered")


def get_active_zones():
    try:
        res = upstash_request("smembers", "active_zones")
        return res.get("result", [])
    except Exception as e:
        log.warning(f"Upstash error: {e}")
        return []


def get_workers_in_zone(zone_id):
    workers = get_all_active_workers()
    return [w for w in workers if w.get("zone_id") == zone_id]


def get_all_active_workers():
    workers = []
    cursor = "0"

    while True:
        res = upstash_request("scan", cursor, "MATCH", "active_worker:*")
        cursor, keys = res.get("result", ["0", []])

        for key in keys:
            data = upstash_request("get", key).get("result")
            if data:
                workers.append(json.loads(data))

        if cursor == "0":
            break

    return workers


def is_worker_active(worker_id):
    res = upstash_request("exists", f"active_worker:{worker_id}")
    return res.get("result") == 1