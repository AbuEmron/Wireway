// src/ExpensesView.jsx — business expense tracker + CSV import + Schedule C tax export
import { useState, useEffect, useRef } from "react";
import {
  getExpenses, addExpense, deleteExpense, parseExpenseCsv,
  getTrips, getPlaidTransactions, buildScheduleCText,
  EXPENSE_CATEGORIES, categoryById, IRS_RATES,
} from "./lib/financeApi";

const IS = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.07)",
  borderRadius: 7,
  padding: "8px 11px",
  fontSize: 13,
  color: "#fff",
  fontFamily: "inherit",
  width: "100%",
  outline: "none",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.07)");

const TODAY = new Date().toISOString().split("T")[0];
const CURRENT_YEAR = new Date().getFullYear();

const fmt = (n) =>
  "$" + Number(n).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// ── CSV IMPORT MODAL ──────────────────────────────────────────────────────────
function CsvImportModal({ onImport, onClose }) {
  const [text, setText] = useState("");
  const [preview, setPreview] = useState([]);
  const [error, setError]   = useState("");
  const fileRef = useRef(null);

  const parse = (raw) => {
    const rows = parseExpenseCsv(raw);
    if (!rows.length) {
      setError("No valid rows found. Check that columns include: date, amount, category, vendor, description.");
    } else {
      setError("");
      setPreview(rows.slice(0, 5));
    }
    return rows;
  };

  const onFile = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => { setText(ev.target.result); parse(ev.target.result); };
    reader.readAsText(file);
  };

  const onPaste = (e) => { const raw = e.target.value; setText(raw); parse(raw); };

  const handleImport = () => {
    const rows = parseExpenseCsv(text);
    if (!rows.length) { setError("Nothing to import."); return; }
    onImport(rows);
  };

  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 300, background: "rgba(0,0,0,0.85)", backdropFilter: "blur(8px)", display: "flex", alignItems: "center", justifyContent: "center", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 16, width: "100%", maxWidth: 520, padding: "24px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
          <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 15, fontWeight: 800, color: "#fff" }}>Import CSV</div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 20, cursor: "pointer" }}>✕</button>
        </div>

        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", marginBottom: 12, lineHeight: 1.7 }}>
          Export transactions from your bank/card as CSV, then import here.
          Required columns: <span style={{ color: "#e8c97a", fontFamily: "'DM Mono',monospace" }}>date, amount, category, vendor, description</span> (case-insensitive, partial match OK).
        </div>

        <button onClick={() => fileRef.current?.click()}
          style={{ width: "100%", padding: "11px", background: "rgba(126,184,232,0.08)", border: "1px dashed rgba(126,184,232,0.3)", borderRadius: 9, color: "#7eb8e8", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", marginBottom: 10 }}>
          Choose CSV file
        </button>
        <input ref={fileRef} type="file" accept=".csv,text/csv" style={{ display: "none" }} onChange={onFile} />

        <textarea placeholder="…or paste CSV text here" value={text} onChange={onPaste}
          rows={5} style={{ ...IS, resize: "vertical", fontSize: 11, fontFamily: "'DM Mono',monospace", marginBottom: 10 }} />

        {error && <div style={{ fontSize: 11, color: "#e87e7e", background: "rgba(232,126,126,0.08)", border: "1px solid rgba(232,126,126,0.2)", borderRadius: 7, padding: "7px 10px", marginBottom: 10 }}>{error}</div>}

        {preview.length > 0 && (
          <div style={{ marginBottom: 14 }}>
            <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 6 }}>Preview (first 5 rows)</div>
            {preview.map((r, i) => (
              <div key={i} style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "rgba(255,255,255,0.55)", padding: "4px 0", borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
                <span>{r.expense_date} · {r.vendor || r.description || "—"}</span>
                <span style={{ fontFamily: "'DM Mono',monospace", color: "#e8c97a" }}>{fmt(r.amount)}</span>
              </div>
            ))}
          </div>
        )}

        <button onClick={handleImport} disabled={!text.trim()}
          style={{ width: "100%", padding: "11px", background: text.trim() ? "linear-gradient(135deg,rgba(100,220,130,0.2),rgba(100,220,130,0.08))" : "rgba(255,255,255,0.03)", border: "1px solid rgba(100,220,130,0.35)", borderRadius: 9, color: text.trim() ? "#7dcea0" : "rgba(255,255,255,0.2)", fontSize: 13, fontWeight: 700, cursor: text.trim() ? "pointer" : "default", fontFamily: "inherit" }}>
          Import All Rows
        </button>
      </div>
    </div>
  );
}

