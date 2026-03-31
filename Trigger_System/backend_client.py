import logging
import requests
from config import BACKEND_API_URL

log = logging.getLogger(__name__)

TIMEOUT = 5


def create_trigger_event(event_id, trigger_type, zone_id, zone_name, trigger_value):
    try:
        res = requests.post(
            f"{BACKEND_API_URL}/api/trigger-events",
            json={
                "eventId":      event_id,
                "triggerType":  trigger_type,
                "zoneId":       zone_id,
                "zoneName":     zone_name,
                "triggerValue": trigger_value,
            },
            timeout=TIMEOUT,
        )
        if res.status_code in (200, 201):
            log.info(f"TriggerEvent saved to DB: {event_id}")
            return True
        else:
            log.warning(f"Backend rejected trigger event {event_id}: {res.status_code} {res.text}")
            return False
    except Exception as e:
        log.warning(f"Could not reach backend to save trigger event: {e}")
        return False


def resolve_trigger_event(event_id, active_worker_ids):
    """
    Uses POST body for eventId — avoids colon-in-URL path variable issues
    since event_ids look like: manual:zone_bengaluru_koramangala:rainfall:1234
    """
    try:
        res = requests.post(
            f"{BACKEND_API_URL}/api/trigger-events/resolve",
            json={
                "eventId":         event_id,
                "activeWorkerIds": active_worker_ids,
            },
            timeout=TIMEOUT,
        )
        if res.status_code == 200:
            log.info(f"TriggerEvent resolved: {event_id} | workers={active_worker_ids}")
            return True
        else:
            log.warning(f"Backend rejected resolve for {event_id}: {res.status_code} {res.text}")
            return False
    except Exception as e:
        log.warning(f"Could not reach backend to resolve trigger event: {e}")
        return False


def is_backend_healthy():
    try:
        res = requests.get(f"{BACKEND_API_URL}/api/workers/health", timeout=3)
        return res.status_code == 200
    except Exception:
        return False
