import React, { useEffect, useState } from "react";

function AdminAnalytics({ api }) {
    const [lossInfo, setLossInfo] = useState(null);
    const [predictive, setPredictive] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAnalytics = async () => {
            try {
                const [l, p] = await Promise.all([
                    api("/api/admin/analytics/loss-ratio"),
                    api("/api/admin/analytics/predictive")
                ]);
                setLossInfo(l);
                setPredictive(p);
            } catch (e) {
                console.error("Analytics fetch failed:", e);
            } finally {
                setLoading(false);
            }
        };
        fetchAnalytics();
    }, []);

    if (loading) return (
        <div className="admin-empty">
            <span className="admin-spinner" style={{ width: 16, height: 16 }} />
            Crunching actuarial data...
        </div>
    );

    return (
        <div className="admin-analytics">
            <div className="analytics-grid">
                {/* LOSS RATIO CARD */}
                <div className="admin-section analytics-card">
                    <h3 className="admin-section-title">📉 Loss Ratio Analysis</h3>
                    <div className="loss-ratio-main">
                        <div className="ratio-circle">
                            <span className="ratio-value">{lossInfo?.lossRatio.toFixed(1)}%</span>
                            <span className="ratio-label">Loss Ratio</span>
                        </div>
                        <div className="ratio-stats">
                            <div className="ratio-stat">
                                <span className="label">Total Premiums</span>
                                <span className="value">₹{lossInfo?.totalPremiums.toLocaleString()}</span>
                            </div>
                            <div className="ratio-stat">
                                <span className="label">Claims Paid</span>
                                <span className="value red">₹{lossInfo?.totalClaimsPaid.toLocaleString()}</span>
                            </div>
                            <p className="ratio-desc">
                                A ratio under 60% indicates a healthy, sustainable insurance pool.
                            </p>
                        </div>
                    </div>
                    
                    <h4 className="chart-title">Collected vs Disbursed (Last 4 Months)</h4>
                    <div className="mini-chart">
                        {lossInfo?.history.map((h, i) => (
                            <div key={i} className="chart-col">
                                <div className="bar-group">
                                    <div className="bar premium" style={{ height: `${Math.min(100, (h.premiums / 60000) * 100)}%` }} title={`Premiums: ${h.premiums}`} />
                                    <div className="bar claims" style={{ height: `${Math.min(100, (h.claims / 60000) * 100)}%` }} title={`Claims: ${h.claims}`} />
                                </div>
                                <span className="col-label">{h.month}</span>
                            </div>
                        ))}
                    </div>
                    <div className="chart-legend">
                        <span className="legend-item"><i className="dot premium" /> Premiums</span>
                        <span className="legend-item"><i className="dot claims" /> Claims Paid</span>
                    </div>
                </div>

                {/* PREDICTIVE ANALYTICS CARD */}
                <div className="admin-section analytics-card">
                    <h3 className="admin-section-title">🔮 Predictive Coverage Needs (Next 7 Days)</h3>
                    <div className="predictive-payout">
                        <div>
                            <span className="payout-label">Est. Next Week Payout</span>
                            <span className="payout-value">₹{predictive?.estimatedNextWeekPayout.toLocaleString()}</span>
                        </div>
                        <div className="risk-badge high">High Probability</div>
                    </div>
                    
                    <div className="risk-heatmap">
                        <h4 className="chart-title">Zone Disruption Probabilities</h4>
                        {predictive?.zoneRisks.map((z, i) => (
                            <div key={i} className="risk-item">
                                <div className="risk-info">
                                    <span className="zone-name">{z.zoneName}</span>
                                    <span className="risk-type">{z.primaryRisk}</span>
                                </div>
                                <div className="risk-visual">
                                    <div className="risk-bar-bg">
                                        <div 
                                            className="risk-bar-fill" 
                                            style={{ 
                                                width: `${z.riskProbability * 100}%`,
                                                background: z.riskProbability > 0.7 ? '#ef4444' : z.riskProbability > 0.4 ? '#f59e0b' : '#22c55e'
                                            }} 
                                        />
                                    </div>
                                    <span className="risk-pct">{(z.riskProbability * 100).toFixed(0)}%</span>
                                </div>
                                <span className="affected-count">{z.affectedWorkers} workers at risk</span>
                            </div>
                        ))}
                    </div>
                    <p className="ai-insight">
                        💡 <b>AI Insight:</b> Heavy rainfall forecast for Mumbai next week. Consider increasing premium loading for standard tiers in that zone.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default AdminAnalytics;
