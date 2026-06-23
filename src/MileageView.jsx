// src/MileageView.jsx — mileage log with IRS standard rate deduction
import { useState, useEffect, useRef } from "react";
import { getTrips, addTrip, deleteTrip, irsRate, IRS_RATES } from "./lib/financeApi";

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

export default function MileageView({ user, onClose }) {
  const [year,    setYear]    = useState(CURRENT_YEAR);
  const [trips,   setTrips]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving,  setSaving]  = useState(false);
  const [msg,     setMsg]     = useState("");

  const [form, setForm] = useState({
    trip_date: TODAY,
    miles:     "",
    purpose:   "",
    start_loc: "",
    end_loc:   "",
    notes:     "",
  });

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2500); };

  useEffect(() => {
    if (!user?.id) return;
    setLoading(true);
    getTrips(user.id, year).then(({ data }) => {
      setTrips(data);
      setLoading(false);
    });
  }, [user?.id, year]);

  const handleAdd = async () => {
    const miles = parseFloat(form.miles);
    if (!miles || miles <= 0 || !form.purpose.trim() || !form.trip_date) {
      flash("Fill in date, miles, and purpose.");
      return;
    }
    setSaving(true);
    const { data, error } = await addTrip(user.id, {
      trip_date: form.trip_date,
      miles,
      purpose:   form.purpose.trim(),
      start_loc: form.start_loc.trim() || null,
      end_loc:   form.end_loc.trim()   || null,
      notes:     form.notes.trim()     || null,
    });
    setSaving(false);
    if (error) { flash("Error saving trip."); return; }
    setTrips((prev) => [data, ...prev]);
    setForm({ trip_date: TODAY, miles: "", purpose: "", start_loc: "", end_loc: "", notes: "" });
    flash("Trip saved!");
  };

  const handleDelete = async (id) => {
    setTrips((prev) => prev.filter((t) => t.id !== id));
    await deleteTrip(id, user.id);
  };

  const totalMiles = trips.reduce((s, t) => s + Number(t.miles), 0);
  const rate       = irsRate(year);
  const deduction  = totalMiles * rate;

  const availableYears = Object.keys(IRS_RATES).map(Number).sort((a, b) => b - a);

  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.8)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 680, padding: "24px" }}>

        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Mileage Tracker</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>
              IRS standard rate · deductible on Schedule C
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <select
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
              style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}
            >
              {availableYears.map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer", lineHeight: 1 }}>✕</button>
          </div>
        </div>

        {/* Summary cards */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 20 }}>
          {[
            { label: "Total Miles",      val: `${totalMiles.toLocaleString()} mi`, color: "#7eb8e8" },
            { label: `IRS Rate (${year})`, val: `$${rate.toFixed(3)}/mi`,           color: "#e8c97a" },
            { label: "Deduction",        val: `$${Math.round(deduction).toLocaleString()}`, color: "#7dcea0" },
          ].map((c) => (
            <div key={c.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "12px 14px" }}>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 4 }}>{c.label}</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 17, fontWeight: 600, color: c.color }}>{loading ? "..." : c.val}</div>
            </div>
          ))}
        </div>

        {/* Add trip form */}
        <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 12, padding: "16px", marginBottom: 18 }}>
          <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12 }}>Log a Trip</div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Date</div>
              <input type="date" value={form.trip_date}
                onChange={(e) => setForm((p) => ({ ...p, trip_date: e.target.value }))}
                style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
            </div>
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Miles</div>
              <input type="number" min="0.1" step="0.1" placeholder="e.g. 23.5" value={form.miles}
                onChange={(e) => setForm((p) => ({ ...p, miles: e.target.value }))}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            </div>
          </div>

          <div style={{ marginBottom: 8 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Purpose (required)</div>
            <input placeholder="e.g. Drive to client job — 123 Main St" value={form.purpose}
              onChange={(e) => setForm((p) => ({ ...p, purpose: e.target.value }))}
              style={IS} onFocus={focusGold} onBlur={blurGray} />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 12 }}>
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Start (optional)</div>
              <input placeholder="Home / shop address" value={form.start_loc}
                onChange={(e) => setForm((p) => ({ ...p, start_loc: e.target.value }))}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            </div>
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>End (optional)</div>
              <input placeholder="Job site / supplier" value={form.end_loc}
                onChange={(e) => setForm((p) => ({ ...p, end_loc: e.target.value }))}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            </div>
          </div>

          {msg && (
            <div style={{ fontSize: 11, color: msg.includes("Error") ? "#e87e7e" : "#7dcea0", background: msg.includes("Error") ? "rgba(232,126,126,0.08)" : "rgba(100,220,130,0.08)", border: `1px solid ${msg.includes("Error") ? "rgba(232,126,126,0.2)" : "rgba(100,220,130,0.2)"}`, borderRadius: 7, padding: "7px 10px", marginBottom: 10 }}>
              {msg}
            </div>
          )}

          <button onClick={handleAdd} disabled={saving}
            style={{ width: "100%", padding: "11px", background: saving ? "rgba(255,255,255,0.03)" : "linear-gradient(135deg,rgba(126,184,232,0.18),rgba(126,184,232,0.07))", border: "1px solid rgba(126,184,232,0.35)", borderRadius: 9, color: saving ? "rgba(126,184,232,0.4)" : "#7eb8e8", fontSize: 13, fontWeight: 700, cursor: saving ? "default" : "pointer", fontFamily: "inherit", transition: "all 0.2s" }}>
            {saving ? "Saving..." : "+ Add Trip"}
          </button>
        </div>

        {/* Trip list */}
        {loading ? (
          <div style={{ textAlign: "center", padding: "32px", color: "rgba(255,255,255,0.25)", fontSize: 13 }}>Loading...</div>
        ) : trips.length === 0 ? (
          <div style={{ textAlign: "center", padding: "32px", color: "rgba(255,255,255,0.2)" }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>🚗</div>
            <div style={{ fontSize: 13 }}>No trips logged for {year}</div>
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.15)" }}>Add your first trip above.</div>
          </div>
        ) : (
          <div>
            <div style={{ fontSize: 9, color: "rgba(255,255,255,0.28)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 10 }}>
              {trips.length} trip{trips.length !== 1 ? "s" : ""} · {totalMiles.toFixed(1)} total miles
            </div>
            {trips.map((trip) => {
              const tripDed = Number(trip.miles) * rate;
              return (
                <div key={trip.id} style={{ display: "flex", alignItems: "flex-start", gap: 10, padding: "11px 14px", background: "rgba(255,255,255,0.022)", border: "1px solid rgba(255,255,255,0.055)", borderRadius: 10, marginBottom: 6 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 2 }}>
                      <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 10, color: "rgba(255,255,255,0.35)" }}>{trip.trip_date}</span>
                      <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 11, fontWeight: 700, color: "#7eb8e8" }}>{Number(trip.miles).toFixed(1)} mi</span>
                    </div>
                    <div style={{ fontSize: 12, color: "#fff", fontWeight: 600, marginBottom: 1 }}>{trip.purpose}</div>
                    {(trip.start_loc || trip.end_loc) && (
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>
                        {[trip.start_loc, trip.end_loc].filter(Boolean).join(" → ")}
                      </div>
                    )}
                  </div>
                  <div style={{ textAlign: "right", flexShrink: 0 }}>
                    <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, fontWeight: 700, color: "#7dcea0" }}>
                      ${tripDed.toFixed(2)}
                    </div>
                    <div style={{ fontSize: 9, color: "rgba(255,255,255,0.25)" }}>deduction</div>
                  </div>
                  <button onClick={() => handleDelete(trip.id)}
                    style={{ padding: "4px 8px", borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer", flexShrink: 0, alignSelf: "center" }}>
                    ✕
                  </button>
                </div>
              );
            })}
          </div>
        )}

        <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
          IRS Standard Mileage Rate {year}: ${rate.toFixed(3)}/mile · Keep odometer records · See Schedule C Part II
        </div>
      </div>
    </div>
  );
}
