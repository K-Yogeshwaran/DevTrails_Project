import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { io } from "socket.io-client";
import "./AdminDashboard.css";
import AdminAnalytics from "./AdminAnalytics";

const TRIGGER_TYPES = [
    { key: "rainfall",          label: "Heavy Rainfall",      emoji: "🌧️", value: 45  },
    { key: "aqi",               label: "Hazardous AQI",       emoji: "😷", value: 250 },
    { key: "heat",              label: "Extreme Heat",        emoji: "🌡️", value: 44  },
    { key: "platform_downtime", label: "Platform Downtime",   emoji: "📵", value: 60  },
    { key: "curfew",            label: "Curfew / Section 144",emoji: "🚫", value: 1   },
];

const TRIGGER_EMOJI = { rainfall:"🌧️", aqi:"😷", heat:"🌡️", platform_downtime:"📵", curfew:"🚫" };

function AdminDashboard() {
    const navigate   = useNavigate();
    const adminUser  = localStorage.getItem("adminUsername") || "Admin";
    const socketRef  = useRef(null);

    const [stats,         setStats]         = useState(null);
    const [workerStats,   setWorkerStats]   = useState(null);
    const [policyStats,   setPolicyStats]   = useState(null);
    const [flaggedClaims, setFlaggedClaims] = useState([]);
    const [allZones,      setAllZones]      = useState({});
    const [activeZones,   setActiveZones]   = useState({});
    const [liveFeed,      setLiveFeed]      = useState([]);
    const [services,      setServices]      = useState({ backend: null, trigger: null, ml: null });
    const [loading,       setLoading]       = useState(true);

    // Trigger modal state
    const [showTriggerModal, setShowTriggerModal] = useState(false);
    const [triggerZone,      setTriggerZone]      = useState("");
    const [triggerType,      setTriggerType]      = useState("rainfall");
    const [triggerLoading,   setTriggerLoading]   = useState(false);
    const [triggerMsg,       setTriggerMsg]        = useState("");

    // Active tab
    const [tab, setTab] = useState("overview");

    useEffect(() => {
        fetchAll();
        checkServices();
        connectSocket();
        const interval = setInterval(() => { fetchAll(); checkServices(); }, 30000);
        return () => {
            clearInterval(interval);
            socketRef.current?.disconnect();
        };
    }, []);

    const api = async (path) => {
        const res = await fetch(`${import.meta.env.BACKEND_API_URL}${path}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem("adminToken")}` }
        });
        if (!res.ok) throw new Error(`${res.status}`);
        return res.json();
    };

    const fetchAll = async () => {
        try {
            const [s, w, p, f, z, az] = await Promise.all([
                api("/api/admin/stats"),
                api("/api/admin/workers"),
                api("/api/admin/policies"),
                api("/api/admin/claims/flagged"),
                fetch(`${import.meta.env.TRIGGER_API_URL}/zones`).then(r => r.json()),
                fetch(`${import.meta.env.TRIGGER_API_URL}/zones/active`).then(r => r.json()).catch(() => ({})),
            ]);
            setStats(s); setWorkerStats(w); setPolicyStats(p);
            setFlaggedClaims(f); setAllZones(z); setActiveZones(az);
        } catch (e) {
            console.error("Admin fetch failed:", e);
        } finally {
            setLoading(false);
        }
    };

    const checkServices = async () => {
        const check = async (url) => {
            try {
                const r = await fetch(url, { signal: AbortSignal.timeout(3000) });
                return r.ok ? "ok" : "error";
            } catch { return "down"; }
        };
        const [backend, trigger, ml] = await Promise.all([
            check(`${import.meta.env.BACKEND_API_URL}/workers/health`),
            check(`${import.meta.env.TRIGGER_API_URL}/health`),
            check(`${import.meta.env.PREMIUM_API_URL}/payout/health`),
        ]);
        setServices({ backend, trigger, ml });
    };

    const connectSocket = () => {
        const socket = io(import.meta.env.SOCKET_URL, { transports: ["polling"] });
        socketRef.current = socket;
        socket.on("trigger_fired", (event) => {
            setLiveFeed(prev => [{ ...event, receivedAt: new Date().toLocaleTimeString() }, ...prev].slice(0, 50));
        });
        socket.on("trigger_resolved", (event) => {
            setLiveFeed(prev => [{ ...event, status: "resolved", receivedAt: new Date().toLocaleTimeString() }, ...prev].slice(0, 50));
        });
    };

    const fireTrigger = async () => {
        if (!triggerZone) { setTriggerMsg("❌ Select a zone first"); return; }
        setTriggerLoading(true); setTriggerMsg("");
        const chosen = TRIGGER_TYPES.find(t => t.key === triggerType);
        try {
            const res = await fetch(`${import.meta.env.TRIGGER_API_URL}/triggers/manual`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ zone_id: triggerZone, trigger_type: triggerType, value: chosen?.value ?? 45 }),
            });
            const data = await res.json();
            if (res.ok) {
                setTriggerMsg(`✅ ${chosen?.emoji} ${chosen?.label} fired in ${allZones[triggerZone]?.name || triggerZone}`);
                setTimeout(() => { setShowTriggerModal(false); setTriggerMsg(""); }, 2000);
            } else {
                setTriggerMsg(`❌ ${data.error || "Failed"}`);
            }
        } catch { setTriggerMsg("❌ Trigger engine unreachable"); }
        finally { setTriggerLoading(false); }
    };

    const handleClaim = async (claimId, action) => {
        try {
            await fetch(`${import.meta.env.BACKEND_API_URL}/admin/claims/${claimId}/${action}`, {
                method: "POST",
                headers: { Authorization: `Bearer ${localStorage.getItem("adminToken")}` },
            });
            setFlaggedClaims(prev => prev.filter(c => c.claimId !== claimId));
        } catch (e) { alert("Action failed: " + e.message); }
    };

    const logout = () => {
        localStorage.removeItem("adminToken");
        localStorage.removeItem("adminUsername");
        navigate("/admin/login");
    };

    const svcColor = (s) => s === "ok" ? "#22c55e" : s === "down" ? "#ef4444" : "#f59e0b";
    const svcLabel = (s) => s === "ok" ? "Online" : s === "down" ? "Down" : s === null ? "Checking..." : "Error";

    const formatDate = (iso) => iso ? new Date(iso).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }) : "—";

    if (loading) return (
        <div className="admin-page">
            <div className="admin-loading">
                <span className="admin-spinner" />
                <p>Loading admin dashboard...</p>
            </div>
        </div>
    );

    const activeZoneIds = Object.keys(activeZones);

    return (
        <div className="admin-page">

            {/* NAVBAR */}
            <nav className="admin-nav">
                <div className="admin-nav-brand">🛡️ GigShield <span>Admin</span></div>
                <div className="admin-nav-tabs">
                    {["overview", "analytics", "zones", "claims", "workers", "feed"].map(t => (
                        <button key={t} className={`admin-tab ${tab === t ? "active" : ""}`} onClick={() => setTab(t)}>
                            {t.charAt(0).toUpperCase() + t.slice(1)}
                        </button>
                    ))}
                </div>
                <div className="admin-nav-right">
                    <span className="admin-user">👤 {adminUser}</span>
                    <button className="admin-logout" onClick={logout}>Logout</button>
                </div>
            </nav>

            {/* SERVICE HEALTH BAR */}
            <div className="admin-health-bar">
                {[
                    { label: "Spring Boot", key: "backend" },
                    { label: "Trigger Engine", key: "trigger" },
                    { label: "ML Payout API", key: "ml" },
                ].map(s => (
                    <div key={s.key} className="health-item">
                        <span className="health-dot" style={{ background: svcColor(services[s.key]) }} />
                        <span className="health-label">{s.label}</span>
                        <span className="health-status" style={{ color: svcColor(services[s.key]) }}>
                            {svcLabel(services[s.key])}
                        </span>
                    </div>
                ))}
                <div className="health-item">
                    <span className="health-dot" style={{ background: "#22c55e" }} />
                    <span className="health-label">Active Zones</span>
                    <span className="health-status" style={{ color: "#22c55e" }}>{activeZoneIds.length}</span>
                </div>
                <button className="admin-fire-btn" onClick={() => setShowTriggerModal(true)}>
                    ⚡ Fire Trigger
                </button>
            </div>

            <div className="admin-body">

                {/* ── OVERVIEW TAB ── */}
                {tab === "overview" && (
                    <>
                        <div className="admin-stats-grid">
                            {[
                                { label: "Total Workers",    value: stats?.totalWorkers,   color: "#818cf8" },
                                { label: "Active Workers",   value: stats?.activeWorkers,  color: "#22c55e" },
                                { label: "Active Policies",  value: stats?.activePolicies, color: "#a78bfa" },
                                { label: "Active Triggers",  value: stats?.activeTriggers, color: "#f87171" },
                                { label: "Total Claims",     value: stats?.totalClaims,    color: "#60a5fa" },
                                { label: "Claims Today",     value: stats?.claimsToday,    color: "#fbbf24" },
                                { label: "Flagged Claims",   value: stats?.flaggedClaims,  color: "#f59e0b" },
                                { label: "Total Paid Out",   value: `₹${Number(stats?.totalPaidOut || 0).toFixed(0)}`, color: "#22c55e" },
                            ].map(s => (
                                <div key={s.label} className="admin-stat-card">
                                    <span className="admin-stat-label">{s.label}</span>
                                    <span className="admin-stat-value" style={{ color: s.color }}>{s.value ?? "—"}</span>
                                </div>
                            ))}
                        </div>

                        {/* Policy breakdown */}
                        <div className="admin-section">
                            <h3 className="admin-section-title">📋 Policy Breakdown</h3>
                            <div className="admin-breakdown-row">
                                {["basic", "standard", "premium"].map(tier => (
                                    <div key={tier} className="admin-breakdown-card">
                                        <span className="breakdown-tier">{tier}</span>
                                        <span className="breakdown-count">{policyStats?.byTier?.[tier] || 0}</span>
                                        <span className="breakdown-label">active policies</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Worker persona breakdown */}
                        <div className="admin-section">
                            <h3 className="admin-section-title">👥 Workers by Persona</h3>
                            <div className="admin-breakdown-row">
                                {[
                                    { key: "food",      emoji: "🍔", label: "Food Delivery" },
                                    { key: "grocery",   emoji: "🛒", label: "Grocery" },
                                    { key: "ecommerce", emoji: "📦", label: "E-Commerce" },
                                ].map(p => (
                                    <div key={p.key} className="admin-breakdown-card">
                                        <span style={{ fontSize: 24 }}>{p.emoji}</span>
                                        <span className="breakdown-count">{workerStats?.byPersona?.[p.key] || 0}</span>
                                        <span className="breakdown-label">{p.label}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </>
                )}

                {/* ── ZONES TAB ── */}
                {tab === "zones" && (
                    <div className="admin-section">
                        <div className="admin-section-header">
                            <h3 className="admin-section-title">🗺️ Active Zones ({activeZoneIds.length})</h3>
                            <button className="admin-fire-btn" onClick={() => setShowTriggerModal(true)}>⚡ Fire Trigger</button>
                        </div>

                        {activeZoneIds.length === 0 ? (
                            <div className="admin-empty">No active zones. Workers need to go online first.</div>
                        ) : (
                            <table className="admin-table">
                                <thead>
                                    <tr>
                                        <th>Zone</th><th>City</th><th>Workers Online</th><th>Active Disruptions</th><th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activeZoneIds.map(zid => {
                                        const zone = allZones[zid] || {};
                                        const workerCount = workerStats?.byZone?.[zid] || 0;
                                        const disruptions = stats?.activeTriggers || 0;
                                        return (
                                            <tr key={zid}>
                                                <td><span className="zone-name">{zone.name || zid}</span></td>
                                                <td>{zone.city || "—"}</td>
                                                <td><span className="worker-count">{workerCount}</span></td>
                                                <td>{disruptions > 0 ? <span className="disruption-badge">{disruptions} active</span> : <span style={{ color: "#4a4a62" }}>None</span>}</td>
                                                <td>
                                                    <button className="admin-zone-trigger-btn" onClick={() => { setTriggerZone(zid); setShowTriggerModal(true); }}>
                                                        ⚡ Fire
                                                    </button>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        )}

                        <h3 className="admin-section-title" style={{ marginTop: 28 }}>🌍 All Zones ({Object.keys(allZones).length})</h3>
                        <div className="admin-all-zones">
                            {Object.entries(allZones).map(([zid, zone]) => {
                                const isActive = activeZoneIds.includes(zid);
                                return (
                                    <div key={zid} className={`admin-zone-chip ${isActive ? "active" : ""}`}>
                                        <span className="zone-chip-dot" style={{ background: isActive ? "#22c55e" : "#2a2a3a" }} />
                                        <span>{zone.name}</span>
                                        <span className="zone-chip-city">{zone.city}</span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* ── CLAIMS TAB ── */}
                {tab === "claims" && (
                    <div className="admin-section">
                        <h3 className="admin-section-title">⚠️ Flagged Claims — Pending Review ({flaggedClaims.length})</h3>
                        {flaggedClaims.length === 0 ? (
                            <div className="admin-empty">✅ No flagged claims. All clear.</div>
                        ) : (
                            <table className="admin-table">
                                <thead>
                                    <tr>
                                        <th>Claim ID</th><th>Worker</th><th>Trigger</th><th>Zone</th>
                                        <th>Payout</th><th>Fraud Score</th><th>Date</th><th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {flaggedClaims.map(c => (
                                        <tr key={c.claimId}>
                                            <td><span style={{ fontSize: 11, color: "#7a7a9a" }}>{c.claimId}</span></td>
                                            <td>
                                                <div style={{ fontSize: 13, color: "#e0e0f0" }}>{c.workerName}</div>
                                                <div style={{ fontSize: 11, color: "#5a5a72" }}>{c.workerId}</div>
                                            </td>
                                            <td>{TRIGGER_EMOJI[c.triggerType] || "⚡"} {c.triggerType?.replace(/_/g," ")}</td>
                                            <td style={{ fontSize: 12 }}>{c.zoneId?.replace("zone_","").replace(/_/g," ")}</td>
                                            <td style={{ color: "#22c55e", fontWeight: 600 }}>₹{Number(c.payoutAmount).toFixed(2)}</td>
                                            <td>
                                                <div className="fraud-mini-bar">
                                                    <div style={{ width: `${Number(c.fraudScore)*100}%`, background: Number(c.fraudScore) > 0.7 ? "#ef4444" : "#f59e0b", height: "100%", borderRadius: 4 }} />
                                                </div>
                                                <span style={{ fontSize: 11, color: "#f87171" }}>{(Number(c.fraudScore)*100).toFixed(0)}%</span>
                                            </td>
                                            <td style={{ fontSize: 12 }}>{formatDate(c.triggeredAt)}</td>
                                            <td>
                                                <div style={{ display: "flex", gap: 6 }}>
                                                    <button className="admin-approve-btn" onClick={() => handleClaim(c.claimId, "approve")}>✅ Approve</button>
                                                    <button className="admin-reject-btn"  onClick={() => handleClaim(c.claimId, "reject")}>❌ Reject</button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {/* ── WORKERS TAB ── */}
                {tab === "workers" && (
                    <div className="admin-section">
                        <h3 className="admin-section-title">👥 Worker Statistics</h3>
                        <div className="admin-stats-grid" style={{ gridTemplateColumns: "repeat(3,1fr)" }}>
                            <div className="admin-stat-card">
                                <span className="admin-stat-label">Total Registered</span>
                                <span className="admin-stat-value" style={{ color: "#818cf8" }}>{workerStats?.total}</span>
                            </div>
                            <div className="admin-stat-card">
                                <span className="admin-stat-label">Active Accounts</span>
                                <span className="admin-stat-value" style={{ color: "#22c55e" }}>{workerStats?.active}</span>
                            </div>
                            <div className="admin-stat-card">
                                <span className="admin-stat-label">Inactive Accounts</span>
                                <span className="admin-stat-value" style={{ color: "#f87171" }}>{workerStats?.inactive}</span>
                            </div>
                        </div>

                        <h3 className="admin-section-title" style={{ marginTop: 24 }}>Workers by Zone</h3>
                        <table className="admin-table">
                            <thead><tr><th>Zone ID</th><th>Worker Count</th></tr></thead>
                            <tbody>
                                {Object.entries(workerStats?.byZone || {})
                                    .sort((a,b) => b[1]-a[1])
                                    .map(([zid, count]) => (
                                    <tr key={zid}>
                                        <td>{zid.replace("zone_","").replace(/_/g," ")}</td>
                                        <td><span className="worker-count">{count}</span></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* ── LIVE FEED TAB ── */}
                {tab === "feed" && (
                    <div className="admin-section">
                        <div className="admin-section-header">
                            <h3 className="admin-section-title">📡 Live Trigger Feed</h3>
                            <span style={{ fontSize: 12, color: "#4a4a62" }}>{liveFeed.length} events received this session</span>
                        </div>
                        {liveFeed.length === 0 ? (
                            <div className="admin-empty">
                                <span className="admin-spinner" style={{ width: 16, height: 16 }} />
                                Listening for trigger events across all zones...
                            </div>
                        ) : (
                            <div className="admin-feed">
                                {liveFeed.map((ev, i) => (
                                    <div key={i} className={`admin-feed-item ${ev.status === "resolved" ? "resolved" : ""}`}>
                                        <span className="feed-emoji">{TRIGGER_EMOJI[ev.trigger_type] || "⚡"}</span>
                                        <div className="feed-content">
                                            <span className="feed-type">{ev.trigger_type?.replace(/_/g," ")} in {ev.zone_name}</span>
                                            <span className="feed-detail">{ev.message || ev.worker_name}</span>
                                        </div>
                                        <div className="feed-right">
                                            <span className={`feed-status ${ev.status === "resolved" ? "resolved" : "active"}`}>
                                                {ev.status === "resolved" ? "Resolved" : "Active"}
                                            </span>
                                            <span className="feed-time">{ev.receivedAt}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
                {/* ANALYTICS TAB */}
                {tab === "analytics" && (
                    <AdminAnalytics api={api} />
                )}
            </div>

            {/* TRIGGER MODAL */}
            {showTriggerModal && (
                <div className="admin-modal-overlay" onClick={() => setShowTriggerModal(false)}>
                    <div className="admin-modal" onClick={e => e.stopPropagation()}>
                        <h3 className="admin-modal-title">⚡ Fire Manual Trigger</h3>

                        <label className="admin-modal-label">Select Zone</label>
                        <select className="admin-modal-select" value={triggerZone} onChange={e => setTriggerZone(e.target.value)}>
                            <option value="">— Choose a zone —</option>
                            {Object.entries(allZones).map(([zid, zone]) => (
                                <option key={zid} value={zid}>
                                    {zone.name}, {zone.city} {activeZoneIds.includes(zid) ? "🟢" : ""}
                                </option>
                            ))}
                        </select>

                        <label className="admin-modal-label" style={{ marginTop: 14 }}>Trigger Type</label>
                        <div className="admin-trigger-grid">
                            {TRIGGER_TYPES.map(t => (
                                <button
                                    key={t.key}
                                    className={`admin-trigger-btn ${triggerType === t.key ? "selected" : ""}`}
                                    onClick={() => setTriggerType(t.key)}
                                >
                                    <span>{t.emoji}</span>
                                    <span>{t.label}</span>
                                </button>
                            ))}
                        </div>

                        {triggerMsg && (
                            <div className={`admin-trigger-msg ${triggerMsg.startsWith("❌") ? "error" : "success"}`}>
                                {triggerMsg}
                            </div>
                        )}

                        <div className="admin-modal-actions">
                            <button className="admin-modal-cancel" onClick={() => setShowTriggerModal(false)}>Cancel</button>
                            <button className="admin-modal-fire" onClick={fireTrigger} disabled={triggerLoading}>
                                {triggerLoading ? "Firing..." : `🚀 Fire ${TRIGGER_TYPES.find(t=>t.key===triggerType)?.label}`}
                            </button>
                        </div>
                    </div>
                </div>
            )}

        </div>
    );
}

export default AdminDashboard;
