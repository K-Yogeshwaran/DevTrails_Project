import React, { useEffect, useState, useRef } from "react";
import "./Dashboard.css";
import API from "../services/api";
import { useNavigate } from "react-router-dom";
import { io } from "socket.io-client";

const PERSONA_EMOJI = { food: "🍔", grocery: "🛒", ecommerce: "📦" };
const TRIGGER_EMOJI = { rainfall: "🌧️", aqi: "😷", heat: "🌡️", platform_downtime: "📵", curfew: "🚫" };

const TRIGGER_TYPES = [
    { key: "rainfall",          label: "Heavy Rainfall",     emoji: "🌧️", value: 45  },
    { key: "aqi",               label: "Hazardous AQI",      emoji: "😷", value: 250 },
    { key: "heat",              label: "Extreme Heat",       emoji: "🌡️", value: 44  },
    { key: "platform_downtime", label: "Platform Downtime",  emoji: "📵", value: 60  },
    { key: "curfew",            label: "Curfew / Section 144",emoji: "🚫", value: 1   },
];

// Live timer card for an active disruption
function ActiveDisruptionCard({ event, onResolve, resolving }) {
    const [elapsed, setElapsed] = useState(event.elapsedSeconds || 0);

    useEffect(() => {
        const interval = setInterval(() => setElapsed(s => s + 1), 1000);
        return () => clearInterval(interval);
    }, []);

    const hrs    = Math.floor(elapsed / 3600);
    const mins   = Math.floor((elapsed % 3600) / 60);
    const secs   = elapsed % 60;
    const timeStr = hrs > 0 ? `${hrs}h ${mins}m ${secs}s` : `${mins}m ${secs}s`;
    const emoji  = TRIGGER_EMOJI[event.triggerType] || "⚡";
    // Capture eventId at render time to avoid stale closure
    const eventId = event.eventId;

    return (
        <div className="active-disruption-card">
            <div className="ad-left">
                <span className="ad-emoji">{emoji}</span>
                <div>
                    <p className="ad-type">{event.triggerType?.replace(/_/g, " ")}</p>
                    <p className="ad-zone">{event.zoneName}</p>
                    {!eventId && (
                        <p style={{ fontSize: 10, color: "#f59e0b" }}>⚠️ Syncing with server...</p>
                    )}
                </div>
            </div>
            <div className="ad-right">
                <span className="ad-timer">{timeStr}</span>
                <span className="ad-label">affected</span>
                <button
                    className="ad-resolve-btn"
                    onClick={() => onResolve({ ...event, eventId })}
                    disabled={resolving || !eventId}
                    title={!eventId ? "Waiting for server sync..." : "Resolve this disruption"}
                >
                    {resolving ? "Resolving..." : !eventId ? "Syncing..." : "✅ Resolve"}
                </button>
            </div>
        </div>
    );
}

