import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
from ml_engine import load_model, predict_payout, train_model
from config import TIERS

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
log = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app, origins=["https://gigshield-devtrails-techx.vercel.app", "http://localhost:5173"])

_model, _encoders = load_model()
if _model is None:
    log.info("No model found. Training payout model now...")
    train_model()
    _model, _encoders = load_model()


@app.route("/api/payout/calculate", methods=["POST"])
def calculate_payout():
    if _model is None:
        return jsonify({"error": "Model not available"}), 503

    data = request.get_json()
    required = [
        "zone_id", "persona", "trigger_type",
        "daily_earnings", "active_hours",
        "season", "experience_months"
    ]
    missing = [f for f in required if f not in data]
    if missing:
        return jsonify({"error": f"Missing fields: {missing}"}), 400

    result = predict_payout(data, _model, _encoders)
    return jsonify(result)


@app.route("/api/payout/tiers", methods=["GET"])
def get_tiers():
    """Returns fixed tier packages for the frontend to display."""
    return jsonify(TIERS)


@app.route("/api/payout/health", methods=["GET"])
def health():
    return jsonify({
        "status"      : "ok" if _model else "model_not_loaded",
        "model_loaded": _model is not None,
        "port"        : 5003,
    })


if __name__ == "__main__":
    log.info("GigShield Payout Calculator API running on http://localhost:5003")
    app.run(host="0.0.0.0", port=5003, debug=False)
