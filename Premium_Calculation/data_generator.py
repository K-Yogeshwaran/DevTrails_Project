import random
import logging
import pandas as pd
from config import (
    ZONE_DISRUPTION_FREQ, PERSONA_SENSITIVITY,
    SHIFT_RISK, SEASON_RISK, N_TRAINING_SAMPLES
)

log = logging.getLogger(__name__)


def generate_training_data(n_samples=N_TRAINING_SAMPLES):
    log.info(f"Generating {n_samples} synthetic worker records...")
    records = []

    for _ in range(n_samples):

        # ── Pick random worker attributes ─────────────────────

        zone_id = random.choice(list(ZONE_DISRUPTION_FREQ.keys()))

        # Food delivery is most common (~50% of gig workers in India)
        persona = random.choices(
            ["food", "grocery", "ecommerce"],
            weights=[50, 30, 20]
        )[0]

        # Daily earnings vary by persona type
        earnings_range = {
            "food"      : (600,  1400),
            "grocery"   : (500,  1200),
            "ecommerce" : (700,  1600),
        }
        min_e, max_e = earnings_range[persona]
        # Round to nearest 50 — cleaner numbers, more realistic
        daily_earnings = round(random.randint(min_e, max_e) / 50) * 50

        # Most workers do 6–10 hours per day
        active_hours = random.choice([4, 5, 6, 7, 8, 9, 10, 11, 12])

        # Afternoon is most popular shift
        shift = random.choices(
            list(SHIFT_RISK.keys()),
            weights=[20, 40, 25, 15]
        )[0]

        season = random.choice(list(SEASON_RISK.keys()))

        # Most gig workers do 5–6 days per week
        # Duplicating 5 and 6 makes them more likely (weighted randomness)
        days_per_week = random.choice([4, 5, 5, 6, 6, 7])

        # 1–48 months experience
        experience_months = random.randint(1, 48)

        # City extracted from zone id
        # "zone_chennai_velachery" → "chennai"
        city = zone_id.split("_")[1]

        # ── Calculate the correct premium for this worker ─────
        # This becomes the "label" — what the model learns to predict

        hourly_rate     = daily_earnings / active_hours
        weekly_earnings = daily_earnings * days_per_week

        # Expected disruptions per week for this zone in this season
        zone_freq        = ZONE_DISRUPTION_FREQ[zone_id]
        season_factor    = SEASON_RISK[season]
        disruptions_week = (zone_freq * season_factor) / 4.33
        # 4.33 = average weeks per month

        # Average hours lost per disruption event
        # Can't lose more hours than you actually work
        avg_disrupted_hours = min(active_hours * 0.5, 4)

        # Expected weekly income loss
        persona_factor = PERSONA_SENSITIVITY[persona]
        shift_factor   = SHIFT_RISK[shift]
        expected_loss  = (
            hourly_rate
            * avg_disrupted_hours
            * disruptions_week
            * persona_factor
            * shift_factor
        )

        # Experienced workers have better route knowledge
        # Up to 15% discount after 48 months
        experience_discount = min(experience_months / 48 * 0.15, 0.15)
        expected_loss = expected_loss * (1 - experience_discount)

        # Actuarial premium: insurer keeps 35%, pays out 65%
        actuarial_premium = expected_loss * 0.65

        # Affordability cap: worker pays max 1.8% of weekly earnings
        affordability_cap = weekly_earnings * 0.018

        # Final premium is the higher of the two
        # Small random noise ±10% makes data realistic
        # (real premiums have variance due to underwriter judgment)
        base_premium  = max(actuarial_premium, affordability_cap)
        noise         = random.uniform(0.90, 1.10)
        final_premium = round(base_premium * noise, 2)

        # Clamp to realistic range
        final_premium = max(25, min(300, final_premium))

        # Recommended tier based on daily earnings
        if daily_earnings >= 1200:
            recommended_tier = "premium"
        elif daily_earnings >= 700:
            recommended_tier = "standard"
        else:
            recommended_tier = "basic"

        records.append({
            # ── Features (inputs to the model) ──
            "zone_id"           : zone_id,
            "persona"           : persona,
            "daily_earnings"    : daily_earnings,
            "active_hours"      : active_hours,
            "shift"             : shift,
            "season"            : season,
            "days_per_week"     : days_per_week,
            "experience_months" : experience_months,
            "city"              : city,
            "hourly_rate"       : round(hourly_rate, 2),
            "weekly_earnings"   : weekly_earnings,
            "disruptions_week"  : round(disruptions_week, 3),
            # ── Target (what the model learns to predict) ──
            "weekly_premium"    : final_premium,
            "recommended_tier"  : recommended_tier,
        })

    df = pd.DataFrame(records)
    log.info(
        f"Done. Premium range: "
        f"₹{df['weekly_premium'].min():.0f} – ₹{df['weekly_premium'].max():.0f} | "
        f"Mean: ₹{df['weekly_premium'].mean():.0f}"
    )
    return df