function Dashboard() {
    const navigate  = useNavigate();
    const workerId  = localStorage.getItem("workerId");
    const socketRef = useRef(null);

    const [profile,       setProfile]       = useState(null);
    const [policy,        setPolicy]        = useState(null);
    const [claims,        setClaims]        = useState([]);
    const [wallet,        setWallet]        = useState(null);
    const [loading,       setLoading]       = useState(true);
    const [liveMsg,       setLiveMsg]       = useState("Listening for disruption events...");
    const [liveTriggers,  setLiveTriggers]  = useState(0);
    const [simType,       setSimType]       = useState("rainfall");
    const [simLoading,    setSimLoading]    = useState(false);
    const [simMsg,        setSimMsg]        = useState("");
    const [isOnline,      setIsOnline]      = useState(false);
    const [onlineLoading, setOnlineLoading] = useState(false);
    const [locationName,  setLocationName]  = useState("");
    const [activeZoneId,  setActiveZoneId]  = useState(null);
    const activeZoneIdRef = useRef(null);

    const [activeEvents,  setActiveEvents]  = useState([]);  // live disruptions
    const [resolvingId,   setResolvingId]   = useState(null);

    useEffect(() => {
        fetchAll();
        connectSocket();
        // Restore online state if worker was online before reload
        const savedZone = localStorage.getItem("zoneId");
        if (savedZone) {
            setActiveZoneId(savedZone);
            activeZoneIdRef.current = savedZone;
            setIsOnline(true);
            const parts = savedZone.replace("zone_", "").split("_");
            const city  = parts[0];
            const area  = parts.slice(1).join(" ");
            setLocationName(
                `${area.replace(/\b\w/g, c => c.toUpperCase())}, ${city.replace(/\b\w/g, c => c.toUpperCase())}`
            );
            fetchActiveEvents(savedZone);
        }
        return () => {
            if (socketRef.current) {
                socketRef.current.disconnect();
                socketRef.current = null;
            }
        };
    }, []);

    const fetchActiveEvents = async (zoneId) => {
        const zone = zoneId || activeZoneIdRef.current || localStorage.getItem("zoneId");
        if (!zone) return;
        try {
            const res = await API.get(`/trigger-events/active/${zone}`);
            const dbEvents = res.data || [];
            setActiveEvents(prev => {
                const dbIds = new Set(dbEvents.map(e => e.eventId));
                const localOnly = prev.filter(e => !dbIds.has(e.eventId));
                return [...dbEvents, ...localOnly];
            });
        } catch {
            // On error, keep existing local events
        }
    };

    const fetchAll = async () => {
        setLoading(true);
        try {
            const [profileRes, claimsRes] = await Promise.all([
                API.get(`/workers/${workerId}`),
                API.get(`/claims/worker/${workerId}`),
            ]);
            setProfile(profileRes.data);
            setClaims(claimsRes.data);

            try {
                const policyRes = await API.get(`/policies/${workerId}/current`);
                setPolicy(policyRes.data);
            } catch {
                setPolicy(null);
            }
            try {
                const walletRes = await API.get(`/wallet/${workerId}`);
                setWallet(walletRes.data);
            } catch {
                setWallet(null);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const connectSocket = () => {
        if (socketRef.current?.connected) return;
        const socket = io("http://localhost:5001", {
            transports: ["polling"],
            reconnectionAttempts: 5,
            reconnectionDelay: 2000,
        });
        socketRef.current = socket;

        socket.on("connect", () => {
            socket.emit("subscribe_worker", { worker_id: workerId });
        });

        socket.on("trigger_fired", (event) => {
            if (event.worker_id !== workerId) return;
            const emoji = TRIGGER_EMOJI[event.trigger_type] || "⚡";
            setLiveMsg(`${emoji} ${event.message}`);
            setLiveTriggers((n) => n + 1);
            // Use ref so closure always has latest zone
            fetchActiveEvents(activeZoneIdRef.current);
            setTimeout(fetchAll, 3000);
        });

        socket.on("trigger_resolved", (event) => {
            if (event.worker_id !== workerId) return;
            const emoji = TRIGGER_EMOJI[event.trigger_type] || "⚡";
            setLiveMsg(`✅ ${emoji} ${event.message}`);
            setActiveEvents(prev => prev.filter(e => e.eventId !== event.event_id));
            setTimeout(fetchAll, 4000);
        });
    };

    const logout = () => {
        localStorage.clear();
        navigate("/");
    };

    // ── GO ONLINE ─────────────────────────────────────────
    const goOnline = () => {
        setOnlineLoading(true);
        // Try GPS first
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                async (pos) => {
                    const { latitude: lat, longitude: lon } = pos.coords;
                    try {
                        // Resolve nearest zone from trigger engine
                        const zoneRes = await fetch("http://localhost:5001/api/workers/resolve-zone", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ lat, lon }),
                        });
                        const zoneData = await zoneRes.json();

                        // Register in trigger engine with GPS coords
                        await fetch("http://localhost:5001/api/workers/register", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({
                                worker_id: workerId,
                                name:      profile?.name || workerId,
                                persona:   profile?.persona || "food",
                                lat,
                                lon,
                            }),
                        });

                        setActiveZoneId(zoneData.zone_id);
                        setLocationName(`${zoneData.zone_name}, ${zoneData.city}`);
                        setIsOnline(true);
                        activeZoneIdRef.current = zoneData.zone_id;
                        localStorage.setItem("zoneId", zoneData.zone_id);
                        fetchActiveEvents(zoneData.zone_id);
                    } catch {
                        // Fallback to registered zone if trigger engine unreachable
                        fallbackOnline();
                    } finally {
                        setOnlineLoading(false);
                    }
                },
                () => {
                    // GPS denied — use registered zone
                    fallbackOnline();
                    setOnlineLoading(false);
                }
            );
        } else {
            fallbackOnline();
            setOnlineLoading(false);
        }
    };

    const fallbackOnline = async () => {
        // Use the zone stored in DB during registration
        const zoneId = profile?.zoneId;
        if (!zoneId) return;
        try {
            await fetch("http://localhost:5001/api/workers/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    worker_id: workerId,
                    name:      profile?.name || workerId,
                    persona:   profile?.persona || "food",
                    zone_id:   zoneId,
                }),
            });
            // Format zone_id to readable name
            const parts = zoneId.replace("zone_", "").split("_");
            const city  = parts[0];
            const area  = parts.slice(1).join(" ");
            setLocationName(`${area.replace(/\b\w/g, c => c.toUpperCase())}, ${city.replace(/\b\w/g, c => c.toUpperCase())}`);
            setActiveZoneId(zoneId);
            setIsOnline(true);
            activeZoneIdRef.current = zoneId;
            localStorage.setItem("zoneId", zoneId);
            fetchActiveEvents(zoneId);
        } catch (e) {
            console.error("Fallback online failed", e);
        }
    };

    const goOffline = async () => {
        setOnlineLoading(true);
        try {
            await fetch("http://localhost:5001/api/workers/deregister", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ worker_id: workerId, zone_id: activeZoneId }),
            });
        } catch (e) {
            console.error("Deregister failed", e);
        } finally {
            setIsOnline(false);
            setLocationName("");
            setActiveZoneId(null);
            activeZoneIdRef.current = null;
            localStorage.removeItem("zoneId");
            setOnlineLoading(false);
        }
    };

    const fireManualTrigger = async () => {
        const zoneToFire = activeZoneId || profile?.zoneId;
        if (!zoneToFire) return;
        setSimLoading(true);
        setSimMsg("");
        const chosen = TRIGGER_TYPES.find(t => t.key === simType);
        try {
            const res = await fetch("http://localhost:5001/api/triggers/manual", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    zone_id:      zoneToFire,
                    trigger_type: simType,
                    value:        chosen?.value ?? 45,
                }),
            });
            const data = await res.json();
            if (!res.ok) {
                setSimMsg(`❌ ${data.error || "Trigger failed"}`);
            } else {
                setSimMsg(`✅ ${chosen?.emoji} ${chosen?.label} triggered!`);
                // Add event locally immediately so card shows without waiting for DB
                const localEvent = {
                    eventId:       data.event_id,
                    triggerType:   simType,
                    zoneId:        zoneToFire,
                    zoneName:      locationName || zoneToFire.replace("zone_","").replace(/_/g," "),
                    status:        "active",
                    elapsedSeconds: 0,
                };
                setActiveEvents(prev => {
                    const exists = prev.find(e => e.eventId === localEvent.eventId);
                    return exists ? prev : [localEvent, ...prev];
                });
                // Refresh from DB after delay — no reload, just merge
                setTimeout(() => fetchActiveEvents(zoneToFire), 3000);
            }
        } catch {
            setSimMsg("❌ Trigger engine not reachable. Make sure port 5001 is running.");
        } finally {
            setSimLoading(false);
        }
    };

    const resolveEvent = async (event) => {
        if (!event?.eventId) {
            setSimMsg("❌ Cannot resolve: event ID is missing. Make sure Spring Boot is running on port 8080.");
            return;
        }
        setResolvingId(event.eventId);
        try {
            await API.post("/trigger-events/resolve", {
                eventId:         event.eventId,
                activeWorkerIds: workerId ? [workerId] : [],
            });
            setActiveEvents(prev => prev.filter(e => e.eventId !== event.eventId));
            setSimMsg(`✅ Disruption resolved. Processing claim...`);
            setTimeout(() => navigate(`/claims?eventId=${encodeURIComponent(event.eventId)}`), 1000);
            setTimeout(fetchAll, 5000);
        } catch (err) {
            const msg = err?.response?.data?.message || err?.response?.status;
            setSimMsg(`❌ Resolve failed: ${msg}`);
        } finally {
            setResolvingId(null);
        }
    };

    const approved = claims.filter(c => c.status === "approved").length;
    const flagged  = claims.filter(c => c.status === "flagged").length;
    const recent   = claims.slice(0, 4);

    const coveragePct = policy
        ? Math.min(100, (Number(policy.coverageUsed) / Number(policy.coverageCap)) * 100)
        : 0;

    const formatDate = (iso) => iso
        ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short" })
        : "—";

    if (loading) {
        return (
            <div className="dashboard">
                <div className="dash-nav">
                    <div className="dash-nav-brand">🛡️ Gig<span>Shield</span></div>
                </div>
                <div className="dash-body">
                    <div className="skeleton" style={{ height: 80, marginBottom: 14 }} />
                    <div className="stats-row">
                        {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 90 }} />)}
                    </div>
                    <div className="dash-grid">
                        <div className="skeleton" style={{ height: 200 }} />
                        <div className="skeleton" style={{ height: 200 }} />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="dashboard">

            {/* NAVBAR */}
            <nav className="dash-nav">
                <div className="dash-nav-brand">🛡️ Gig<span>Shield</span></div>
                <div className="dash-nav-right">
                    {profile && (
                        <span className="dash-nav-worker">
                            {PERSONA_EMOJI[profile.persona]} {profile.name}
                        </span>
                    )}
                    {/* GO ONLINE / OFFLINE TOGGLE */}
                    <button
                        className={`btn-online-toggle ${isOnline ? "online" : "offline"}`}
                        onClick={isOnline ? goOffline : goOnline}
                        disabled={onlineLoading}
                    >
                        {onlineLoading ? (
                            <><span className="spinner-sm" /> {isOnline ? "Going offline..." : "Going online..."}</>
                        ) : isOnline ? (
                            <>🟢 Online</>
                        ) : (
                            <>⚪ Go Online</>
                        )}
                    </button>
                    <button className="btn-logout" onClick={logout}>Logout</button>
                </div>
            </nav>

            {/* LIVE TRIGGER STRIP */}
            <div className="trigger-strip">
                <div className="trigger-strip-dot" />
                <span className="trigger-strip-msg">{liveMsg}</span>
                {liveTriggers > 0 && (
                    <span className="trigger-strip-badge">{liveTriggers} new</span>
                )}
            </div>

            <div className="dash-body">

                {/* PROFILE */}
                {profile && (
                    <div className="profile-card">
                        <div className="profile-avatar">
                            {PERSONA_EMOJI[profile.persona] || "👤"}
                        </div>
                        <div className="profile-info">
                            <h2>{profile.name}</h2>
                            <div className="profile-meta">
                                <span>📍 <b>
                                    {isOnline && locationName
                                        ? locationName
                                        : profile.zoneId?.replace("zone_", "").replace(/_/g, " ")}
                                </b></span>
                                <span>💰 <b>₹{profile.dailyEarnings}/day</b></span>
                                <span>⏱ <b>{profile.activeHours}hrs/day</b></span>
                                <span>📅 <b>{profile.daysPerWeek} days/week</b></span>
                                {isOnline && (
                                    <span className="online-badge">🟢 Active • Covered</span>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* STATS */}
                <div className="stats-row">
                    <div className="stat-card">
                        <span className="stat-label">Total Claims</span>
                        <span className="stat-value accent">{claims.length}</span>
                        <span className="stat-sub">{approved} approved · {flagged} flagged</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Active Policy</span>
                        <span className={`stat-value ${policy ? "green" : ""}`}>
                            {policy ? policy.tier.toUpperCase() : "None"}
                        </span>
                        <span className="stat-sub">
                            {policy ? `₹${policy.weeklyPremium}/week` : "No coverage this week"}
                        </span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Wallet Balance</span>
                        <span className="stat-value green">
                            {wallet ? `₹${Number(wallet.balance).toFixed(0)}` : "—"}
                        </span>
                        <span className="stat-sub">
                            {wallet ? `₹${Number(wallet.totalCredited).toFixed(0)} in · ₹${Number(wallet.totalDebited).toFixed(0)} out` : "Loading..."}
                        </span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Coverage Left</span>
                        <span className="stat-value green">
                            {policy ? `₹${Number(policy.coverageRemaining).toFixed(0)}` : "—"}
                        </span>
                        <span className="stat-sub">
                            {policy ? `of ₹${policy.coverageCap} cap` : "Subscribe to get covered"}
                        </span>
                    </div>
                </div>

                {/* BOTTOM GRID */}
                <div className="dash-grid">

                    {/* POLICY CARD */}
                    <div className="section-card">
                        <div className="section-header">
                            <span className="section-title">📜 Policy</span>
                            <span className="section-link" onClick={() => navigate("/policy")}>
                                {policy ? "View details →" : "Get covered →"}
                            </span>
                        </div>

                        {policy ? (
                            <>
                                <span className={`policy-tier-badge tier-${policy.tier}`}>
                                    {policy.tier}
                                </span>
                                <div className="coverage-bar-wrap">
                                    <div className="coverage-bar-label">
                                        <span>Coverage used</span>
                                        <span>₹{Number(policy.coverageUsed).toFixed(0)} / ₹{policy.coverageCap}</span>
                                    </div>
                                    <div className="coverage-bar">
                                        <div className="coverage-bar-fill" style={{ width: `${coveragePct}%` }} />
                                    </div>
                                </div>
                                <div className="policy-row">
                                    <span>Policy #</span>
                                    <span>{policy.policyNumber}</span>
                                </div>
                                <div className="policy-row">
                                    <span>Valid till</span>
                                    <span>{policy.weekEnd}</span>
                                </div>
                                <div className="policy-row">
                                    <span>Season</span>
                                    <span style={{ textTransform: "capitalize" }}>{policy.season}</span>
                                </div>
                            </>
                        ) : (
                            <>
                                <p className="no-policy-msg">No active policy this week</p>
                                <button className="btn-get-policy" onClick={() => navigate("/policy")}>
                                    Get Covered Now
                                </button>
                            </>
                        )}
                    </div>

                    {/* RECENT CLAIMS */}
                    <div className="section-card">
                        <div className="section-header">
                            <span className="section-title">📋 Recent Claims</span>
                            <span className="section-link" onClick={() => navigate("/claims")}>
                                View all →
                            </span>
                        </div>

                        {recent.length === 0 ? (
                            <p className="empty-state">No claims yet. Claims appear automatically when a disruption is detected.</p>
                        ) : (
                            recent.map((c) => (
                                <div className="claim-item" key={c.claimId}>
                                    <div className="claim-left">
                                        <span className="claim-type">
                                            {TRIGGER_EMOJI[c.triggerType] || "⚡"} {c.triggerType?.replace(/_/g, " ")}
                                        </span>
                                        <span className="claim-date">{formatDate(c.triggeredAt)}</span>
                                    </div>
                                    <div className="claim-right">
                                        <span className="claim-amount">₹{Number(c.payoutAmount).toFixed(0)}</span>
                                        <span className={`status-badge status-${c.status}`}>{c.status}</span>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                </div>

                {/* ACTION BUTTONS */}
                <div className="dash-actions">
                    <button className="btn-action" onClick={() => navigate("/policy")}>
                        📜 Manage Policy
                    </button>
                    <button className="btn-action" onClick={() => navigate("/claims")}>
                        📋 View All Claims
                    </button>
                    <button className="btn-action" onClick={() => navigate("/wallet")}>
                        💰 My Wallet
                    </button>
                </div>

                {isOnline && (
                    <div className="active-disruptions-panel">
                        <div className="sim-header">
                            <span className="sim-title">🚨 Active Disruptions in Your Zone</span>
                            {activeEvents.length > 0 && (
                                <span className="active-count-badge">{activeEvents.length} ongoing</span>
                            )}
                        </div>
                        {activeEvents.length === 0 ? (
                            <p style={{ fontSize: 13, color: "#4a4a62", marginTop: 10 }}>
                                ✅ No active disruptions in your zone right now.
                            </p>
                        ) : (
                            activeEvents.map((ev, idx) => (
                                <ActiveDisruptionCard
                                    key={ev.eventId || `local-${idx}`}
                                    event={ev}
                                    onResolve={resolveEvent}
                                    resolving={resolvingId === ev.eventId}
                                />
                            ))
                        )}
                    </div>
                )}

                {/* SIMULATE DISRUPTION */}
                <div className="sim-panel">
                    <div className="sim-header">
                        <span className="sim-title">⚡ Simulate Disruption</span>
                        <span className="sim-badge">Demo Tool</span>
                    </div>
                    <p className="sim-desc">
                        Fire a manual disruption trigger in your zone to test the end-to-end claim flow.
                    </p>
                    <div className="sim-trigger-grid">
                        {TRIGGER_TYPES.map((t) => (
                            <button
                                key={t.key}
                                className={`sim-trigger-btn ${simType === t.key ? "selected" : ""}`}
                                onClick={() => setSimType(t.key)}
                            >
                                <span className="sim-trigger-emoji">{t.emoji}</span>
                                <span className="sim-trigger-label">{t.label}</span>
                            </button>
                        ))}
                    </div>
                    {simMsg && (
                        <div className={`sim-msg ${simMsg.startsWith("❌") ? "sim-msg-error" : "sim-msg-success"}`}>
                            {simMsg}
                        </div>
                    )}
                    <button
                        className="sim-fire-btn"
                        onClick={fireManualTrigger}
                        disabled={simLoading || !profile?.zoneId}
                    >
                        {simLoading ? (
                            <><span className="spinner-sm" /> Firing trigger...</>
                        ) : (
                            `🚀 Fire ${TRIGGER_TYPES.find(t => t.key === simType)?.label}`
                        )}
                    </button>
                </div>

            </div>
        </div>
    );
}

export default Dashboard;
