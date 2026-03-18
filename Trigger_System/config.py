

PLATFORM_API_URL = "http://localhost:5002/api/status"

AQI_API_URL = "https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b6f2-c1bfd384ba69"
AQI_API_KEY = "579b464db66ec23bdd000001f9849d09edf24aeb65c8fb5c8f0069ce"   # get free key at data.gov.in

# --- TRIGGER THRESHOLDS -------------------------------------
# These are the exact conditions that cause income loss.
# Each value was chosen based on IMD / NDMA guidelines.
THRESHOLDS = {
    "rainfall_mm": 30,
    "aqi": 200,
    "heat_celsius": 42,
    "platform_down_minutes": 45,
}

POLL_INTERVAL_SECONDS = 300

ZONES = {
    "zone_chennai_velachery": {
        "name": "Velachery",
        "lat": 12.9780,
        "lon": 80.2209,
        "city": "Chennai"
    },
    "zone_chennai_adyar": {
        "name": "Adyar",
        "lat": 13.0012,
        "lon": 80.2565,
        "city": "Chennai"
    },
    "zone_chennai_annanagar": {
        "name": "Anna Nagar",
        "lat": 13.0850,
        "lon": 80.2101,
        "city": "Chennai"
    },
    "zone_mumbai_bandra": {
        "name": "Bandra",
        "lat": 19.0596,
        "lon": 72.8295,
        "city": "Mumbai"
    },
    "zone_bengaluru_koramangala": {
        "name": "Koramangala",
        "lat": 12.9352,
        "lon": 77.6245,
        "city": "Bengaluru"
    },
}

PLATFORMS = ["swiggy", "zomato", "zepto", "blinkit", "amazon"]

DEDUP_WINDOW_SECONDS = 3600