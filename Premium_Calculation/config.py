MODEL_PATH         = "premium_model.joblib"
ENCODERS_PATH      = "premium_encoders.joblib"


N_TRAINING_SAMPLES = 5000


ZONE_DISRUPTION_FREQ = {
    "zone_chennai_velachery"     : 6.5,
    "zone_chennai_adyar"         : 5.8,
    "zone_chennai_annanagar"     : 3.2,
    "zone_mumbai_bandra"         : 5.5,
    "zone_bengaluru_koramangala" : 2.8,
}


PERSONA_SENSITIVITY = {
    "food"      : 1.20,   # orders stop instantly when it rains
    "grocery"   : 1.00,   # baseline
    "ecommerce" : 0.85,   # some buffer — indoor pickups possible
}


SHIFT_RISK = {
    "morning"   : 0.90,
    "afternoon" : 1.00,
    "evening"   : 1.10,
    "night"     : 1.25,
}


SEASON_RISK = {
    "summer"  : 0.80,
    "monsoon" : 1.60,
    "winter"  : 0.70,
    "spring"  : 0.90,
}


TIERS = {
    "basic"    : {"weekly_premium": 49,  "coverage_cap": 2000},
    "standard" : {"weekly_premium": 89,  "coverage_cap": 4500},
    "premium"  : {"weekly_premium": 149, "coverage_cap": 8000},
}


FEATURE_COLS = [
    "zone_id", "persona", "daily_earnings", "active_hours",
    "shift", "season", "days_per_week", "experience_months",
    "hourly_rate", "weekly_earnings", "disruptions_week",
]


CATEGORICAL_COLS = ["zone_id", "persona", "shift", "season"]