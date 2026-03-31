import React, { useEffect, useState, useRef } from "react";
import "./Claims.css";
import API from "../services/api";
import { useNavigate, useLocation } from "react-router-dom";

const TRIGGER_EMOJI = {
    rainfall:          "🌧️",
    aqi:               "😷",
    heat:              "🌡️",
    platform_downtime: "📵",
    curfew:            "🚫",
};

const STAGE_META = {
    queued:          { label: "Queued",              icon: "⏳", color: "#818cf8" },
    duplicate_check: { label: "Duplicate Check",     icon: "🔍", color: "#60a5fa" },
    policy_check:    { label: "Policy Verification", icon: "📋", color: "#a78bfa" },
    ml_calculation:  { label: "ML Payout Model",     icon: "🤖", color: "#34d399" },
    coverage_cap:    { label: "Coverage Cap Check",  icon: "🛡️", color: "#fbbf24" },
    fraud_check:     { label: "Fraud Detection",     icon: "🔒", color: "#f87171" },
    approved:        { label: "Approved",            icon: "✅", color: "#22c55e" },
    flagged:         { label: "Flagged for Review",  icon: "⚠️", color: "#f59e0b" },
    failed:          { label: "Rejected",            icon: "❌", color: "#ef4444" },
};

const FILTERS = ["all", "approved", "flagged", "rejected", "pending"];

// ── PROCESSING STEPPER COMPONENT ─────────────────────────────
function ClaimProcessingStepper({ claimId, eventId, onComplete }) {
    const [logs,    setLogs]    = useState([]);
    const [done,    setDone]    = useState(false);
    const pollRef               = useRef(null);

    useEffect(() => {
        if (claimId) {
            startPollingLogs(claimId);
        } else if (eventId) {
            pollRef.current = setInterval(async () => {
                try {
                    const res = await API.get(`/claims/event/${encodeURIComponent(eventId)}`);
                    if (res.data?.claimId) {
                        clearInterval(pollRef.current);
                        startPollingLogs(res.data.claimId);
                        onComplete && onComplete(res.data.claimId);
                    }
                } catch {}
            }, 3000);
        }
        return () => clearInterval(pollRef.current);
    }, [claimId, eventId]);

    const startPollingLogs = (cid) => {
        const poll = async () => {
            try {
                const res = await API.get(`/claims/logs/${cid}`);
                setLogs(res.data || []);
                const isFinished = res.data?.some(l =>
                    (l.stage === "approved" || l.stage === "flagged" || l.stage === "duplicate_check")
                    && l.status === "done"
                );
                if (isFinished) {
                    setDone(true);
                    clearInterval(pollRef.current);
                    onComplete && onComplete(cid);
                }
            } catch {}
        };
        poll();
        pollRef.current = setInterval(poll, 2000);
    };

    // Group logs by stage — keep latest entry per stage
    const stageMap = {};
    logs.forEach(l => { stageMap[l.stage] = l; });
    const stages = Object.values(stageMap);

    return (
        <div className="processing-stepper">
            {stages.length === 0 ? (
                <div className="stepper-waiting">
                    <span className="spinner-sm" />
                    <span>Waiting for claim processor... (up to 30s)</span>
                </div>
            ) : (
                stages.map((log, i) => {
                    const meta   = STAGE_META[log.stage] || { label: log.stage, icon: "⚙️", color: "#9090b0" };
                    const isLast = i === stages.length - 1;
                    return (
                        <div key={log.stage} className="stepper-step">
                            <div className="stepper-left">
                                <div
                                    className={`stepper-icon ${log.status === "processing" ? "pulsing" : ""}`}
                                    style={{ background: log.status === "done" ? meta.color : log.status === "failed" ? "#ef4444" : "#2a2a3a",
                                             color: log.status === "processing" ? "#9090b0" : "#fff" }}
                                >
                                    {log.status === "processing" ? <span className="spinner-sm" /> : meta.icon}
                                </div>
                                {!isLast && <div className="stepper-line" style={{ background: log.status === "done" ? meta.color : "#2a2a3a" }} />}
                            </div>
                            <div className="stepper-content">
                                <div className="stepper-stage-name" style={{ color: meta.color }}>
                                    {meta.label}
                                    {log.status === "processing" && <span className="stepper-processing-badge">Processing...</span>}
                                    {log.status === "done"       && <span className="stepper-done-badge">Done</span>}
                                    {log.status === "failed"     && <span className="stepper-failed-badge">Failed</span>}
                                </div>
                                <p className="stepper-detail">{log.detail}</p>
                            </div>
                        </div>
                    );
                })
            )}
        </div>
    );
}

