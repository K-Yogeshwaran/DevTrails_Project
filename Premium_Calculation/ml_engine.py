import logging
import os
import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import mean_absolute_error, r2_score
import xgboost as xgb

from config import (
    MODEL_PATH, ENCODERS_PATH,
    FEATURE_COLS, CATEGORICAL_COLS,
    ZONE_DISRUPTION_FREQ, PERSONA_SENSITIVITY,
    SHIFT_RISK, SEASON_RISK, TIERS
)
from data_generator import generate_training_data

log = logging.getLogger(__name__)

def train_model():
    df = generate_training_data()

    X = df[FEATURE_COLS].copy()
    y = df["weekly_premium"]
    encoders = {}
    for col in CATEGORICAL_COLS:
        le = LabelEncoder()
        X[col] = le.fit_transform(X[col])
        # Save encoder — we need the EXACT same mapping at prediction time
        encoders[col] = le

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    log.info(f"Training: {len(X_train)} samples | Test: {len(X_test)} samples")

    model = xgb.XGBRegressor(
        n_estimators    = 200,   # number of decision trees
        max_depth       = 6,     # how deep each tree grows
        learning_rate   = 0.1,   # how much each tree corrects the previous
        subsample       = 0.8,   # 80% of data used per tree (prevents overfitting)
        colsample_bytree= 0.8,   # 80% of features used per tree
        random_state    = 42,
        verbosity       = 0
    )

    log.info("Training XGBoost model...")
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    mae    = mean_absolute_error(y_test, y_pred)
    r2     = r2_score(y_test, y_pred)
    log.info(f"Accuracy — MAE: ₹{mae:.2f} | R²: {r2:.4f}")


    importance = dict(zip(FEATURE_COLS, model.feature_importances_))
    top5 = sorted(importance.items(), key=lambda x: x[1], reverse=True)[:5]
    log.info("Top 5 features driving the premium:")
    for feat, score in top5:
        log.info(f"  {feat}: {score:.4f}")

    joblib.dump(model, MODEL_PATH)
    joblib.dump(encoders, ENCODERS_PATH)
    log.info(f"Saved model → {MODEL_PATH}")
    log.info(f"Saved encoders → {ENCODERS_PATH}")



def load_model():
    if not os.path.exists(MODEL_PATH):
        log.warning(f"No model found at {MODEL_PATH}. Run train first.")
        return None, None

    model    = joblib.load(MODEL_PATH)
    encoders = joblib.load(ENCODERS_PATH)
    log.info("Model and encoders loaded successfully.")
    return model, encoders

def predict_premium(worker_data, model, encoders):

    daily_earnings   = worker_data["daily_earnings"]
    active_hours     = worker_data["active_hours"]
    hourly_rate      = daily_earnings / active_hours
    weekly_earnings  = daily_earnings * worker_data["days_per_week"]

    zone_id          = worker_data["zone_id"]
    season           = worker_data["season"]
    zone_freq        = ZONE_DISRUPTION_FREQ.get(zone_id, 4.0)
    season_factor    = SEASON_RISK.get(season, 1.0)
    disruptions_week = (zone_freq * season_factor) / 4.33
    row = {
        "zone_id"           : zone_id,
        "persona"           : worker_data["persona"],
        "daily_earnings"    : daily_earnings,
        "active_hours"      : active_hours,
        "shift"             : worker_data["shift"],
        "season"            : season,
        "days_per_week"     : worker_data["days_per_week"],
        "experience_months" : worker_data["experience_months"],
        "hourly_rate"       : round(hourly_rate, 2),
        "weekly_earnings"   : weekly_earnings,
        "disruptions_week"  : round(disruptions_week, 3),
    }
    df_input = pd.DataFrame([row])


    for col in CATEGORICAL_COLS:
        le  = encoders[col]
        val = df_input[col].values[0]
        if val not in le.classes_:
            # Unseen zone or persona → safe fallback
            log.warning(f"Unseen value '{val}' for '{col}'. Using fallback.")
            df_input[col] = 0
        else:
            df_input[col] = le.transform([val])[0]

    # Step 4: Predict
    predicted = float(model.predict(df_input[FEATURE_COLS])[0])
    predicted = max(25, min(300, round(predicted, 2)))

    # Step 5: Recommended tier based on daily earnings
    if daily_earnings >= 1200:
        tier = "premium"
    elif daily_earnings >= 700:
        tier = "standard"
    else:
        tier = "basic"

    explanation = {
        "hourly_rate"         : f"₹{hourly_rate:.0f}/hr",
        "weekly_earnings"     : f"₹{weekly_earnings}",
        "disruptions_per_week": f"{disruptions_week:.1f}",
        "zone_risk"           : f"{zone_freq} disruptions/month",
        "season_factor"       : f"{season_factor}× ({season})",
        "persona_sensitivity" : f"{PERSONA_SENSITIVITY.get(worker_data['persona'], 1.0)}×",
        "affordability_check" : f"₹{weekly_earnings * 0.018:.0f} (1.8% cap)",
        "model_output"        : f"₹{predicted}",
    }

    return {
        "worker_id"       : worker_data.get("worker_id", "unknown"),
        "weekly_premium"  : predicted,
        "recommended_tier": tier,
        "tier_details"    : TIERS[tier],
        "explanation"     : explanation,
        "confidence"      : "high" if worker_data["experience_months"] > 3 else "medium",
    }