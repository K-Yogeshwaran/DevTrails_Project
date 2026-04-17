import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

from zones_india import INDIA_ZONES

# API Configuration
PLATFORM_API_URL  = os.getenv("PLATFORM_API_URL", "http://localhost:5002/api/status")
BACKEND_API_URL   = os.getenv("BACKEND_API_URL", "http://localhost:8080")

# External API Configuration
AQI_API_URL = os.getenv("AQI_API_URL", "https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b6f2-c1bfd384ba069")
AQI_API_KEY = os.getenv("AQI_API_KEY", "579b464db66ec23bdd000001f9849d09edf24aeb65c8fb5c8f0069ce")

THRESHOLDS = {
    "rainfall_mm":          30,
    "aqi":                  200,
    "heat_celsius":         42,
    "platform_down_minutes":45,
}

POLL_INTERVAL_SECONDS = 300

# ZONES is now the full India map — but the trigger engine only polls
# zones that are in Redis (i.e. have at least one active worker)
ZONES = INDIA_ZONES

PLATFORMS = ["swiggy", "zomato", "zepto", "blinkit", "amazon"]

DEDUP_WINDOW_SECONDS = 3600
