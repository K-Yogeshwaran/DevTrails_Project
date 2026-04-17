import random
import logging
import pandas as pd
from config import (
    ZONE_DISRUPTION_FREQ, PERSONA_SENSITIVITY,
    SEASON_RISK, DISRUPTED_HOURS, N_TRAINING_SAMPLES
)

log = logging.getLogger(__name__)


def generate_training_data(n_samples=N_TRAINING_SAMPLES):
    log.info(f"Generating {n_samples} synthetic claim records for payout model...")
    records = []

    trigger_types = list(DISRUPTED_HOURS.keys())

    for _ in range(n_samples):

        zone_id = random.choice(list(ZONE_DISRUPTION_FREQ.keys()))

        persona = random.choices(
            ["food", "grocery", "ecommerce"],
            weights=[50, 30, 20]
        )[0]

        earnings_range = {
            "food"      : (600,  1400),
            "grocery"   : (500,  1200),
            "ecommerce" : (700,  1600),
        }
        min_e, max_e   = earnings_range[persona]
        daily_earnings = round(random.randint(min_e, max_e) / 50) * 50

        active_hours      = random.choice([4, 5, 6, 7, 8, 9, 10, 11, 12])
        season            = random.choice(list(SEASON_RISK.keys()))
        experience_months = random.randint(1, 48)
        trigger_type      = random.choice(trigger_types)

        # Base disrupted hours for this trigger type
        # Add ±20% noise — real disruptions vary in duration
        base_disrupted = DISRUPTED_HOURS[trigger_type]
        disrupted_hours = round(base_disrupted * random.uniform(0.8, 1.2), 1)
        # Can't lose more hours than you actually work
        disrupted_hours = min(disrupted_hours, active_hours)

        hourly_rate     = daily_earnings / active_hours
        persona_factor  = PERSONA_SENSITIVITY[persona]
        season_factor   = SEASON_RISK[season]

        # Experience discount — up to 15% for 48 months
        experience_discount = min(experience_months / 48 * 0.15, 0.15)

        # Payout = hourly_rate × disrupted_hours × persona_factor × season_factor × (1 - discount)
        # This is what the ML model learns to predict
        payout = (
            hourly_rate
            * disrupted_hours
            * persona_factor
            * season_factor
            * (1 - experience_discount)
        )

        # Small noise ±8% — real payouts have variance
        payout = round(payout * random.uniform(0.92, 1.08), 2)
        payout = max(50, min(5000, payout))

        records.append({
            "zone_id"          : zone_id,
            "persona"          : persona,
            "trigger_type"     : trigger_type,
            "daily_earnings"   : daily_earnings,
            "active_hours"     : active_hours,
            "disrupted_hours"  : disrupted_hours,
            "season"           : season,
            "experience_months": experience_months,
            "hourly_rate"      : round(hourly_rate, 2),
            # Label — what the model predicts
            "payout_amount"    : payout,
        })

    df = pd.DataFrame(records)
    log.info(
        f"Done. Payout range: "
        f"₹{df['payout_amount'].min():.0f} – ₹{df['payout_amount'].max():.0f} | "
        f"Mean: ₹{df['payout_amount'].mean():.0f}"
    )
    return df
