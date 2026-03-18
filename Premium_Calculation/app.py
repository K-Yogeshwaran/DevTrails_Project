import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
from ml_engine import load_model, predict_premium, train_model
from config import TIERS

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
log = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)


_model, _encoders = load_model()
if _model is None:
    log.info("No model found. Training now...")
    train_model()
    _model, _encoders = load_model()



@app.route("/api/premium/calculate", methods=["POST"])
def calculate():
    if _model is None:
        return jsonify({"error": "Model not available"}), 503

    data     = request.get_json()
    required = [
        "zone_id", "persona", "daily_earnings", "active_hours",
        "shift", "season", "days_per_week", "experience_months"
    ]

    # Check all required fields are present
    missing = [f for f in required if f not in data]
    if missing:
        return jsonify({"error": f"Missing fields: {missing}"}), 400

    result = predict_premium(data, _model, _encoders)
    return jsonify(result)


@app.route("/api/premium/batch", methods=["POST"])
def batch_calculate():
    if _model is None:
        return jsonify({"error": "Model not available"}), 503

    workers = request.get_json().get("workers", [])
    results = []

    for worker in workers:
        try:
            result = predict_premium(worker, _model, _encoders)
            results.append(result)
        except Exception as e:
            # Don't fail the whole batch for one bad record
            results.append({
                "worker_id": worker.get("worker_id", "unknown"),
                "error"    : str(e)
            })

    return jsonify({"results": results, "count": len(results)})


@app.route("/api/premium/tiers", methods=["GET"])
def get_tiers():
    """
    Returns all coverage tiers.
    Mobile app calls this to show plan options to the worker.
    """
    return jsonify(TIERS)


@app.route("/api/premium/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status"      : "ok" if _model else "model_not_loaded",
        "model_loaded": _model is not None,
        "port"        : 5003,
    })


if __name__ == "__main__":
    log.info("Premium Calculator API running on http://localhost:5003")
    app.run(host="0.0.0.0", port=5003, debug=False)