// ── MAIN CLAIMS PAGE ──────────────────────────────────────────
function Claims() {
    const navigate  = useNavigate();
    const location  = useLocation();
    const workerId  = localStorage.getItem("workerId");

    // Read eventId from URL query param: /claims?eventId=xxx
    const urlParams  = new URLSearchParams(location.search);
    const urlEventId = urlParams.get("eventId");

    const [claims,       setClaims]       = useState([]);
    const [analytics,    setAnalytics]    = useState(null);
    const [filter,       setFilter]       = useState(urlEventId ? "pending" : "all");
    const [expanded,     setExpanded]     = useState(null);
    const [expandedData, setExpandedData] = useState({});  // claimId → full ClaimResponse
    const [loading,      setLoading]      = useState(true);
    const [pendingItems, setPendingItems] = useState(
        urlEventId ? [{ eventId: urlEventId, claimId: null }] : []
    );

    useEffect(() => {
        fetchAll();
        fetchActiveTriggerEvents();
    }, []);

    // Fetch active trigger events from Spring Boot for this worker's zone
    // These are disruptions that are ongoing or resolved but not yet processed
    const fetchActiveTriggerEvents = async () => {
        const zoneId = localStorage.getItem("zoneId");
        if (!zoneId) return;
        try {
            // Get active trigger events in the worker's zone
            const res = await API.get(`/trigger-events/active/${zoneId}`);
            const activeEvents = res.data || [];
            if (activeEvents.length === 0) return;

            // For each active event, check if a claim already exists
            // If not, add to pendingItems
            const newPending = [];
            for (const ev of activeEvents) {
                try {
                    const claimRes = await API.get(`/claims/event/${encodeURIComponent(ev.eventId)}`);
                    if (claimRes.data?.status === "queued") {
                        // No claim yet — show as pending
                        newPending.push({ eventId: ev.eventId, claimId: null });
                    } else if (claimRes.data?.claimId) {
                        // Claim exists but may still be processing
                        const claim = claimRes.data;
                        if (claim.status === "processing") {
                            newPending.push({ eventId: ev.eventId, claimId: claim.claimId });
                        }
                    }
                } catch {}
            }

            if (newPending.length > 0) {
                setPendingItems(prev => {
                    const existingIds = new Set(prev.map(p => p.eventId));
                    const toAdd = newPending.filter(p => !existingIds.has(p.eventId));
                    const merged = [...prev, ...toAdd];
                    if (merged.length > 0) setFilter("pending");
                    return merged;
                });
            }
        } catch {}
    };

    const fetchAll = async () => {
        setLoading(true);
        try {
            const [claimsRes, analyticsRes] = await Promise.all([
                API.get(`/claims/worker/${workerId}`),
                API.get(`/claims/analytics?workerId=${workerId}`),
            ]);
            setClaims(claimsRes.data);
            setAnalytics(analyticsRes.data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleClaimReady = (eventId, claimId) => {
        setPendingItems(prev =>
            prev.map(p => p.eventId === eventId ? { ...p, claimId } : p)
        );
        setTimeout(fetchAll, 2000);
    };

    const handleProcessingComplete = (eventId) => {
        setTimeout(() => {
            setPendingItems(prev => prev.filter(p => p.eventId !== eventId));
            fetchAll();
        }, 3000);
    };

    const handleExpand = async (claimId) => {
        if (expanded === claimId) { setExpanded(null); return; }
        setExpanded(claimId);
        if (!expandedData[claimId]) {
            try {
                const res = await API.get(`/claims/detail/${claimId}`);
                setExpandedData(prev => ({ ...prev, [claimId]: res.data }));
            } catch {}
        }
    };

    const filtered = filter === "all"
        ? claims
        : filter === "pending"
        ? claims.filter(c => c.status === "pending" || c.status === "processing")
        : claims.filter(c => c.status === filter);

    const formatDate = (iso) => iso
        ? new Date(iso).toLocaleString("en-IN", {
            day: "numeric", month: "short",
            hour: "2-digit", minute: "2-digit"
          })
        : "—";

    const fraudColor = (score) => {
        const s = Number(score);
        if (s > 0.7) return "#ef4444";
        if (s > 0.4) return "#f59e0b";
        return "#22c55e";
    };

    if (loading) {
        return (
            <div className="claims-page">
                <nav className="page-nav">
                    <span className="page-nav-title">📋 Claims</span>
                    <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
                </nav>
                <div className="claims-body">
                    <div className="analytics-row">
                        {[1,2,3,4].map(i => <div key={i} className="skeleton" style={{ height: 72 }} />)}
                    </div>
                    {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 68, marginBottom: 10 }} />)}
                </div>
            </div>
        );
    }

    return (
        <div className="claims-page">
            <nav className="page-nav">
                <span className="page-nav-title">📋 Claims</span>
                <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
            </nav>

            <div className="claims-body">

                {/* ANALYTICS */}
                {analytics && (
                    <div className="analytics-row">
                        <div className="analytics-card">
                            <span className="analytics-label">Total</span>
                            <span className="analytics-value purple">{analytics.totalClaims}</span>
                        </div>
                        <div className="analytics-card">
                            <span className="analytics-label">Approved</span>
                            <span className="analytics-value green">{analytics.approvedClaims}</span>
                        </div>
                        <div className="analytics-card">
                            <span className="analytics-label">Flagged</span>
                            <span className="analytics-value yellow">{analytics.flaggedClaims}</span>
                        </div>
                        <div className="analytics-card">
                            <span className="analytics-label">Total Paid</span>
                            <span className="analytics-value green">
                                ₹{Number(analytics.totalPaidOut || 0).toFixed(0)}
                            </span>
                        </div>
                    </div>
                )}

                {/* FILTER TABS */}
                <div className="filter-tabs">
                    {FILTERS.map(f => (
                        <button
                            key={f}
                            className={`filter-tab ${filter === f ? "active" : ""}`}
                            onClick={() => setFilter(f)}
                        >
                            {f.charAt(0).toUpperCase() + f.slice(1)}
                            {f === "pending" && pendingItems.length > 0 && (
                                <span style={{ marginLeft: 5, color: "#818cf8" }}>
                                    ({pendingItems.length})
                                </span>
                            )}
                            {f !== "all" && f !== "pending" && (
                                <span style={{ marginLeft: 5, opacity: 0.6 }}>
                                    ({claims.filter(c => c.status === f).length})
                                </span>
                            )}
                        </button>
                    ))}
                </div>

                {/* PENDING PROCESSING CARDS */}
                {(filter === "all" || filter === "pending") && pendingItems.map(item => (
                    <div key={item.eventId} className="claim-card processing-card">
                        <div className="claim-card-header">
                            <div className="claim-header-left">
                                <div className="claim-icon">⚙️</div>
                                <div>
                                    <p className="claim-title">Claim Processing</p>
                                    <p className="claim-subtitle">
                                        Event: {item.eventId?.split(":")[1]?.replace(/_/g," ")} •{" "}
                                        {item.claimId || "Waiting for processor..."}
                                    </p>
                                </div>
                            </div>
                            <div className="claim-header-right">
                                <span className="status-badge status-processing">Processing</span>
                            </div>
                        </div>

                        <ClaimProcessingStepper
                            claimId={item.claimId}
                            eventId={item.eventId}
                            onComplete={(cid) => {
                                if (!item.claimId) handleClaimReady(item.eventId, cid);
                                else handleProcessingComplete(item.eventId);
                            }}
                        />
                    </div>
                ))}

                {/* REGULAR CLAIMS LIST */}
                {filtered.length === 0 && pendingItems.length === 0 ? (
                    <div className="empty-state">
                        <span style={{ fontSize: 36 }}>📭</span>
                        <p>No {filter === "all" ? "" : filter} claims yet.</p>
                        <p style={{ marginTop: 6, fontSize: 12 }}>
                            Claims are created automatically when a disruption trigger fires in your zone.
                        </p>
                    </div>
                ) : (
                    <div className="claims-list">
                        {filtered.map((c) => {
                            const detail = expandedData[c.claimId];
                            return (
                            <div
                                key={c.claimId}
                                className={`claim-card ${expanded === c.claimId ? "expanded" : ""}`}
                                onClick={() => handleExpand(c.claimId)}
                            >
                                <div className="claim-card-header">
                                    <div className="claim-header-left">
                                        <div className="claim-icon">
                                            {TRIGGER_EMOJI[c.triggerType] || "⚡"}
                                        </div>
                                        <div>
                                            <p className="claim-title">
                                                {c.triggerType?.replace(/_/g, " ")}
                                            </p>
                                            <p className="claim-subtitle">{formatDate(c.triggeredAt)}</p>
                                        </div>
                                    </div>
                                    <div className="claim-header-right">
                                        <span className="claim-payout">
                                            ₹{Number(c.payoutAmount).toFixed(2)}
                                        </span>
                                        <span className={`status-badge status-${c.status}`}>
                                            {c.status}
                                        </span>
                                    </div>
                                </div>

                                {expanded === c.claimId && (
                                    <div className="claim-detail">
                                        {!detail ? (
                                            <div style={{ gridColumn: "1/-1", display: "flex", alignItems: "center", gap: 8, color: "#5a5a72", fontSize: 13 }}>
                                                <span className="spinner-sm" /> Loading details...
                                            </div>
                                        ) : (
                                            <>
                                                <div className="detail-item">
                                                    <span className="detail-label">Claim ID</span>
                                                    <span className="detail-value">{detail.claimId}</span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Policy</span>
                                                    <span className="detail-value">{detail.policyNumber || "—"}</span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Zone</span>
                                                    <span className="detail-value">
                                                        {detail.zoneId?.replace("zone_","").replace(/_/g," ") || "—"}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Disrupted Hours</span>
                                                    <span className="detail-value">
                                                        {detail.disruptedHours ? `${detail.disruptedHours} hrs` : "—"}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Trigger Value</span>
                                                    <span className="detail-value">{detail.triggerValue ?? "—"}</span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Processed At</span>
                                                    <span className="detail-value">{formatDate(detail.processedAt)}</span>
                                                </div>
                                                <div className="fraud-bar-wrap">
                                                    <span className="detail-label">
                                                        Fraud Score — {(Number(detail.fraudScore) * 100).toFixed(0)}%
                                                    </span>
                                                    <div className="fraud-bar">
                                                        <div
                                                            className="fraud-bar-fill"
                                                            style={{
                                                                width: `${Number(detail.fraudScore) * 100}%`,
                                                                background: fraudColor(detail.fraudScore),
                                                            }}
                                                        />
                                                    </div>
                                                </div>
                                                <div className="logs-section">
                                                    <span className="detail-label" style={{ marginBottom: 10, display: "block" }}>
                                                        Processing Pipeline
                                                    </span>
                                                    <ClaimProcessingStepper claimId={detail.claimId} />
                                                </div>
                                            </>
                                        )}
                                    </div>
                                )}
                            </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}

export default Claims;
