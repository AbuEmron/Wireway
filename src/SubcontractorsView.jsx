/* eslint-disable react-hooks/exhaustive-deps */
// src/SubcontractorsView.jsx — Subcontractor ledger + 1099 tracking  ·  Feature 3
import { useState, useEffect, useCallback } from "react";
import {
  getSubLedger, upsertSubcontractor, deleteSubcontractor,
  addSubPayment, deleteSubPayment, build1099Text, PAY_METHODS, thresholdForYear,
} from "./lib/subs";
import { getRecentJobs } from "./lib/receipts";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");
const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("en-US");
const fmt2 = (n) => "$" + (Number(n) || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const TEAL = "#7ee8b8", GOLD = "#e8c97a", RED = "#e87e7e", GREEN = "#7dcea0";
const CURRENT_YEAR = new Date().getFullYear();
const TODAY = new Date().toISOString().split("T")[0];

// ── 1099 worksheet modal ──────────────────────────────────────────────────────
function Form1099Modal({ sub, year, company, threshold, onClose }) {
  const [copied, setCopied] = useState(false);
  const text = build1099Text({ sub, year, company, threshold });
  const copy = () => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2500); };
  const download = () => {
    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = `1099-NEC-${sub.name.replace(/\s+/g, "_")}-${year}.txt`; a.click();
    URL.revokeObjectURL(url);
  };
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 420, background: "rgba(0,0,0,0.85)", backdropFilter: "blur(8px)", display: "flex", alignItems: "center", justifyContent: "center", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 16, width: "100%", maxWidth: 560, padding: "24px", maxHeight: "90vh", display: "flex", flexDirection: "column" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16, flexShrink: 0 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 15, fontWeight: 800, color: "#fff" }}>1099-NEC Worksheet — {year}</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>{sub.name} · Box 1 {fmt2(sub.total)}</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 20, cursor: "pointer" }}>✕</button>
        </div>
        <pre style={{ flex: 1, overflowY: "auto", fontFamily: "'DM Mono',monospace", fontSize: 11, color: "rgba(255,255,255,0.75)", background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "14px 16px", lineHeight: 1.7, marginBottom: 14, whiteSpace: "pre-wrap" }}>{text}</pre>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, flexShrink: 0 }}>
          <button onClick={copy} style={{ padding: "11px", background: copied ? "rgba(125,206,160,0.1)" : "linear-gradient(135deg,rgba(126,232,184,0.18),rgba(126,232,184,0.07))", border: `1px solid ${copied ? "rgba(125,206,160,0.35)" : "rgba(126,232,184,0.35)"}`, borderRadius: 9, color: copied ? GREEN : TEAL, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>{copied ? "✓ Copied!" : "Copy"}</button>
          <button onClick={download} style={{ padding: "11px", background: "rgba(126,184,232,0.08)", border: "1px solid rgba(126,184,232,0.3)", borderRadius: 9, color: "#7eb8e8", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Download .txt</button>
        </div>
      </div>
    </div>
  );
}

export default function SubcontractorsView({ user, company = {}, onClose }) {
  const [year,    setYear]    = useState(CURRENT_YEAR);
  const [ledger,  setLedger]  = useState([]);
  const [meta,    setMeta]    = useState({ threshold: thresholdForYear(CURRENT_YEAR), totalPaid: 0, needCount: 0 });
  const [jobs,    setJobs]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg,     setMsg]     = useState("");
  const [show1099, setShow1099] = useState(null);
  const [expanded, setExpanded] = useState(null);
  const [showSubForm, setShowSubForm] = useState(false);
  const [editingSub,  setEditingSub]  = useState(null);

  const [subForm, setSubForm] = useState({ name: "", business_name: "", email: "", phone: "", address: "", tax_id: "", tax_id_type: "ein", w9_received: false });
  const [pay, setPay] = useState({ subcontractor_id: "", amount: "", payment_date: TODAY, method: "check", job_id: "", memo: "" });

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    const [res, recentJobs] = await Promise.all([getSubLedger(user.id, year), getRecentJobs(user.id, 40)]);
    setLedger(res.ledger);
    setMeta({ threshold: res.threshold, totalPaid: res.totalPaid, needCount: res.needCount });
    setJobs(recentJobs);
    setLoading(false);
  }, [user?.id, year]);

  useEffect(() => { load(); }, [load]);

  const resetSubForm = () => { setSubForm({ name: "", business_name: "", email: "", phone: "", address: "", tax_id: "", tax_id_type: "ein", w9_received: false }); setEditingSub(null); };

  const saveSub = async () => {
    if (!subForm.name.trim()) return flash("Enter a name.");
    const { error } = await upsertSubcontractor(user.id, { ...subForm, id: editingSub });
    if (error) return flash("Could not save.");
    setShowSubForm(false); resetSubForm(); await load(); flash("Subcontractor saved.");
  };

  const removeSub = async (id) => {
    if (!window.confirm("Delete this subcontractor and all their payment records?")) return;
    await deleteSubcontractor(id, user.id); await load();
  };

  const savePayment = async () => {
    if (!pay.subcontractor_id) return flash("Pick a subcontractor.");
    const amt = parseFloat(pay.amount);
    if (!amt || amt <= 0) return flash("Enter an amount.");
    const { error } = await addSubPayment(user.id, { ...pay, amount: amt });
    if (error) return flash("Could not save payment.");
    setPay((p) => ({ ...p, amount: "", memo: "" }));
    await load(); flash("Payment logged.");
  };

  const removePayment = async (id) => { await deleteSubPayment(id, user.id); await load(); };

  const startEdit = (s) => { setEditingSub(s.id); setSubForm({ name: s.name || "", business_name: s.business_name || "", email: s.email || "", phone: s.phone || "", address: s.address || "", tax_id: s.tax_id || "", tax_id_type: s.tax_id_type || "ein", w9_received: !!s.w9_received }); setShowSubForm(true); };

  const wrap = { position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 760, padding: "24px" };
  const YEARS = [CURRENT_YEAR + 1, CURRENT_YEAR, CURRENT_YEAR - 1, CURRENT_YEAR - 2];

  return (
    <div style={wrap}>
      <div style={panel}>
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Subcontractors &amp; 1099s</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Per-sub ledger · {year} threshold {fmt(meta.threshold)}</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))} style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}>
              {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
          </div>
        </div>

        {msg && <div style={{ marginBottom: 12, fontSize: 11, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 7, padding: "7px 11px" }}>{msg}</div>}

        {/* Summary */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(150px,1fr))", gap: 8, marginBottom: 16 }}>
          {[
            { label: `Paid to subs — ${year}`, val: fmt(meta.totalPaid), color: TEAL },
            { label: "Subcontractors", val: ledger.length, color: "#fff" },
            { label: "Need a 1099", val: meta.needCount, color: meta.needCount > 0 ? GOLD : "rgba(255,255,255,0.5)" },
          ].map((c) => (
            <div key={c.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "11px 13px" }}>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 5 }}>{c.label}</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 18, fontWeight: 600, color: c.color }}>{loading ? "…" : c.val}</div>
            </div>
          ))}
        </div>

        {/* Log a payment */}
        <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 12, padding: "14px", marginBottom: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em" }}>Log a payment</div>
            <button onClick={() => { setShowSubForm((v) => !v); resetSubForm(); }} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(126,232,184,0.3)", background: "rgba(126,232,184,0.07)", color: TEAL, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ New subcontractor</button>
          </div>

          {showSubForm && (
            <div style={{ background: "rgba(126,232,184,0.04)", border: "1px solid rgba(126,232,184,0.14)", borderRadius: 10, padding: "12px", marginBottom: 10 }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 7, marginBottom: 7 }}>
                <input placeholder="Name *" value={subForm.name} onChange={(e) => setSubForm((p) => ({ ...p, name: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                <input placeholder="Business name" value={subForm.business_name} onChange={(e) => setSubForm((p) => ({ ...p, business_name: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                <input placeholder="Email" value={subForm.email} onChange={(e) => setSubForm((p) => ({ ...p, email: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                <input placeholder="Phone" value={subForm.phone} onChange={(e) => setSubForm((p) => ({ ...p, phone: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <input placeholder="Address (for 1099)" value={subForm.address} onChange={(e) => setSubForm((p) => ({ ...p, address: e.target.value }))} style={{ ...IS, marginBottom: 7 }} onFocus={focusGold} onBlur={blurGray} />
              <div className="ww-form-row" style={{ display: "grid", gridTemplateColumns: "1fr 110px", gap: 7, marginBottom: 7 }}>
                <input placeholder="Tax ID (EIN/SSN)" value={subForm.tax_id} onChange={(e) => setSubForm((p) => ({ ...p, tax_id: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                <select value={subForm.tax_id_type} onChange={(e) => setSubForm((p) => ({ ...p, tax_id_type: e.target.value }))} style={{ ...IS, colorScheme: "dark" }}>
                  <option value="ein">EIN</option><option value="ssn">SSN</option>
                </select>
              </div>
              <label style={{ display: "flex", alignItems: "center", gap: 7, fontSize: 11, color: "rgba(255,255,255,0.55)", marginBottom: 10, cursor: "pointer" }}>
                <input type="checkbox" checked={subForm.w9_received} onChange={(e) => setSubForm((p) => ({ ...p, w9_received: e.target.checked }))} /> W-9 on file
              </label>
              <div style={{ display: "flex", gap: 7 }}>
                <button onClick={saveSub} style={{ flex: 1, padding: "9px", borderRadius: 8, border: "1px solid rgba(126,232,184,0.35)", background: "rgba(126,232,184,0.1)", color: TEAL, fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>{editingSub ? "Update" : "Add subcontractor"}</button>
                <button onClick={() => { setShowSubForm(false); resetSubForm(); }} style={{ padding: "9px 14px", borderRadius: 8, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.45)", fontSize: 12, cursor: "pointer", fontFamily: "inherit" }}>Cancel</button>
              </div>
            </div>
          )}

          <div className="ww-form-row" style={{ display: "grid", gridTemplateColumns: "1fr 110px 130px", gap: 7, marginBottom: 7 }}>
            <select value={pay.subcontractor_id} onChange={(e) => setPay((p) => ({ ...p, subcontractor_id: e.target.value }))} style={{ ...IS, colorScheme: "dark" }}>
              <option value="">Subcontractor…</option>
              {ledger.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
            <input type="number" min="0.01" step="0.01" placeholder="Amount" value={pay.amount} onChange={(e) => setPay((p) => ({ ...p, amount: e.target.value }))} style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
            <input type="date" value={pay.payment_date} onChange={(e) => setPay((p) => ({ ...p, payment_date: e.target.value }))} style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
          </div>
          <div className="ww-form-row" style={{ display: "grid", gridTemplateColumns: "110px 1fr 1fr", gap: 7, marginBottom: 10 }}>
            <select value={pay.method} onChange={(e) => setPay((p) => ({ ...p, method: e.target.value }))} style={{ ...IS, colorScheme: "dark", textTransform: "capitalize" }}>
              {PAY_METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
            <select value={pay.job_id} onChange={(e) => setPay((p) => ({ ...p, job_id: e.target.value }))} style={{ ...IS, colorScheme: "dark" }}>
              <option value="">Job (optional)…</option>
              {jobs.map((j) => <option key={j.id} value={j.id}>{j.title}{j.client_name ? ` · ${j.client_name}` : ""}</option>)}
            </select>
            <input placeholder="Memo" value={pay.memo} onChange={(e) => setPay((p) => ({ ...p, memo: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
          </div>
          <button onClick={savePayment} style={{ width: "100%", padding: "10px", borderRadius: 9, background: "linear-gradient(135deg,rgba(126,232,184,0.18),rgba(126,232,184,0.07))", border: "1px solid rgba(126,232,184,0.35)", color: TEAL, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ Log payment</button>
        </div>

        {/* Ledger */}
        {loading ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "32px 0" }}>Loading…</div>
        ) : ledger.length === 0 ? (
          <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", padding: "36px 0" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>👷</div>
            <div style={{ fontSize: 13 }}>No subcontractors yet</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.18)" }}>Add one above, then log payments through the year.</div>
          </div>
        ) : (
          ledger.map((s) => {
            const pct = Math.min(100, meta.threshold > 0 ? (s.total / meta.threshold) * 100 : 0);
            const open = expanded === s.id;
            return (
              <div key={s.id} style={{ background: "rgba(255,255,255,0.022)", border: `1px solid ${s.over_threshold ? "rgba(232,201,122,0.25)" : "rgba(255,255,255,0.07)"}`, borderRadius: 12, padding: "13px 15px", marginBottom: 8 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 10 }}>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                      <span style={{ fontSize: 13, fontWeight: 700, color: "#fff" }}>{s.name}</span>
                      {s.over_threshold && <span style={{ fontSize: 9, fontWeight: 800, color: GOLD, background: "rgba(232,201,122,0.12)", border: "1px solid rgba(232,201,122,0.3)", borderRadius: 4, padding: "1px 6px" }}>1099 NEEDED</span>}
                      {s.needs_w9 && <span style={{ fontSize: 9, fontWeight: 800, color: RED, background: "rgba(232,126,126,0.1)", border: "1px solid rgba(232,126,126,0.3)", borderRadius: 4, padding: "1px 6px" }}>NO W-9</span>}
                    </div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", marginTop: 2, fontFamily: "'DM Mono',monospace" }}>
                      {s.count} payment{s.count !== 1 ? "s" : ""}{s.business_name ? ` · ${s.business_name}` : ""}
                    </div>
                  </div>
                  <div style={{ textAlign: "right", flexShrink: 0 }}>
                    <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 16, fontWeight: 600, color: TEAL }}>{fmt2(s.total)}</div>
                    {!s.over_threshold && <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>{fmt(s.remaining)} to threshold</div>}
                  </div>
                </div>

                {/* threshold progress */}
                <div style={{ height: 5, background: "rgba(255,255,255,0.06)", borderRadius: 3, overflow: "hidden", marginTop: 9 }}>
                  <div style={{ width: `${pct}%`, height: "100%", background: s.over_threshold ? GOLD : TEAL, opacity: 0.6 }} />
                </div>

                <div style={{ display: "flex", gap: 6, marginTop: 10, flexWrap: "wrap" }}>
                  <button onClick={() => setExpanded(open ? null : s.id)} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)", background: "transparent", color: "rgba(255,255,255,0.45)", fontSize: 10, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>{open ? "Hide payments" : "Payments"}</button>
                  <button onClick={() => startEdit(s)} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)", background: "transparent", color: "rgba(255,255,255,0.45)", fontSize: 10, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Edit</button>
                  <button onClick={() => setShow1099(s)} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(232,201,122,0.35)", background: "rgba(232,201,122,0.08)", color: GOLD, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Generate 1099</button>
                  <button onClick={() => removeSub(s.id)} style={{ marginLeft: "auto", padding: "4px 9px", borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.55)", fontSize: 10, cursor: "pointer", fontFamily: "inherit" }}>Delete</button>
                </div>

                {open && (
                  <div style={{ marginTop: 10, display: "flex", flexDirection: "column", gap: 4 }}>
                    {s.payments.length === 0 ? (
                      <div style={{ fontSize: 11, color: "rgba(255,255,255,0.25)" }}>No payments in {year}.</div>
                    ) : s.payments.slice().sort((a, b) => (b.payment_date || "").localeCompare(a.payment_date || "")).map((p) => (
                      <div key={p.id} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 9px", background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.05)", borderRadius: 7 }}>
                        <span style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "'DM Mono',monospace", minWidth: 72 }}>{p.payment_date}</span>
                        <span style={{ fontSize: 11, color: "rgba(255,255,255,0.55)", flex: 1, minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.method || ""}{p.memo ? ` · ${p.memo}` : ""}</span>
                        <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, fontWeight: 600, color: "#fff" }}>{fmt2(p.amount)}</span>
                        <button onClick={() => removePayment(p.id)} style={{ width: 22, height: 22, borderRadius: 5, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 10, cursor: "pointer" }}>✕</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })
        )}

        <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
          {year} 1099-NEC threshold: {fmt(meta.threshold)} · Collect a W-9 before paying · File official forms via your accountant
        </div>
      </div>

      {show1099 && <Form1099Modal sub={show1099} year={year} company={company} threshold={meta.threshold} onClose={() => setShow1099(null)} />}
    </div>
  );
}
