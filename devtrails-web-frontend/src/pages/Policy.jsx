import React, { useEffect, useState } from "react";
import "./Policy.css";
import API from "../services/api";
import { useNavigate } from "react-router-dom";

const TIERS = [
    {
        key: "basic",
        price: 49,
        cap: 2000,
        features: ["Up to ₹2,000/week", "All 5 disruption types", "Auto payout", "No claim filing"],
    },
    {
        key: "standard",
        price: 89,
        cap: 4500,
        popular: true,
        features: ["Up to ₹4,500/week", "All 5 disruption types", "Auto payout", "Priority processing"],
    },
    {
        key: "premium",
        price: 149,
        cap: 8000,
        features: ["Up to ₹8,000/week", "All 5 disruption types", "Auto payout", "Highest coverage cap"],
    },
];

function getCurrentSeason() {
    const month = new Date().getMonth() + 1; // 1–12
    if (month >= 6 && month <= 9)  return "monsoon"; // Jun–Sep
    if (month >= 11 || month <= 1) return "winter";  // Nov–Jan
    if (month >= 2 && month <= 3)  return "spring";  // Feb–Mar
    return "summer";                                  // Apr–May, Oct
}

function Policy() {
    const navigate  = useNavigate();
    const workerId  = localStorage.getItem("workerId");

    const [policy,    setPolicy]    = useState(null);
    const [history,   setHistory]   = useState([]);
    const [tier,      setTier]      = useState("standard");
    const season = getCurrentSeason();
    const [loading,   setLoading]   = useState(true);
    const [submitting,setSubmitting]= useState(false);
    const [error,     setError]     = useState("");
    const [success,   setSuccess]   = useState("");

    useEffect(() => { fetchPolicy(); }, []);

    const fetchPolicy = async () => {
        setLoading(true);
        try {
            const res = await API.get(`/policies/${workerId}/current`);
            setPolicy(res.data);
        } catch {
            setPolicy(null);
        }
        try {
            const res = await API.get(`/policies/${workerId}/history`);
            setHistory(res.data);
        } catch {
            setHistory([]);
        }
        setLoading(false);
    };

    const createPolicy = async () => {
        setError("");
        setSuccess("");
        setSubmitting(true);
        try {
            const res = await API.post("/policies", { workerId, tier, season });
            setPolicy(res.data);
            setSuccess(`✅ ${tier.charAt(0).toUpperCase() + tier.slice(1)} policy activated! Coverage starts now.`);
            fetchPolicy();
        } catch (err) {
            setError(err?.response?.data?.message || "Policy creation failed. Please try again.");
        } finally {
            setSubmitting(false);
        }
    };

    const coveragePct = policy
        ? Math.min(100, (Number(policy.coverageUsed) / Number(policy.coverageCap)) * 100)
        : 0;

    if (loading) {
        return (
            <div className="policy-page">
                <nav className="page-nav">
                    <span className="page-nav-title">📜 Policy</span>
                    <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
                </nav>
                <div className="policy-body">
                    <div style={{ background: "#16161f", borderRadius: 14, height: 200,
                        animation: "shimmer 1.4s infinite" }} />
                </div>
            </div>
        );
    }

    return (
        <div className="policy-page">

            <nav className="page-nav">
                <span className="page-nav-title">📜 Policy</span>
                <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
            </nav>

            <div className="policy-body">

                {/* ── ACTIVE POLICY ── */}
                {policy && (
                    <div className="active-policy-card">
                        <div className="active-policy-header">
                            <h3>Active Policy</h3>
                            <span className={`policy-tier-badge tier-${policy.tier}`}>
                                {policy.tier}
                            </span>
                        </div>

                        <div className="policy-details-grid">
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Policy Number</span>
                                <span className="policy-detail-value">{policy.policyNumber}</span>
                            </div>
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Weekly Premium</span>
                                <span className="policy-detail-value">₹{policy.weeklyPremium}</span>
                            </div>
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Valid From</span>
                                <span className="policy-detail-value">{policy.weekStart}</span>
                            </div>
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Valid Till</span>
                                <span className="policy-detail-value">{policy.weekEnd}</span>
                            </div>
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Season</span>
                                <span className="policy-detail-value" style={{ textTransform: "capitalize" }}>
                                    {policy.season}
                                </span>
                            </div>
                            <div className="policy-detail-item">
                                <span className="policy-detail-label">Coverage Remaining</span>
                                <span className="policy-detail-value" style={{ color: "#22c55e" }}>
                                    ₹{Number(policy.coverageRemaining).toFixed(2)}
                                </span>
                            </div>
                        </div>

                        <div className="coverage-bar-wrap">
                            <div className="coverage-bar-label">
                                <span>Coverage used</span>
                                <span>₹{Number(policy.coverageUsed).toFixed(2)} of ₹{policy.coverageCap}</span>
                            </div>
                            <div className="coverage-bar">
                                <div
                                    className={`coverage-bar-fill ${coveragePct > 80 ? "danger" : ""}`}
                                    style={{ width: `${coveragePct}%` }}
                                />
                            </div>
                        </div>
                    </div>
                )}

                {/* ── SUBSCRIBE SECTION (only if no active policy) ── */}
                {!policy && (
                    <>
                        <p className="tier-section-title">Choose Your Plan</p>

                        {/* Tier cards */}
                        <div className="tier-cards">
                            {TIERS.map((t) => (
                                <div
                                    key={t.key}
                                    className={`tier-card ${tier === t.key ? "selected" : ""} ${t.popular ? "popular" : ""}`}
                                    onClick={() => setTier(t.key)}
                                >
                                    <p className="tier-name">{t.key}</p>
                                    <p className="tier-price">
                                        ₹{t.price}<span>/week</span>
                                    </p>
                                    <p className="tier-cap">Up to <b>₹{t.cap.toLocaleString("en-IN")}</b> coverage</p>
                                    <ul className="tier-features">
                                        {t.features.map((f, i) => <li key={i}>{f}</li>)}
                                    </ul>
                                </div>
                            ))}
                        </div>

                        {error   && <div className="error-msg">⚠️ {error}</div>}
                        {success && <div className="success-msg">{success}</div>}

                        <button className="btn-subscribe" onClick={createPolicy} disabled={submitting}>
                            {submitting ? (
                                <><span className="spinner" /> Activating Policy...</>
                            ) : (
                                `Activate ${tier.charAt(0).toUpperCase() + tier.slice(1)} Plan — ₹${TIERS.find(t => t.key === tier)?.price}/week`
                            )}
                        </button>
                    </>
                )}

                {/* ── POLICY HISTORY ── */}
                {history.length > 0 && (
                    <div className="history-card">
                        <p className="history-title">Policy History</p>
                        {history.map((p) => (
                            <div className="history-item" key={p.policyNumber}>
                                <div className="history-left">
                                    <span className="history-tier">{p.tier} plan</span>
                                    <span className="history-dates">{p.weekStart} → {p.weekEnd}</span>
                                </div>
                                <div className="history-right">
                                    <span className="history-premium">₹{p.weeklyPremium}/week</span>
                                    <span className={`status-badge status-${p.status}`}>{p.status}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

            </div>
        </div>
    );
}

export default Policy;
