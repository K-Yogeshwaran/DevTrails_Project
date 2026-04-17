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
    SEASON_RISK, DISRUPTED_HOURS, TIERS
)
from data_generator import generate_training_data

log = logging.getLogger(__name__)


def train_model():
    df = generate_training_data()

    X = df[FEATURE_COLS].copy()
    y = df["payout_amount"]

    encoders = {}
    for col in CATEGORICAL_COLS:
        le = LabelEncoder()
        X[col] = le.fit_transform(X[col])
        encoders[col] = le

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    log.info(f"Training: {len(X_train)} samples | Test: {len(X_test)} samples")

    model = xgb.XGBRegressor(
        n_estimators    = 200,
        max_depth       = 6,
        learning_rate   = 0.1,
        subsample       = 0.8,
        colsample_bytree= 0.8,
        random_state    = 42,
        verbosity       = 0
    )

    log.info("Training XGBoost payout model...")
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    mae    = mean_absolute_error(y_test, y_pred)
    r2     = r2_score(y_test, y_pred)
    log.info(f"Accuracy — MAE: ₹{mae:.2f} | R²: {r2:.4f}")

    importance = dict(zip(FEATURE_COLS, model.feature_importances_))
    top5 = sorted(importance.items(), key=lambda x: x[1], reverse=True)[:5]
    log.info("Top 5 features driving the payout:")
    for feat, score in top5:
        log.info(f"  {feat}: {score:.4f}")

    joblib.dump(model, MODEL_PATH)
    joblib.dump(encoders, ENCODERS_PATH)
    log.info(f"Saved model → {MODEL_PATH}")
    log.info(f"Saved encoders → {ENCODERS_PATH}")


def load_model():
    if not os.path.exists(MODEL_PATH):
        log.warning(f"No model found at {MODEL_PATH}. Will train on first request.")
        return None, None
    model    = joblib.load(MODEL_PATH)
    encoders = joblib.load(ENCODERS_PATH)
    log.info("Payout model and encoders loaded successfully.")
    return model, encoders


def predict_payout(claim_data, model, encoders):
    """
    Predicts the payout amount for a triggered claim.

    Inputs (claim_data dict):
      zone_id, persona, trigger_type,
      daily_earnings, active_hours,
      disrupted_hours, season, experience_months
    """
    daily_earnings    = claim_data["daily_earnings"]
    active_hours      = claim_data["active_hours"]
    hourly_rate       = daily_earnings / active_hours
    disrupted_hours   = claim_data.get(
        "disrupted_hours",
        DISRUPTED_HOURS.get(claim_data.get("trigger_type", "rainfall"), 4.0)
    )

    row = {
        "zone_id"          : claim_data["zone_id"],
        "persona"          : claim_data["persona"],
        "trigger_type"     : claim_data["trigger_type"],
        "daily_earnings"   : daily_earnings,
        "active_hours"     : active_hours,
        "disrupted_hours"  : disrupted_hours,
        "season"           : claim_data["season"],
        "experience_months": claim_data["experience_months"],
        "hourly_rate"      : round(hourly_rate, 2),
    }

    df_input = pd.DataFrame([row])

    for col in CATEGORICAL_COLS:
        le  = encoders[col]
        val = df_input[col].values[0]
        if val not in le.classes_:
            log.warning(f"Unseen value '{val}' for '{col}'. Using fallback.")
            df_input[col] = 0
        else:
            df_input[col] = le.transform([val])[0]

    predicted = float(model.predict(df_input[FEATURE_COLS])[0])
    predicted = max(50, min(5000, round(predicted, 2)))

    explanation = {
        "hourly_rate"      : f"₹{hourly_rate:.0f}/hr",
        "disrupted_hours"  : f"{disrupted_hours}hrs",
        "trigger_type"     : claim_data["trigger_type"],
        "persona_factor"   : f"{PERSONA_SENSITIVITY.get(claim_data['persona'], 1.0)}×",
        "season_factor"    : f"{SEASON_RISK.get(claim_data['season'], 1.0)}× ({claim_data['season']})",
        "model_output"     : f"₹{predicted}",
    }

    return {
        "worker_id"    : claim_data.get("worker_id", "unknown"),
        "payout_amount": predicted,
        "disrupted_hours": disrupted_hours,
        "explanation"  : explanation,
    }
