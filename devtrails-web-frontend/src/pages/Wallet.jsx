import React, { useEffect, useState } from "react";
import API from "../services/api";
import { useNavigate } from "react-router-dom";
import "./Wallet.css";

const CATEGORY_META = {
    initial_credit: { label: "Welcome Bonus",     icon: "🎁", color: "#22c55e" },
    premium_debit:  { label: "Premium Deducted",  icon: "📋", color: "#f87171" },
    auto_renewal:   { label: "Auto Renewal",      icon: "🔄", color: "#f59e0b" },
    claim_credit:   { label: "Claim Payout",      icon: "💰", color: "#22c55e" },
};

function Wallet() {
    const navigate  = useNavigate();
    const workerId  = localStorage.getItem("workerId");

    const [wallet,       setWallet]       = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading,      setLoading]      = useState(true);
    const [filter,       setFilter]       = useState("all");

    useEffect(() => { fetchWallet(); }, []);

    const fetchWallet = async () => {
        setLoading(true);
        try {
            const [walletRes, txRes] = await Promise.all([
                API.get(`/wallet/${workerId}`),
                API.get(`/wallet/${workerId}/transactions`),
            ]);
            setWallet(walletRes.data);
            setTransactions(txRes.data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const filtered = filter === "all"
        ? transactions
        : transactions.filter(t => t.type === filter);

    const formatDate = (iso) => iso
        ? new Date(iso).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })
        : "—";

    if (loading) return (
        <div className="wallet-page">
            <nav className="page-nav">
                <span className="page-nav-title">💰 Wallet</span>
                <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
            </nav>
            <div className="wallet-body">
                <div className="skeleton" style={{ height: 160, marginBottom: 16 }} />
                {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 64, marginBottom: 10 }} />)}
            </div>
        </div>
    );

    return (
        <div className="wallet-page">
            <nav className="page-nav">
                <span className="page-nav-title">💰 Wallet</span>
                <button className="btn-back" onClick={() => navigate("/dashboard")}>← Dashboard</button>
            </nav>

            <div className="wallet-body">

                {/* BALANCE CARD */}
                {wallet && (
                    <div className="wallet-balance-card">
                        <div className="wallet-balance-top">
                            <div>
                                <p className="wallet-balance-label">GigShield Wallet Balance</p>
                                <p className="wallet-balance-amount">₹{Number(wallet.balance).toFixed(2)}</p>
                                <p className="wallet-balance-sub">Available for premiums and payouts</p>
                            </div>
                            <div className="wallet-shield">🛡️</div>
                        </div>
                        <div className="wallet-stats-row">
                            <div className="wallet-stat">
                                <span className="wallet-stat-label">Total Credited</span>
                                <span className="wallet-stat-value credit">+₹{Number(wallet.totalCredited).toFixed(0)}</span>
                            </div>
                            <div className="wallet-stat-divider" />
                            <div className="wallet-stat">
                                <span className="wallet-stat-label">Total Debited</span>
                                <span className="wallet-stat-value debit">-₹{Number(wallet.totalDebited).toFixed(0)}</span>
                            </div>
                            <div className="wallet-stat-divider" />
                            <div className="wallet-stat">
                                <span className="wallet-stat-label">Transactions</span>
                                <span className="wallet-stat-value">{transactions.length}</span>
                            </div>
                        </div>
                    </div>
                )}

                {/* FILTER TABS */}
                <div className="filter-tabs" style={{ marginBottom: 14 }}>
                    {["all", "credit", "debit"].map(f => (
                        <button
                            key={f}
                            className={`filter-tab ${filter === f ? "active" : ""}`}
                            onClick={() => setFilter(f)}
                        >
                            {f === "all" ? "All" : f === "credit" ? "Credits" : "Debits"}
                            <span style={{ marginLeft: 5, opacity: 0.6 }}>
                                ({transactions.filter(t => f === "all" || t.type === f).length})
                            </span>
                        </button>
                    ))}
                </div>

                {/* TRANSACTION LIST */}
                {filtered.length === 0 ? (
                    <div style={{ textAlign: "center", padding: 40, color: "#4a4a62", fontSize: 13 }}>
                        No transactions yet.
                    </div>
                ) : (
                    <div className="wallet-tx-list">
                        {filtered.map((tx) => {
                            const meta = CATEGORY_META[tx.category] || { label: tx.category, icon: "💳", color: "#9090b0" };
                            return (
                                <div key={tx.id} className="wallet-tx-item">
                                    <div className="wallet-tx-icon" style={{ background: `${meta.color}18` }}>
                                        <span>{meta.icon}</span>
                                    </div>
                                    <div className="wallet-tx-content">
                                        <p className="wallet-tx-desc">{tx.description}</p>
                                        <p className="wallet-tx-date">{formatDate(tx.createdAt)}</p>
                                    </div>
                                    <div className="wallet-tx-right">
                                        <span className={`wallet-tx-amount ${tx.type}`}>
                                            {tx.type === "credit" ? "+" : "-"}₹{Number(tx.amount).toFixed(2)}
                                        </span>
                                        <span className="wallet-tx-balance">
                                            Bal: ₹{Number(tx.balanceAfter).toFixed(0)}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}

export default Wallet;
