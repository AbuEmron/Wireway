/* eslint-disable react-hooks/exhaustive-deps */
// src/ComplianceView.jsx — Legally Load-Bearing  ·  Phase 2 · Feature 4
import { useState, useEffect, useCallback } from "react";
import {
  get1099Status, getSalesTax, getScheduleCReadiness,
  getRenewals, upsertRenewal, deleteRenewal, renewalDue,
} from "./lib/compliance";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");
const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const GREEN = "#7dcea0", GOLD = "#e8c97a", RED = "#e87e7e", BLUE = "#7eb8e8";
const CURRENT_YEAR = new Date().getFullYear();
const KINDS = ["license", "permit", "insurance", "registration", "other"];

export default function ComplianceView({ user, onClose }) {
  const [year, setYear] = useState(CURRENT_YEAR);
  const [s1099, setS1099] = useState(null);
  const [salesTax, setSalesTax] = useState(null);
  const [readiness, setReadiness] = useState(null);
  const [renewals, setRenewals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [form, setForm] = useState({ label: "", kind: "license", identifier: "", issuer: "", expires_on: "", reminder_days: 30, notes: "" });
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    const [a, b, c, d] = await Promise.all([
      get1099Status(user.id, year), getSalesTax(user.id, year),
      getScheduleCReadiness(user.id, year), getRenewals(user.id),
    ]);
    setS1099(a); setSalesTax(b); setReadiness(c); setRenewals(d);
    setLoading(false);
  }, [user?.id, year]);

  useEffect(() => { load(); }, [load]);

  const reset = () => { setForm({ label: "", kind: "license", identifier: "", issuer: "", expires_on: "", reminder_days: 30, notes: "" }); setEditing(null); };
  const save = async () => {
    if (!form.label.trim() || !form.expires_on) return flash("Add a name and expiry date.");
    const { error } = await upsertRenewal(user.id, { ...form, id: editing });
    if (error) return flash("Could not save.");
    setShowForm(false); reset(); await load(); flash("Saved.");
  };
  const startEdit = (r) => { setEditing(r.id); setForm({ label: r.label, kind: r.kind, identifier: r.identifier || "", issuer: r.issuer || "", expires_on: r.expires_on, reminder_days: r.reminder_days, notes: r.notes || "" }); setShowForm(true); };
  const remove = async (id) => { if (!window.confirm("Delete this renewal?")) return; await deleteRenewal(id, user.id); await load(); };

  // Build the alert feed locally from loaded data
  const alerts = [];
  if (s1099?.needCount > 0) alerts.push({ level: "high", text: `${s1099.needCount} sub${s1099.needCount !== 1 ? "s" : ""} over the ${year} ${fmt(s1099.threshold)} 1099 threshold${s1099.missingW9 ? ` · ${s1099.missingW9} missing W-9` : ""}.` });
  for (const r of renewals.filter(renewalDue)) alerts.push({ level: r.days_left < 0 ? "high" : "med", text: r.days_left < 0 ? `${r.label} EXPIRED ${Math.abs(r.days_left)}d ago.` : `${r.label} expires in ${r.days_left}d.` });
  if (salesTax?.collected > 0) alerts.push({ level: "med", text: `${fmt(salesTax.collected)} sales tax collected in ${year} — set aside to remit.` });

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 720, padding: "24px" };
  const YEARS = [CURRENT_YEAR + 1, CURRENT_YEAR, CURRENT_YEAR - 1];

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Compliance</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>1099s · Schedule C · sales tax · renewals</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))} style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}>
              {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
          </div>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "40px 0" }}>Loading…</div>
        ) : (
          <>
            {/* Alerts */}
            <div style={{ marginBottom: 18 }}>
              {alerts.length === 0 ? (
                <div style={{ fontSize: 12, color: GREEN, background: "rgba(125,206,160,0.07)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 10, padding: "12px 14px" }}>✓ Nothing needs attention right now.</div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                  {alerts.map((a, i) => (
                    <div key={i} style={{ fontSize: 12, color: a.level === "high" ? RED : GOLD, background: a.level === "high" ? "rgba(232,126,126,0.07)" : "rgba(232,201,122,0.06)", border: `1px solid ${a.level === "high" ? "rgba(232,126,126,0.22)" : "rgba(232,201,122,0.2)"}`, borderRadius: 9, padding: "10px 13px" }}>
                      {a.level === "high" ? "⚠ " : "• "}{a.text}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Sales tax + Schedule C readiness */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 18 }}>
              <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "14px" }}>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Sales tax — {year}</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 20, fontWeight: 600, color: GOLD }}>{fmt(salesTax?.collected)}</div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginTop: 4 }}>collected (to remit) · {fmt(salesTax?.invoiced)} billed</div>
              </div>
              <div style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 11, padding: "14px" }}>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Schedule C ready</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 20, fontWeight: 600, color: readiness?.score >= 75 ? GREEN : GOLD }}>{readiness?.score}%</div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginTop: 4 }}>{readiness?.passed}/{readiness?.total} checks pass</div>
              </div>
            </div>

            {/* Readiness checklist */}
            <div style={{ marginBottom: 18 }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Schedule C checklist</div>
              <div style={{ display: "flex", flexDirection: "column", gap: 5 }}>
                {readiness?.checks.map((c) => (
                  <div key={c.key} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 12px", background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.05)", borderRadius: 8 }}>
                    <span style={{ color: c.ok ? GREEN : "rgba(255,255,255,0.3)", fontSize: 14 }}>{c.ok ? "✓" : "○"}</span>
                    <span style={{ flex: 1, fontSize: 12, color: "#fff" }}>{c.label}</span>
                    <span style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>{c.detail}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Renewals */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em" }}>License &amp; permit renewals</div>
              <button onClick={() => { setShowForm((v) => !v); reset(); }} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(126,184,232,0.3)", background: "rgba(126,184,232,0.07)", color: BLUE, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ Add</button>
            </div>

            {showForm && (
              <div style={{ background: "rgba(126,184,232,0.04)", border: "1px solid rgba(126,184,232,0.14)", borderRadius: 10, padding: "12px", marginBottom: 12 }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 120px", gap: 7, marginBottom: 7 }}>
                  <input placeholder="Name (e.g. Master Electrician License) *" value={form.label} onChange={(e) => setForm((p) => ({ ...p, label: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                  <select value={form.kind} onChange={(e) => setForm((p) => ({ ...p, kind: e.target.value }))} style={{ ...IS, colorScheme: "dark", textTransform: "capitalize" }}>{KINDS.map((k) => <option key={k} value={k}>{k}</option>)}</select>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 7, marginBottom: 7 }}>
                  <input placeholder="Number / ID" value={form.identifier} onChange={(e) => setForm((p) => ({ ...p, identifier: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                  <input placeholder="Issuer (state board, city…)" value={form.issuer} onChange={(e) => setForm((p) => ({ ...p, issuer: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 140px", gap: 7, marginBottom: 10 }}>
                  <div><div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 3 }}>Expires</div><input type="date" value={form.expires_on} onChange={(e) => setForm((p) => ({ ...p, expires_on: e.target.value }))} style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} /></div>
                  <div><div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 3 }}>Remind (days before)</div><input type="number" min="1" value={form.reminder_days} onChange={(e) => setForm((p) => ({ ...p, reminder_days: e.target.value }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} /></div>
                </div>
                <div style={{ display: "flex", gap: 7 }}>
                  <button onClick={save} style={{ flex: 1, padding: "9px", borderRadius: 8, border: "1px solid rgba(126,184,232,0.35)", background: "rgba(126,184,232,0.12)", color: BLUE, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>{editing ? "Update" : "Add renewal"}</button>
                  <button onClick={() => { setShowForm(false); reset(); }} style={{ padding: "9px 14px", borderRadius: 8, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.45)", fontSize: 12, cursor: "pointer", fontFamily: "inherit" }}>Cancel</button>
                </div>
              </div>
            )}

            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {renewals.length === 0 && !showForm && <div style={{ textAlign: "center", fontSize: 12, color: "rgba(255,255,255,0.25)", padding: "20px 0" }}>No renewals tracked. Add your license so it never lapses.</div>}
              {renewals.map((r) => {
                const due = renewalDue(r);
                const color = r.days_left < 0 ? RED : due ? GOLD : "rgba(255,255,255,0.5)";
                return (
                  <div key={r.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 13px", background: "rgba(255,255,255,0.022)", border: `1px solid ${due ? "rgba(232,201,122,0.25)" : "rgba(255,255,255,0.06)"}`, borderRadius: 10 }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, color: "#fff", fontWeight: 600 }}>{r.label} <span style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase" }}>{r.kind}</span></div>
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace" }}>{[r.identifier, r.issuer].filter(Boolean).join(" · ") || "—"} · exp {r.expires_on}</div>
                    </div>
                    <div style={{ textAlign: "right", flexShrink: 0 }}>
                      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 700, color }}>{r.days_left < 0 ? `${Math.abs(r.days_left)}d ago` : `${r.days_left}d`}</div>
                      <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>{r.days_left < 0 ? "expired" : "left"}</div>
                    </div>
                    <button onClick={() => startEdit(r)} style={{ flexShrink: 0, width: 26, height: 26, borderRadius: 6, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.4)", fontSize: 11, cursor: "pointer" }}>✎</button>
                    <button onClick={() => remove(r.id)} style={{ flexShrink: 0, width: 26, height: 26, borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer" }}>✕</button>
                  </div>
                );
              })}
            </div>

            <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
              Nudges are built from your own data — verify thresholds and filings with your accountant.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
