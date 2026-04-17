import React, { useState } from "react";
import "./Register.css";
import API from "../services/api";
import { useNavigate } from "react-router-dom";

import swiggyLogo from "../assets/Swiggy Logo.png";
import zomatoLogo from "../assets/Zomato Logo.png";
import zeptoLogo from "../assets/Zepto Logo.png";
import blinkitLogo from "../assets/Blinkit Logo.png";
import instamartLogo from "../assets/Swiggy Instamart Logo.jpg";
import bigbasketLogo from "../assets/Big Basket Now Logo.jpg";
import amazonLogo from "../assets/Amazon Logo.png";
import flipkartLogo from "../assets/Flipkart Logo.png";

const PLATFORM_MAP = {
    food: [{ key: "swiggy", logo: swiggyLogo }, { key: "zomato", logo: zomatoLogo }],
    grocery: [{ key: "zepto", logo: zeptoLogo }, { key: "blinkit", logo: blinkitLogo }, { key: "instamart", logo: instamartLogo }, { key: "bigbasket", logo: bigbasketLogo }],
    ecommerce: [{ key: "amazon", logo: amazonLogo }, { key: "flipkart", logo: flipkartLogo }],
};

const STEPS = ["Personal Info", "Work Profile", "Set Password"];

const PLATFORM_FETCH_PHASES = [
    "Fetching user details...",
    "Fetching average working hours...",
    "Fetching average salary per week...",
];

