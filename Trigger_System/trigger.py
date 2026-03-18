import requests   
import random    
import logging 
from datetime import datetime, timezone
from config import  AQI_API_URL, AQI_API_KEY, THRESHOLDS, PLATFORMS

log = logging.getLogger(__name__)


def _safe_get(url, params=None, timeout=8):
    try:
        response = requests.get(url, params=params, timeout=timeout)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        log.warning(f"API call failed for {url}: {e}")
        return None


def check_aqi(zone):
    threshold = THRESHOLDS["aqi"]
    params = {
        "api-key": AQI_API_KEY,
        "format": "json",
        "filters[city]": zone["city"],  
        "limit": 5,                    
    }

    data = _safe_get(AQI_API_URL, params)

    aqi_value = None

    if data and "records" in data and len(data["records"]) > 0:
        record = data["records"][0]
        aqi_value = record.get("aqi") or record.get("pollutant_avg")

        if aqi_value:
            aqi_value = int(float(aqi_value))

    if aqi_value is None:
        aqi_value = random.randint(80, 320)
        log.info(f"[{zone['name']}] Using mock AQI: {aqi_value}")

    triggered = aqi_value >= threshold

    return {
        "type": "aqi",
        "zone_id": zone.get("zone_id"),
        "zone_name": zone["name"],
        "triggered": triggered,
        "value": aqi_value,
        "threshold": threshold,
        "unit": "AQI index",
        "message": (
            f"Hazardous air quality: AQI {aqi_value} in {zone['name']}. "
            f"Outdoor work unsafe."
        ) if triggered else (
            f"Air quality acceptable: AQI {aqi_value} in {zone['name']}."
        )
    }

def check_platform_downtime(zone):
    """
    Calls the platform status API to get real downtime values.
    In dev: calls our mock_platform_api.py on port 5002.
    In production: swap PLATFORM_API_URL in config.py — nothing else changes.
    """
    from config import PLATFORM_API_URL

    threshold_minutes = THRESHOLDS["platform_down_minutes"]

    # Call the status API, filtering by the zone's city
    # so we only get platforms affecting workers in this zone
    data = _safe_get(PLATFORM_API_URL, params={"city": zone["city"]})

    if data is None:
        # If the mock API is not running, fall back to all-operational
        log.warning(f"Platform API unreachable — assuming all platforms up")
        data = {p: {"status": "operational", "down_minutes": 0} for p in PLATFORMS}

    # Build platform_statuses in the same format as before
    # so the rest of the function stays identical
    platform_statuses = {
        name: info.get("down_minutes", 0)
        for name, info in data.items()
    }

    triggered_platforms = {
        name: mins
        for name, mins in platform_statuses.items()
        if mins >= threshold_minutes
    }

    triggered = len(triggered_platforms) > 0

    if triggered:
        down_list = ", ".join(
            f"{name} ({mins} min)"
            for name, mins in triggered_platforms.items()
        )
        message = f"Platform outage in {zone['name']}: {down_list} down."
    else:
        message = f"All platforms operational in {zone['name']}."

    return {
        "type": "platform_downtime",
        "zone_id": zone.get("zone_id"),
        "zone_name": zone["name"],
        "triggered": triggered,
        "value": platform_statuses,
        "triggered_platforms": triggered_platforms,
        "threshold": threshold_minutes,
        "unit": "minutes down",
        "message": message,
    }

