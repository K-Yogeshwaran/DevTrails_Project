import os
from flask import Flask, jsonify, request
from datetime import datetime, timezone
from flask_cors import CORS
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = Flask(__name__)
CORS(app, resources={r"/api/*": {"origins": "*", "methods": ["GET", "POST", "OPTIONS"], "allow_headers": "*"}})

platform_state = {
    "swiggy": {
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    },
    "zomato": {
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    },
    "zepto": {
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    },
    "blinkit": {
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    },
    "amazon": {
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    },
}


def calculate_down_minutes(down_since_iso):
    
    if not down_since_iso:
        return 0

    down_since = datetime.fromisoformat(down_since_iso)
    now = datetime.now(timezone.utc)

    diff_seconds = (now - down_since).total_seconds()
    return int(diff_seconds / 60)

@app.route("/api/status", methods=["GET"])
def get_all_status():

    city_filter = request.args.get("city", "").lower()

    result = {}
    for name, state in platform_state.items():
        if city_filter and state["affected_cities"]:
            affected = [c.lower() for c in state["affected_cities"]]
            if city_filter not in affected:
                result[name] = {
                    "status": "operational",
                    "down_minutes": 0,
                    "affected_cities": state["affected_cities"],
                    "reason": None,
                }
                continue

        result[name] = {
            "status": state["status"],
            "down_minutes": calculate_down_minutes(state["down_since"]),
            "affected_cities": state["affected_cities"],
            "reason": state["reason"],
        }

    return jsonify(result)


@app.route("/api/status/<platform_name>", methods=["GET"])
def get_platform_status(platform_name):
    if platform_name not in platform_state:
        return jsonify({"error": f"Unknown platform: {platform_name}"}), 404

    state = platform_state[platform_name]
    return jsonify({
        "platform": platform_name,
        "status": state["status"],
        "down_minutes": calculate_down_minutes(state["down_since"]),
        "down_since": state["down_since"],
        "affected_cities": state["affected_cities"],
        "reason": state["reason"],
    })


@app.route("/api/status/<platform_name>/down", methods=["POST"])
def mark_platform_down(platform_name):
    if platform_name not in platform_state:
        return jsonify({"error": f"Unknown platform: {platform_name}"}), 404

    data = request.get_json() or {}

    platform_state[platform_name].update({
        "status": "down",
        # Record exact timestamp when it went down
        "down_since": datetime.now(timezone.utc).isoformat(),
        "affected_cities": data.get("affected_cities", []),
        "reason": data.get("reason", "Unknown"),
    })

    return jsonify({
        "message": f"{platform_name} marked as DOWN",
        "down_since": platform_state[platform_name]["down_since"],
        "reason": platform_state[platform_name]["reason"],
    })


@app.route("/api/status/<platform_name>/up", methods=["POST"])
def mark_platform_up(platform_name):
    if platform_name not in platform_state:
        return jsonify({"error": f"Unknown platform: {platform_name}"}), 404

    platform_state[platform_name].update({
        "status": "operational",
        "down_since": None,
        "affected_cities": [],
        "reason": None,
    })

    return jsonify({"message": f"{platform_name} marked as OPERATIONAL"})


@app.route("/api/status/<platform_name>/degraded", methods=["POST"])
def mark_platform_degraded(platform_name):
    """
    Marks a platform as degraded (slow but not fully down).
    Useful for partial outages — some workers affected, some not.
    """
    if platform_name not in platform_state:
        return jsonify({"error": f"Unknown platform: {platform_name}"}), 404

    data = request.get_json() or {}

    platform_state[platform_name].update({
        "status": "degraded",
        "down_since": datetime.now(timezone.utc).isoformat(),
        "affected_cities": data.get("affected_cities", []),
        "reason": data.get("reason", "Degraded performance"),
    })

    return jsonify({"message": f"{platform_name} marked as DEGRADED"})

import random

@app.route("/api/platform/verify-user", methods=["POST"])
def verify_platform_user():
    data = request.get_json()
    platform = data.get("platform")

    if not platform or platform not in platform_state:
        return jsonify({"error": "Invalid platform"}), 400

    avg_hours = random.randint(35, 60)         # weekly hours
    avg_income = random.randint(4000, 9000)    # weekly income

    experience = random.randint(3, 24)         # months
    days_per_week = random.randint(5, 7)

    shift = random.choice(["morning", "afternoon", "evening"])

    return jsonify({
        "platformUserId": f"{platform.upper()}_{random.randint(100,999)}",
        "avgWeeklyHours": avg_hours,
        "avgWeeklyIncome": avg_income,
        "experienceMonths": experience,
        "daysPerWeek": days_per_week,
        "shift": shift
    })


if __name__ == "__main__":
    # Get port from environment variable or use default
    mock_port = int(os.getenv("MOCK_API_PORT", "5002"))
    
    print(f"Mock Platform API running on http://localhost:{mock_port}")
    print("Endpoints:")
    print("  GET  /api/status")
    print("  GET  /api/status/<platform>")
    print("  POST /api/status/<platform>/down")
    print("  POST /api/status/<platform>/up")
    print("  POST /api/status/<platform>/degraded")
    print(" POST /api/status/<platform>/verify-user")
    app.run(host="0.0.0.0", port=mock_port, debug=False)