function Register() {
    const navigate = useNavigate();

    const [step, setStep] = useState(1);

    const [form, setForm] = useState({
        name: "",
        phone: "",
        email: "",
        persona: "",
        zoneId: "",
        activeHours: "",
        dailyEarnings: "",
        experienceMonths: "",
        daysPerWeek: "",
        password: "",
    });

    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [error, setError] = useState("");
    const [linkedPlatform, setLinkedPlatform] = useState(null);

    // Platform loading: null = idle, { key, phase } = loading
    const [platformLoading, setPlatformLoading] = useState(null);
    const [loadingLocation, setLoadingLocation] = useState(false);
    const [loadingSubmit, setLoadingSubmit] = useState(false);

    const handleChange = (e) => {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    // ── PLATFORM VERIFY with sequential phases ────────────
    const verifyPlatform = async (platform) => {
        setError("");
        setLinkedPlatform(null);

        // Phase 1
        setPlatformLoading({ key: platform, phase: 0 });
        await new Promise((r) => setTimeout(r, 900));

        // Phase 2
        setPlatformLoading({ key: platform, phase: 1 });
        await new Promise((r) => setTimeout(r, 900));

        // Phase 3
        setPlatformLoading({ key: platform, phase: 2 });
        await new Promise((r) => setTimeout(r, 700));

        // Actual API call
        try {
            const res = await fetch(import.meta.env.PLATFORM_VERIFY_URL || "http://localhost:5001/api/platform/verify-user", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ platform }),
            });
            const data = await res.json();
            if (!res.ok) { setError(data.error); return; }

            setForm((prev) => ({
                ...prev,
                activeHours: Math.floor(data.avgWeeklyHours / 6),
                dailyEarnings: Math.floor(data.avgWeeklyIncome / 6),
                experienceMonths: data.experienceMonths ?? 6,
                daysPerWeek: data.daysPerWeek ?? 6,
            }));
            setLinkedPlatform(platform);
        } catch {
            setError("Platform verification failed. Make sure the mock API is running.");
        } finally {
            setPlatformLoading(null);
        }
    };

    // ── DETECT LOCATION ───────────────────────────────────
    const detectLocation = () => {
        setLoadingLocation(true);
        setError("");
        navigator.geolocation.getCurrentPosition(
            async (pos) => {
                try {
                    const { latitude: lat, longitude: lon } = pos.coords;
                    const res = await fetch(import.meta.env.TRIGGER_API_URL + "/zones" || "http://localhost:5001/api/zones");
                    const zones = await res.json();

                    let selected = "";
                    let minDist = Infinity;
                    Object.keys(zones).forEach((z) => {
                        const d = Math.sqrt(
                            Math.pow(lat - zones[z].lat, 2) +
                            Math.pow(lon - zones[z].lon, 2)
                        );
                        if (d < minDist) { minDist = d; selected = z; }
                    });
                    setForm((prev) => ({ ...prev, zoneId: selected }));
                } catch {
                    setError("Could not fetch zones. Make sure the trigger engine is running.");
                } finally {
                    setLoadingLocation(false);
                }
            },
            () => {
                setError("Location access denied. Please allow location permission.");
                setLoadingLocation(false);
            }
        );
    };

    // ── VALIDATION ────────────────────────────────────────
    const validateStep = () => {
        if (step === 1) {
            if (!form.name.trim()) return "Full name is required.";
            if (!form.phone.trim()) return "Mobile number is required.";
            if (!/^[6-9]\d{9}$/.test(form.phone)) return "Enter a valid 10-digit Indian mobile number.";
        }
        if (step === 2) {
            if (!form.persona) return "Please select your work type.";
            if (!linkedPlatform) return "Please link at least one platform to verify your income.";
            if (!form.zoneId) return "Please detect your location to set your zone.";
        }
        if (step === 3) {
            if (!form.password) return "Password is required.";
            if (form.password.length < 6) return "Password must be at least 6 characters.";
            if (form.password !== confirmPassword) return "Passwords do not match.";
        }
        return null;
    };

    const handleNext = () => {
        const err = validateStep();
        if (err) { setError(err); return; }
        setError("");
        setStep((s) => s + 1);
    };

    const handleBack = () => {
        setError("");
        setStep((s) => s - 1);
    };

    // ── REGISTER SUBMIT ───────────────────────────────────
    const handleRegister = async () => {
        const err = validateStep();
        if (err) { setError(err); return; }

        setError("");
        setLoadingSubmit(true);
        try {
            const payload = {
                name: form.name,
                phone: form.phone,
                email: form.email,
                persona: form.persona,
                zoneId: form.zoneId,

                dailyEarnings: Number(form.dailyEarnings) || 1000,
                activeHours: Number(form.activeHours) || 7,
                experienceMonths: Number(form.experienceMonths) || 6,
                daysPerWeek: Number(form.daysPerWeek) || 6,
                password: form.password
            };
            const res = await API.post("/workers/register", payload);
            const workerId = res.data.workerId;

            await fetch(import.meta.env.TRIGGER_API_URL + "/workers/register" || "http://localhost:5001/api/workers/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    worker_id: workerId,
                    name: form.name,
                    zone_id: form.zoneId,
                    persona: form.persona,
                }),
            });

            navigate("/");
        } catch (err) {
            const msg = err?.response?.data?.message;
            setError(msg || "Registration failed. Please try again.");
        } finally {
            setLoadingSubmit(false);
        }
    };

    // ── RENDER ────────────────────────────────────────────
    const platforms = PLATFORM_MAP[form.persona] || [];

    return (
        <div className="register-container">
            <div className="register-card">

                {/* Brand */}
                <div className="register-brand">
                    <span className="brand-icon">🛡️</span>
                    <h1>Gig<span>Shield</span></h1>
                </div>
                <p className="register-tagline">Create your account — takes under 2 minutes</p>

                {/* Step progress */}
                <div className="step-progress">
                    {STEPS.map((label, i) => {
                        const num = i + 1;
                        const isDone = step > num;
                        const isActive = step === num;
                        return (
                            <React.Fragment key={num}>
                                <div className={`step-item ${isActive ? "active" : ""} ${isDone ? "done" : ""}`}>
                                    <div className="step-circle">{isDone ? "✓" : num}</div>
                                    <span className="step-label">{label}</span>
                                </div>
                                {i < STEPS.length - 1 && (
                                    <div className={`step-connector ${isDone ? "done" : ""}`} />
                                )}
                            </React.Fragment>
                        );
                    })}
                </div>

                {/* ── STEP 1: PERSONAL INFO ── */}
                {step === 1 && (
                    <div className="step-panel">
                        <p className="step-heading">Personal Information</p>
                        <p className="step-sub">Tell us who you are</p>

                        <div className="field-group">
                            <label>Full Name</label>
                            <div className="input-wrapper">
                                <span className="input-icon">👤</span>
                                <input
                                    name="name"
                                    placeholder="e.g. Ravi Kumar"
                                    value={form.name}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        <div className="field-group">
                            <label>Mobile Number</label>
                            <div className="input-wrapper">
                                <span className="input-icon">📱</span>
                                <input
                                    name="phone"
                                    placeholder="10-digit number"
                                    value={form.phone}
                                    onChange={handleChange}
                                    maxLength={10}
                                />
                            </div>
                        </div>

                        <div className="field-group">
                            <label>
                                Email{" "}
                                <span style={{ color: "#4a4a62", fontWeight: 400, textTransform: "none", letterSpacing: 0 }}>
                                    (optional)
                                </span>
                            </label>
                            <div className="input-wrapper">
                                <span className="input-icon">✉️</span>
                                <input
                                    name="email"
                                    placeholder="your@email.com"
                                    value={form.email}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        {error && <div className="error-msg">⚠️ {error}</div>}

                        <div className="step-nav">
                            <button className="btn-next full" onClick={handleNext}>
                                Continue →
                            </button>
                        </div>
                    </div>
                )}

                {/* ── STEP 2: WORK PROFILE ── */}
                {step === 2 && (
                    <div className="step-panel">
                        <p className="step-heading">Work Profile</p>
                        <p className="step-sub">Select your work type and link your platform</p>

                        {/* Persona cards */}
                        <div className="field-group">
                            <label>Work Type</label>
                            <div className="persona-cards">
                                {[
                                    { value: "food", emoji: "🍔", label: "Food" },
                                    { value: "grocery", emoji: "🛒", label: "Grocery" },
                                    { value: "ecommerce", emoji: "📦", label: "E-Commerce" },
                                ].map((p) => (
                                    <div
                                        key={p.value}
                                        className={`persona-card ${form.persona === p.value ? "selected" : ""}`}
                                        onClick={() => {
                                            setForm((prev) => ({ ...prev, persona: p.value }));
                                            setLinkedPlatform(null);
                                        }}
                                    >
                                        <span className="persona-emoji">{p.emoji}</span>
                                        <span className="persona-name">{p.label}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Platform logos */}
                        {form.persona && (
                            <div className="field-group">
                                <p className="platform-section-label">Link Your Platform</p>
                                <div className="platforms">
                                    {platforms.map((p) => (
                                        <button
                                            key={p.key}
                                            className={`platform-btn ${linkedPlatform === p.key ? "linked" : ""}`}
                                            onClick={() => verifyPlatform(p.key)}
                                            disabled={platformLoading !== null}
                                            title={p.key}
                                        >
                                            <img src={p.logo} alt={p.key} />
                                            {platformLoading?.key === p.key && (
                                                <div className="platform-loading">
                                                    <span className="spinner-sm" />
                                                </div>
                                            )}
                                            {linkedPlatform === p.key && (
                                                <span className="platform-linked-badge">✓</span>
                                            )}
                                        </button>
                                    ))}
                                </div>

                                {/* Sequential phase message */}
                                {platformLoading && (
                                    <div className="platform-fetch-status">
                                        <span className="spinner-sm" />
                                        <span>{PLATFORM_FETCH_PHASES[platformLoading.phase]}</span>
                                    </div>
                                )}

                                {linkedPlatform && !platformLoading && (
                                    <div className="success-msg">
                                        ✅ Linked to <strong>{linkedPlatform}</strong> — earnings auto-filled
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Location */}
                        <div className="field-group">
                            <label>Your Zone</label>
                            <button
                                className="btn-location"
                                onClick={detectLocation}
                                disabled={loadingLocation}
                            >
                                {loadingLocation ? (
                                    <><span className="spinner-sm" /> Detecting location...</>
                                ) : (
                                    <>📍 Detect My Location</>
                                )}
                            </button>
                            <div className="input-wrapper">
                                <span className="input-icon">🗺️</span>
                                <div className={`zone-display ${!form.zoneId ? "empty" : ""}`}>
                                    {form.zoneId || "Zone will appear after detection"}
                                </div>
                            </div>
                        </div>

                        {error && <div className="error-msg">⚠️ {error}</div>}

                        <div className="step-nav">
                            <button className="btn-back" onClick={handleBack}>← Back</button>
                            <button className="btn-next" onClick={handleNext}>Continue →</button>
                        </div>
                    </div>
                )}

                {/* ── STEP 3: PASSWORD ── */}
                {step === 3 && (
                    <div className="step-panel">
                        <p className="step-heading">Secure Your Account</p>
                        <p className="step-sub">Set a strong password to protect your account</p>

                        <div className="field-group">
                            <label>Password</label>
                            <div className="input-wrapper">
                                <span className="input-icon">🔒</span>
                                <input
                                    type={showPassword ? "text" : "password"}
                                    placeholder="Minimum 6 characters"
                                    value={form.password}
                                    onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
                                />
                                <button className="toggle-pw" onClick={() => setShowPassword(!showPassword)} tabIndex={-1}>
                                    {showPassword ? "🙈" : "👁️"}
                                </button>
                            </div>
                        </div>

                        <div className="field-group">
                            <label>Confirm Password</label>
                            <div className="input-wrapper">
                                <span className="input-icon">🔒</span>
                                <input
                                    type={showConfirm ? "text" : "password"}
                                    placeholder="Re-enter your password"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                />
                                <button className="toggle-pw" onClick={() => setShowConfirm(!showConfirm)} tabIndex={-1}>
                                    {showConfirm ? "🙈" : "👁️"}
                                </button>
                            </div>
                        </div>

                        {error && <div className="error-msg">⚠️ {error}</div>}

                        <div className="step-nav">
                            <button className="btn-back" onClick={handleBack}>← Back</button>
                            <button className="btn-submit" onClick={handleRegister} disabled={loadingSubmit}>
                                {loadingSubmit ? (
                                    <><span className="spinner" /> Creating Account...</>
                                ) : (
                                    "Create Account 🚀"
                                )}
                            </button>
                        </div>
                    </div>
                )}

                <p className="register-footer">
                    Already have an account?{" "}
                    <span onClick={() => navigate("/")}>Sign in</span>
                </p>

            </div>
        </div>
    );
}

export default Register;
