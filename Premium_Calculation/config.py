MODEL_PATH    = "payout_model.joblib"
ENCODERS_PATH = "payout_encoders.joblib"

N_TRAINING_SAMPLES = 5000

# Fixed tier packages — worker picks one, premium is fixed
TIERS = {
    "basic"    : {"weekly_premium": 49,  "coverage_cap": 2000},
    "standard" : {"weekly_premium": 89,  "coverage_cap": 4500},
    "premium"  : {"weekly_premium": 149, "coverage_cap": 8000},
}

# Disrupted hours per trigger type — how long income stops
DISRUPTED_HOURS = {
    "rainfall"         : 4.0,
    "aqi"              : 6.0,
    "heat"             : 5.0,
    "platform_downtime": 2.0,
    "curfew"           : 8.0,
}

ZONE_DISRUPTION_FREQ = {
    "zone_chennai_velachery"     : 6.5,
    "zone_chennai_adyar"         : 5.8,
    "zone_chennai_annanagar"     : 3.2,
    "zone_mumbai_bandra"         : 5.5,
    "zone_bengaluru_koramangala" : 2.8,
}

PERSONA_SENSITIVITY = {
    "food"      : 1.20,
    "grocery"   : 1.00,
    "ecommerce" : 0.85,
}

SEASON_RISK = {
    "summer"  : 0.80,
    "monsoon" : 1.60,
    "winter"  : 0.70,
    "spring"  : 0.90,
}

# Features the payout model is trained on
FEATURE_COLS = [
    "zone_id", "persona", "trigger_type",
    "daily_earnings", "active_hours",
    "disrupted_hours", "season",
    "experience_months", "hourly_rate",
]

CATEGORICAL_COLS = ["zone_id", "persona", "trigger_type", "season"]
