import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

function AdminLogin() {
    const navigate  = useNavigate();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPw,   setShowPw]   = useState(false);
    const [error,    setError]    = useState("");
    const [loading,  setLoading]  = useState(false);

    const handleLogin = async () => {
        if (!username || !password) { setError("Username and password required."); return; }
        setError(""); setLoading(true);
        try {
            const res = await fetch("http://localhost:8080/api/admin/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });
            const data = await res.json();
            if (!res.ok) { setError(data.error || "Invalid credentials"); return; }
            localStorage.setItem("adminToken",    data.token);
            localStorage.setItem("adminUsername", data.username);
            navigate("/admin");
        } catch {
            setError("Cannot reach backend. Make sure Spring Boot is running.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.page}>
            <div style={styles.card}>
                <div style={styles.brand}>
                    <span style={{ fontSize: 28 }}>🛡️</span>
                    <h1 style={styles.brandText}>Gig<span style={{ color: "#6c63ff" }}>Shield</span></h1>
                </div>
                <p style={styles.subtitle}>Admin Control Panel</p>
                <div style={styles.divider} />

                <div style={styles.field}>
                    <label style={styles.label}>Username</label>
                    <input
                        style={styles.input}
                        placeholder="admin"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        onKeyDown={e => e.key === "Enter" && handleLogin()}
                    />
                </div>

                <div style={styles.field}>
                    <label style={styles.label}>Password</label>
                    <div style={{ position: "relative" }}>
                        <input
                            style={styles.input}
                            type={showPw ? "text" : "password"}
                            placeholder="••••••••"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            onKeyDown={e => e.key === "Enter" && handleLogin()}
                        />
                        <button
                            onClick={() => setShowPw(!showPw)}
                            style={styles.eyeBtn}
                        >{showPw ? "🙈" : "👁️"}</button>
                    </div>
                </div>

                {error && <div style={styles.error}>⚠️ {error}</div>}

                <button style={styles.btn} onClick={handleLogin} disabled={loading}>
                    {loading ? "Signing in..." : "Sign In as Admin"}
                </button>

                <p style={styles.back} onClick={() => navigate("/")}>← Back to Worker App</p>
            </div>
        </div>
    );
}

const styles = {
    page:     { minHeight: "100vh", background: "#0d0d14", display: "flex", alignItems: "center", justifyContent: "center" },
    card:     { background: "#16161f", border: "1px solid #2a2a3a", borderRadius: 20, padding: "44px 40px 36px", width: 400, boxShadow: "0 24px 60px rgba(0,0,0,0.5)" },
    brand:    { display: "flex", alignItems: "center", gap: 10, justifyContent: "center", marginBottom: 4 },
    brandText:{ fontSize: 26, fontWeight: 700, color: "#fff", margin: 0 },
    subtitle: { textAlign: "center", fontSize: 13, color: "#5a5a72", marginBottom: 24 },
    divider:  { height: 1, background: "#2a2a3a", marginBottom: 24 },
    field:    { marginBottom: 16, textAlign: "left" },
    label:    { display: "block", fontSize: 11.5, fontWeight: 600, color: "#7a7a9a", textTransform: "uppercase", letterSpacing: "0.7px", marginBottom: 6 },
    input:    { width: "100%", padding: "11px 14px", background: "#1e1e2e", border: "1px solid #2a2a3a", borderRadius: 10, color: "#e0e0f0", fontSize: 14, outline: "none", boxSizing: "border-box" },
    eyeBtn:   { position: "absolute", right: 12, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", cursor: "pointer", fontSize: 15, color: "#4a4a62" },
    error:    { background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.3)", color: "#f87171", fontSize: 13, padding: "9px 12px", borderRadius: 8, marginBottom: 14 },
    btn:      { width: "100%", padding: 13, background: "#6c63ff", border: "none", borderRadius: 10, color: "#fff", fontSize: 15, fontWeight: 600, cursor: "pointer", marginTop: 4 },
    back:     { textAlign: "center", marginTop: 20, fontSize: 13, color: "#4a4a62", cursor: "pointer" },
};

export default AdminLogin;