def check_curfew(zone):
    """
    Checks for active curfews or Section 144 in a zone.
    Real source: district NIC portals, state disaster mgmt APIs.
    For demo: we mock this with a low probability trigger.

    In Phase 2 we can scrape the relevant government portals.
    """
    # 10% chance of an active curfew in any zone at any time.
    # Low probability makes the demo realistic — curfews are rare.
    has_curfew = random.random() < 0.10

    if has_curfew:
        # Pick a random restriction type for variety
        restriction_types = [
            "Section 144 CrPC — assembly restricted",
            "Local strike — roads blocked",
            "Night curfew — 10PM to 6AM",
            "Flood evacuation zone — no entry",
        ]
        # random.choice() picks one item from the list
        restriction = random.choice(restriction_types)
        message = f"Active restriction in {zone['name']}: {restriction}."
    else:
        restriction = None
        message = f"No restrictions active in {zone['name']}."

    return {
        "type": "curfew",
        "zone_id": zone.get("zone_id"),
        "zone_name": zone["name"],
        "triggered": has_curfew,
        "value": restriction,             # what the restriction is
        "threshold": "any active order",  # any restriction = trigger
        "unit": "restriction type",
        "message": message,
    }
    
# In triggers.py — replace the OWM URL and params with this:
def check_rainfall(zone):
    threshold = THRESHOLDS["rainfall_mm"]

    url = "https://api.open-meteo.com/v1/forecast"
    params = {
        "latitude": zone["lat"],
        "longitude": zone["lon"],
        "current": "precipitation,rain",
        "hourly": "precipitation",
        "forecast_days": 1,
    }

    data = _safe_get(url, params)
    rain_3h_estimate = None

    # Only try to parse if we actually got data back
    if data and isinstance(data, dict):
        current = data.get("current")
        if current and isinstance(current, dict):
            rain_1h = current.get("precipitation", 0) or 0
            rain_3h_estimate = round(rain_1h * 3, 1)

    # If API failed OR parsing failed, use mock
    if rain_3h_estimate is None:
        rain_3h_estimate = round(random.uniform(0, 60), 1)
        log.info(f"[{zone['name']}] Using mock rainfall: {rain_3h_estimate}mm")

    triggered = rain_3h_estimate >= threshold

    return {
        "type": "rainfall",
        "zone_id": zone.get("zone_id"),
        "zone_name": zone["name"],
        "triggered": triggered,
        "value": rain_3h_estimate,
        "threshold": threshold,
        "unit": "mm (3h estimate)",
        "message": (
            f"Heavy rainfall: {rain_3h_estimate}mm in {zone['name']}. Deliveries halted."
        ) if triggered else (
            f"Rainfall normal: {rain_3h_estimate}mm in {zone['name']}."
        )
    }
    
def check_heat(zone):
    threshold = THRESHOLDS["heat_celsius"]

    url = "https://api.open-meteo.com/v1/forecast"
    params = {
        "latitude": zone["lat"],
        "longitude": zone["lon"],
        "current": "temperature_2m",
        "forecast_days": 1,
    }

    data = _safe_get(url, params)
    temp = None

    # Only try to parse if we actually got data back
    if data and isinstance(data, dict):
        current = data.get("current")
        if current and isinstance(current, dict):
            temp = current.get("temperature_2m")

    # If API failed OR parsing failed, use mock
    if temp is None:
        temp = round(random.uniform(22, 46), 1)
        log.info(f"[{zone['name']}] Using mock temp: {temp}°C")

    triggered = temp >= threshold

    return {
        "type": "heat",
        "zone_id": zone.get("zone_id"),
        "zone_name": zone["name"],
        "triggered": triggered,
        "value": temp,
        "threshold": threshold,
        "unit": "°C",
        "message": (
            f"Extreme heat: {temp}°C in {zone['name']}. Outdoor delivery unsafe."
        ) if triggered else (
            f"Temperature normal: {temp}°C in {zone['name']}."
        )
    }



def run_all_checks(zone):
    checks = [
        check_rainfall,
        check_aqi,
        check_heat,
        check_platform_downtime,
        check_curfew,
    ]

    results = []
    for check in checks:
        try:
            result = check(zone)
            if result is not None:
                results.append(result)
            else:
                log.warning(f"{check.__name__} returned None for zone {zone['name']}")
        except Exception as e:
            log.error(f"{check.__name__} crashed: {e}")

    now = datetime.now(timezone.utc).isoformat()
    for r in results:
        r["checked_at"] = now

    return results