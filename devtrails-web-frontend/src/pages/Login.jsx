import React, { useState } from "react";
import "./Login.css";
import API from "../services/api";
import { useNavigate } from "react-router-dom";

function Login() {
    const [phone, setPhone] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async () => {
        if (!phone || !password) {
            setError("Please enter your phone number and password.");
            return;
        }

        setError("");
        setLoading(true);

        try {
            const res = await API.post("/workers/login", { phone, password });
            localStorage.setItem("token", res.data.token);
            localStorage.setItem("workerId", res.data.workerId);
            navigate("/dashboard");
        } catch (err) {
            const msg = err?.response?.data?.message;
            setError(msg || "Invalid phone number or password.");
        } finally {
            setLoading(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter") handleLogin();
    };

    return (
        <div className="login-container">
            <div className="login-card">

                {/* Brand */}
                <div className="login-brand">
                    <span className="brand-icon">🛡️</span>
                    <h1>Gig<span>Shield</span></h1>
                </div>
                <p className="login-tagline">Parametric income protection for gig workers</p>

                <div className="login-divider" />

                {/* Phone */}
                <div className="field-group">
                    <label>Mobile Number</label>
                    <div className="input-wrapper">
                        <span className="input-icon">📱</span>
                        <input
                            type="tel"
                            placeholder="Enter your 10-digit number"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            onKeyDown={handleKeyDown}
                            maxLength={10}
                        />
                    </div>
                </div>

                {/* Password */}
                <div className="field-group">
                    <label>Password</label>
                    <div className="input-wrapper">
                        <span className="input-icon">🔒</span>
                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            onKeyDown={handleKeyDown}
                        />
                        <button
                            className="toggle-pw"
                            onClick={() => setShowPassword(!showPassword)}
                            tabIndex={-1}
                        >
                            {showPassword ? "🙈" : "👁️"}
                        </button>
                    </div>
                </div>

                {error && <div className="error-msg">⚠️ {error}</div>}

                <button
                    className="btn-primary"
                    onClick={handleLogin}
                    disabled={loading}
                >
                    {loading ? (
                        <>
                            <span className="spinner" />
                            Signing in...
                        </>
                    ) : (
                        "Sign In"
                    )}
                </button>

                <p className="login-footer">
                    Don't have an account?{" "}
                    <span onClick={() => navigate("/register")}>Create one</span>
                </p>

            </div>
        </div>
    );
}

export default Login;