// ── TAX EXPORT MODAL ──────────────────────────────────────────────────────────
function TaxExportModal({ year, expenses, onClose, userId }) {
  const [trips,      setTrips]      = useState([]);
  const [plaidTxns,  setPlaidTxns]  = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [copied,     setCopied]     = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getTrips(userId, year),
      getPlaidTransactions(userId, year),
    ]).then(([tripsRes, plaidRes]) => {
      setTrips(tripsRes.data);
      setPlaidTxns(plaidRes.data);
      setLoading(false);
    });
  }, [userId, year]);

  const text = loading ? "Loading data..." : buildScheduleCText({ year, trips, expenses, plaidTxns });

  const copy = () => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const download = () => {
    const blob = new Blob([text], { type: "text/plain" });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement("a");
    a.href     = url;
    a.download = `wireway-schedule-c-${year}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 300, background: "rgba(0,0,0,0.85)", backdropFilter: "blur(8px)", display: "flex", alignItems: "center", justifyContent: "center", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 16, width: "100%", maxWidth: 580, padding: "24px", maxHeight: "90vh", display: "flex", flexDirection: "column" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16, flexShrink: 0 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 15, fontWeight: 800, color: "#fff" }}>Schedule C Summary — {year}</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Copy or download for your accountant / tax filing</div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 20, cursor: "pointer" }}>✕</button>
        </div>

        <pre style={{ flex: 1, overflowY: "auto", fontFamily: "'DM Mono',monospace", fontSize: 11, color: "rgba(255,255,255,0.75)", background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "14px 16px", lineHeight: 1.7, marginBottom: 14, whiteSpace: "pre-wrap" }}>
          {text}
        </pre>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, flexShrink: 0 }}>
          <button onClick={copy}
            style={{ padding: "11px", background: copied ? "rgba(100,220,130,0.1)" : "linear-gradient(135deg,rgba(232,201,122,0.18),rgba(232,201,122,0.07))", border: `1px solid ${copied ? "rgba(100,220,130,0.35)" : "rgba(232,201,122,0.35)"}`, borderRadius: 9, color: copied ? "#7dcea0" : "#e8c97a", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
            {copied ? "✓ Copied!" : "Copy Text"}
          </button>
          <button onClick={download}
            style={{ padding: "11px", background: "rgba(126,184,232,0.08)", border: "1px solid rgba(126,184,232,0.3)", borderRadius: 9, color: "#7eb8e8", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
            Download .txt
          </button>
        </div>
      </div>
    </div>
  );
}

// ── MAIN EXPENSES VIEW ────────────────────────────────────────────────────────
export default function ExpensesView({ user, onClose, onOpenPlaid }) {
  const [year,       setYear]       = useState(CURRENT_YEAR);
  const [expenses,   setExpenses]   = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [saving,     setSaving]     = useState(false);
  const [msg,        setMsg]        = useState("");
  const [filterCat,  setFilterCat]  = useState("all");
  const [showImport, setShowImport] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [importing,  setImporting]  = useState(false);

  const [form, setForm] = useState({
    expense_date: TODAY,
    amount:       "",
    category:     "materials",
    vendor:       "",
    description:  "",
  });

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  useEffect(() => {
    if (!user?.id) return;
    setLoading(true);
    getExpenses(user.id, year).then(({ data }) => {
      setExpenses(data);
      setLoading(false);
    });
  }, [user?.id, year]);

  const handleAdd = async () => {
    const amount = parseFloat(form.amount);
    if (!amount || amount <= 0 || !form.expense_date) {
      flash("Fill in date and amount.");
      return;
    }
    setSaving(true);
    const { data, error } = await addExpense(user.id, {
      expense_date: form.expense_date,
      amount,
      category:    form.category,
      vendor:      form.vendor.trim()      || null,
      description: form.description.trim() || null,
    });
    setSaving(false);
    if (error) { flash("Error saving expense."); return; }
    setExpenses((prev) => [data, ...prev]);
    setForm({ expense_date: TODAY, amount: "", category: form.category, vendor: "", description: "" });
    flash("Expense saved!");
  };

  const handleDelete = async (id) => {
    setExpenses((prev) => prev.filter((e) => e.id !== id));
    await deleteExpense(id, user.id);
  };

  const handleImport = async (rows) => {
    setShowImport(false);
    setImporting(true);
    const saved = [];
    for (const row of rows) {
      const { data } = await addExpense(user.id, row);
      if (data) saved.push(data);
    }
    setExpenses((prev) => [...saved, ...prev]);
    setImporting(false);
    flash(`${saved.length} expense${saved.length !== 1 ? "s" : ""} imported!`);
  };

  // Totals
  const totalAll = expenses.reduce((s, e) => s + Number(e.amount), 0);
  const byCategory = {};
  for (const e of expenses) {
    byCategory[e.category] = (byCategory[e.category] || 0) + Number(e.amount);
  }

  const filtered = filterCat === "all" ? expenses : expenses.filter((e) => e.category === filterCat);

  const availableYears = Object.keys(IRS_RATES).map(Number).sort((a, b) => b - a);
  const activeCats = EXPENSE_CATEGORIES.filter((c) => byCategory[c.id] > 0);

  return (
    <>
      <div style={{ position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.8)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" }}>
        <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 720, padding: "24px" }}>

          {/* Header */}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 }}>
            <div>
              <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Business Expenses</div>
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Track spending · Schedule C deductions</div>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <select value={year} onChange={(e) => setYear(Number(e.target.value))}
                style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}>
                {availableYears.map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
              <button onClick={() => setShowExport(true)}
                style={{ padding: "5px 12px", borderRadius: 7, border: "1px solid rgba(232,201,122,0.35)", background: "rgba(232,201,122,0.08)", color: "#e8c97a", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", whiteSpace: "nowrap" }}>
                Tax Export
              </button>
              <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
            </div>
          </div>

          {/* Total summary */}
          <div style={{ background: "linear-gradient(135deg,rgba(168,232,126,0.07),rgba(255,255,255,0.02))", border: "1px solid rgba(168,232,126,0.18)", borderRadius: 12, padding: "14px 16px", marginBottom: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 2 }}>Total Expenses — {year}</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 24, fontWeight: 600, color: "#a8e87e", letterSpacing: "-0.02em" }}>
                  {loading ? "..." : fmt(totalAll)}
                </div>
              </div>
              <div style={{ display: "flex", gap: 6 }}>
                {onOpenPlaid && (
                  <button onClick={onOpenPlaid}
                    style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid rgba(126,184,232,0.3)", background: "rgba(126,184,232,0.07)", color: "#7eb8e8", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", whiteSpace: "nowrap" }}>
                    Auto Import
                  </button>
                )}
                <button onClick={() => setShowImport(true)} disabled={importing}
                  style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid rgba(255,255,255,0.12)", background: "rgba(255,255,255,0.04)", color: importing ? "rgba(255,255,255,0.2)" : "rgba(255,255,255,0.5)", fontSize: 11, fontWeight: 700, cursor: importing ? "default" : "pointer", fontFamily: "inherit" }}>
                  {importing ? "Importing..." : "Import CSV"}
                </button>
              </div>
            </div>

            {/* Category bar */}
            {activeCats.length > 0 && (
              <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginTop: 12 }}>
                {activeCats.map((cat) => (
                  <div key={cat.id} style={{ fontSize: 10, color: cat.color, background: `${cat.color}14`, border: `1px solid ${cat.color}30`, borderRadius: 5, padding: "2px 7px", fontFamily: "'DM Mono',monospace" }}>
                    {cat.label}: {fmt(byCategory[cat.id])}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Add expense form */}
          <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 12, padding: "16px", marginBottom: 16 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12 }}>Add Expense</div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 8 }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Date</div>
                <input type="date" value={form.expense_date}
                  onChange={(e) => setForm((p) => ({ ...p, expense_date: e.target.value }))}
                  style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Amount ($)</div>
                <input type="number" min="0.01" step="0.01" placeholder="0.00" value={form.amount}
                  onChange={(e) => setForm((p) => ({ ...p, amount: e.target.value }))}
                  style={IS} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Category</div>
                <select value={form.category}
                  onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))}
                  style={{ ...IS, colorScheme: "dark" }}>
                  {EXPENSE_CATEGORIES.map((c) => (
                    <option key={c.id} value={c.id}>{c.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 12 }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Vendor (optional)</div>
                <input placeholder="Home Depot, Grainger, etc." value={form.vendor}
                  onChange={(e) => setForm((p) => ({ ...p, vendor: e.target.value }))}
                  style={IS} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Description (optional)</div>
                <input placeholder="Wire, conduit, breakers..." value={form.description}
                  onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
                  style={IS} onFocus={focusGold} onBlur={blurGray} />
              </div>
            </div>

            {msg && (
              <div style={{ fontSize: 11, color: msg.includes("Error") ? "#e87e7e" : "#7dcea0", background: msg.includes("Error") ? "rgba(232,126,126,0.08)" : "rgba(100,220,130,0.08)", border: `1px solid ${msg.includes("Error") ? "rgba(232,126,126,0.2)" : "rgba(100,220,130,0.2)"}`, borderRadius: 7, padding: "7px 10px", marginBottom: 10 }}>
                {msg}
              </div>
            )}

            <button onClick={handleAdd} disabled={saving}
              style={{ width: "100%", padding: "11px", background: saving ? "rgba(255,255,255,0.03)" : "linear-gradient(135deg,rgba(168,232,126,0.18),rgba(168,232,126,0.07))", border: "1px solid rgba(168,232,126,0.35)", borderRadius: 9, color: saving ? "rgba(168,232,126,0.4)" : "#a8e87e", fontSize: 13, fontWeight: 700, cursor: saving ? "default" : "pointer", fontFamily: "inherit", transition: "all 0.2s" }}>
              {saving ? "Saving..." : "+ Add Expense"}
            </button>
          </div>

          {/* Category filter */}
          {activeCats.length > 1 && (
            <div style={{ display: "flex", gap: 5, flexWrap: "wrap", marginBottom: 12 }}>
              <button onClick={() => setFilterCat("all")}
                style={{ padding: "4px 10px", borderRadius: 6, fontSize: 10, fontWeight: 700, border: filterCat === "all" ? "1px solid rgba(255,255,255,0.3)" : "1px solid rgba(255,255,255,0.07)", background: filterCat === "all" ? "rgba(255,255,255,0.1)" : "transparent", color: filterCat === "all" ? "#fff" : "rgba(255,255,255,0.35)", cursor: "pointer", fontFamily: "inherit" }}>
                All
              </button>
              {activeCats.map((cat) => (
                <button key={cat.id} onClick={() => setFilterCat(cat.id)}
                  style={{ padding: "4px 10px", borderRadius: 6, fontSize: 10, fontWeight: 700, border: filterCat === cat.id ? `1px solid ${cat.color}50` : "1px solid rgba(255,255,255,0.07)", background: filterCat === cat.id ? `${cat.color}14` : "transparent", color: filterCat === cat.id ? cat.color : "rgba(255,255,255,0.35)", cursor: "pointer", fontFamily: "inherit" }}>
                  {cat.label}
                </button>
              ))}
            </div>
          )}

          {/* Expense list */}
          {loading ? (
            <div style={{ textAlign: "center", padding: "32px", color: "rgba(255,255,255,0.25)", fontSize: 13 }}>Loading...</div>
          ) : filtered.length === 0 ? (
            <div style={{ textAlign: "center", padding: "32px", color: "rgba(255,255,255,0.2)" }}>
              <div style={{ fontSize: 28, marginBottom: 8 }}>🧾</div>
              <div style={{ fontSize: 13 }}>{expenses.length === 0 ? `No expenses logged for ${year}` : "No expenses in this category"}</div>
              <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.15)" }}>{expenses.length === 0 && "Add expenses above or import from a CSV."}</div>
            </div>
          ) : (
            <div>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>
                {filtered.length} expense{filtered.length !== 1 ? "s" : ""} · {fmt(filtered.reduce((s, e) => s + Number(e.amount), 0))}
              </div>
              {filtered.map((expense) => {
                const cat = categoryById(expense.category);
                return (
                  <div key={expense.id} style={{ display: "flex", alignItems: "flex-start", gap: 10, padding: "11px 14px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.055)", borderRadius: 10, marginBottom: 6 }}>
                    <div style={{ width: 4, height: 36, background: cat.color, borderRadius: 2, flexShrink: 0, marginTop: 2 }} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 2 }}>
                        <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: "rgba(255,255,255,0.35)" }}>{expense.expense_date}</span>
                        <span style={{ fontSize: 9, color: cat.color, background: `${cat.color}14`, border: `1px solid ${cat.color}30`, borderRadius: 4, padding: "1px 5px" }}>{cat.label}</span>
                      </div>
                      <div style={{ fontSize: 12, color: "#fff", fontWeight: 600, marginBottom: expense.description ? 1 : 0 }}>
                        {expense.vendor || expense.description || cat.label}
                      </div>
                      {expense.description && expense.vendor && (
                        <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)" }}>{expense.description}</div>
                      )}
                      <div style={{ fontSize: 9, color: "rgba(255,255,255,0.25)", marginTop: 1, fontFamily: "'DM Mono',monospace" }}>{cat.scheduleC}</div>
                    </div>
                    <div style={{ textAlign: "right", flexShrink: 0 }}>
                      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 700, color: "#a8e87e" }}>
                        {fmt(expense.amount)}
                      </div>
                      {expense.category === "meals" && (
                        <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)" }}>{fmt(expense.amount * 0.5)} ded.</div>
                      )}
                    </div>
                    <button onClick={() => handleDelete(expense.id)}
                      style={{ padding: "4px 8px", borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer", flexShrink: 0, alignSelf: "center" }}>
                      ✕
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
            Keep all receipts as supporting documentation · Meals are 50% deductible
          </div>
        </div>
      </div>

      {showImport && <CsvImportModal onImport={handleImport} onClose={() => setShowImport(false)} />}
      {showExport && <TaxExportModal year={year} expenses={expenses} userId={user.id} onClose={() => setShowExport(false)} />}
    </>
  );
}
