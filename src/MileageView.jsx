// src/MileageView.jsx — mileage log with LIVE GPS tracking + IRS standard-rate deduction
import { useState, useEffect, useRef } from "react";
import { getTrips, addTrip, deleteTrip, irsRate, IRS_RATES } from "./lib/financeApi";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.07)",
  borderRadius: 7, padding: "8px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", width: "100%", outline: "none",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.07)");

const TODAY = new Date().toISOString().split("T")[0];
const CURRENT_YEAR = new Date().getFullYear();

// ── GPS filtering constants ──
const ACCURACY_MAX_M  = 50;     // ignore fixes worse than 50 m accuracy
const MIN_SEGMENT_MI  = 0.006;  // ~10 m — ignore jitter below this
const MAX_SEGMENT_MI  = 2;      // a single fix jump > 2 mi is bogus
const MAX_MPH         = 120;    // implausible speed → erroneous fix

function haversineMi(lat1, lon1, lat2, lon2) {
  const R = 3958.8, toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1), dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
const fmtElapsed = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

export default function MileageView({ user, onClose }) {
  const [year,    setYear]    = useState(CURRENT_YEAR);
  const [trips,   setTrips]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving,  setSaving]  = useState(false);
  const [msg,     setMsg]     = useState("");

  // live tracking
  const [tracking,   setTracking]   = useState(false);
  const [distanceMi, setDistanceMi] = useState(0);
  const [elapsedSec, setElapsedSec] = useState(0);
  const [accuracy,   setAccuracy]   = useState(null);
  const [gpsErr,     setGpsErr]     = useState("");
  const [gpsMsg,     setGpsMsg]     = useState("");
  const [gapWarn,    setGapWarn]    = useState(false);
  const [trackPurpose, setTrackPurpose] = useState("");

  const watchRef = useRef(null), timerRef = useRef(null), wakeRef = useRef(null);
  const lastRef  = useRef(null), distRef  = useRef(0), startRef = useRef(0), trackingRef = useRef(false);

  // manual form
  const [mode, setMode] = useState("miles"); // "miles" | "odometer"
  const [form, setForm] = useState({ trip_date: TODAY, miles: "", purpose: "", start_loc: "", end_loc: "", odo_start: "", odo_end: "" });

  const flash = (m) => { setMsg(m); setTimeout(() => setMsg(""), 2800); };

  useEffect(() => {
    if (!user?.id) return;
    setLoading(true);
    getTrips(user.id, year).then(({ data }) => { setTrips(data); setLoading(false); });
  }, [user?.id, year]);

  // Detect backgrounding — mobile browsers suspend JS/GPS when not foreground.
  useEffect(() => {
    const onVis = () => { if (document.hidden && trackingRef.current) setGapWarn(true); };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, []);

  // Cleanup on unmount
  useEffect(() => () => {
    if (watchRef.current != null) navigator.geolocation?.clearWatch(watchRef.current);
    if (timerRef.current) clearInterval(timerRef.current);
    try { wakeRef.current?.release?.(); } catch {}
  }, []);

  const onPos = (pos) => {
    const { latitude, longitude, accuracy: acc } = pos.coords;
    setAccuracy(acc);
    if (acc != null && acc > ACCURACY_MAX_M) return; // too noisy
    const last = lastRef.current;
    if (last) {
      const d = haversineMi(last.lat, last.lng, latitude, longitude);
      const dtH = (pos.timestamp - last.t) / 3600000;
      const mph = dtH > 0 ? d / dtH : 0;
      if (d >= MIN_SEGMENT_MI && d <= MAX_SEGMENT_MI && mph <= MAX_MPH) {
        distRef.current += d;
        setDistanceMi(distRef.current);
      }
    }
    lastRef.current = { lat: latitude, lng: longitude, t: pos.timestamp };
  };
  const onErr = (e) => {
    if (e.code === 1) setGpsErr("Location permission denied — turn it on in your browser, or use manual entry below.");
    else setGpsErr("Couldn't get a GPS fix. Move to open sky, or use manual entry below.");
  };

  const startTracking = async () => {
    if (!navigator.geolocation) { setGpsErr("This device doesn't support GPS. Use manual entry."); return; }
    setGpsErr(""); setGpsMsg(""); setGapWarn(false);
    distRef.current = 0; setDistanceMi(0); lastRef.current = null;
    startRef.current = Date.now(); setElapsedSec(0);
    try { if (navigator.wakeLock) wakeRef.current = await navigator.wakeLock.request("screen"); } catch {}
    watchRef.current = navigator.geolocation.watchPosition(onPos, onErr, { enableHighAccuracy: true, maximumAge: 2000, timeout: 20000 });
    timerRef.current = setInterval(() => setElapsedSec(Math.round((Date.now() - startRef.current) / 1000)), 1000);
    trackingRef.current = true; setTracking(true);
  };

  const stopTracking = async () => {
    if (watchRef.current != null) navigator.geolocation.clearWatch(watchRef.current);
    watchRef.current = null;
    if (timerRef.current) clearInterval(timerRef.current);
    try { await wakeRef.current?.release?.(); } catch {}
    wakeRef.current = null;
    trackingRef.current = false; setTracking(false);

    const miles = Math.round(distRef.current * 10) / 10;
    if (miles <= 0) { setGpsMsg("No distance recorded. If you just started, try again or use manual entry."); return; }
    setSaving(true);
    const mins = Math.round(elapsedSec / 60);
    const { data, error } = await addTrip(user.id, {
      trip_date: TODAY, miles,
      purpose: trackPurpose.trim() || "Business drive (GPS)",
      start_loc: null, end_loc: null,
      notes: `GPS tracked${gapWarn ? " — foreground only, gaps possible" : ""} · ${mins} min`,
    });
    setSaving(false);
    if (!error && data) { setTrips((p) => [data, ...p]); setTrackPurpose(""); flash(`Saved ${miles} mi from GPS`); }
    else flash("Couldn't save trip — use manual entry.");
  };

  const handleAdd = async () => {
    let miles;
    if (mode === "odometer") {
      const s = parseFloat(form.odo_start), e = parseFloat(form.odo_end);
      miles = (isFinite(s) && isFinite(e)) ? Math.round((e - s) * 10) / 10 : NaN;
    } else miles = parseFloat(form.miles);
    if (!miles || miles <= 0 || !form.purpose.trim() || !form.trip_date) { flash("Fill in date, miles (or odometer), and purpose."); return; }
    setSaving(true);
    const { data, error } = await addTrip(user.id, {
      trip_date: form.trip_date, miles, purpose: form.purpose.trim(),
      start_loc: form.start_loc.trim() || null, end_loc: form.end_loc.trim() || null,
      notes: mode === "odometer" ? `Odometer ${form.odo_start} → ${form.odo_end}` : null,
    });
    setSaving(false);
    if (error) { flash("Error saving trip."); return; }
    setTrips((p) => [data, ...p]);
    setForm({ trip_date: TODAY, miles: "", purpose: "", start_loc: "", end_loc: "", odo_start: "", odo_end: "" });
    flash("Trip saved!");
  };

  const handleDelete = async (id) => { setTrips((p) => p.filter((t) => t.id !== id)); await deleteTrip(id, user.id); };

  const totalMiles = trips.reduce((s, t) => s + Number(t.miles), 0);
  const rate = irsRate(year);
  const deduction = totalMiles * rate;
  const availableYears = Object.keys(IRS_RATES).map(Number).sort((a, b) => b - a);

  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 360, background: "rgba(0,0,0,0.8)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 680, padding: "24px" }}>

        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Mileage Tracker</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Live GPS or manual · IRS standard rate · Schedule C</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))} style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}>
              {availableYears.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer", lineHeight: 1 }}>✕</button>
          </div>
        </div>

        {/* ── LIVE TRACKER ── */}
        <div style={{ background: tracking ? "rgba(126,184,232,0.06)" : "rgba(255,255,255,0.02)", border: `1px solid ${tracking ? "rgba(126,184,232,0.4)" : "rgba(255,255,255,0.06)"}`, borderRadius: 14, padding: "18px", marginBottom: 16 }}>
          <style>{`@keyframes mvpulse{0%,100%{opacity:1}50%{opacity:0.35}}`}</style>
          {!tracking ? (
            <>
              <button onClick={startTracking} style={{ width: "100%", padding: "16px", borderRadius: 12, border: "1px solid rgba(126,184,232,0.4)", background: "linear-gradient(135deg,rgba(126,184,232,0.22),rgba(126,184,232,0.08))", color: "#7eb8e8", fontSize: 16, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", letterSpacing: "0.02em" }}>
                ▶  Start live tracking
              </button>
              <div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.35)", marginTop: 8, lineHeight: 1.6 }}>
                Keep this screen open while you drive — phone browsers pause GPS when the tab is backgrounded or the screen locks. We keep the screen awake when your phone allows it. For locked-phone driving, use manual or odometer entry below.
              </div>
            </>
          ) : (
            <>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 9, height: 9, borderRadius: "50%", background: "#e87e7e", display: "inline-block", animation: "mvpulse 1.2s infinite" }} />
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#e87e7e", letterSpacing: "0.05em" }}>TRACKING…</span>
                </div>
                <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, color: "rgba(255,255,255,0.4)" }}>{fmtElapsed(elapsedSec)}{accuracy != null ? ` · ±${Math.round(accuracy)}m` : ""}</span>
              </div>
              <div style={{ textAlign: "center", marginBottom: 12 }}>
                <span style={{ fontFamily: "'DM Mono',monospace", fontSize: 44, fontWeight: 600, color: "#7eb8e8", lineHeight: 1 }}>{distanceMi.toFixed(2)}</span>
                <span style={{ fontSize: 14, color: "rgba(255,255,255,0.4)", marginLeft: 6 }}>mi</span>
              </div>
              <input placeholder="Purpose / job (optional)" value={trackPurpose} onChange={(e) => setTrackPurpose(e.target.value)} style={{ ...IS, marginBottom: 10 }} onFocus={focusGold} onBlur={blurGray} />
              <button onClick={stopTracking} disabled={saving} style={{ width: "100%", padding: "15px", borderRadius: 12, border: "1px solid rgba(232,126,126,0.45)", background: "rgba(232,126,126,0.12)", color: "#e87e7e", fontSize: 15, fontWeight: 800, cursor: "pointer", fontFamily: "inherit" }}>
                ■  Stop &amp; save trip
              </button>
              {gapWarn && <div style={{ fontSize: 10.5, color: "#e8c97a", marginTop: 8 }}>⚠ The tab was backgrounded — distance while away from this screen may be missing.</div>}
            </>
          )}
          {gpsErr && <div style={{ fontSize: 11, color: "#e87e7e", marginTop: 10 }}>{gpsErr}</div>}
          {gpsMsg && <div style={{ fontSize: 11, color: "rgba(255,255,255,0.5)", marginTop: 10 }}>{gpsMsg}</div>}
        </div>

        {/* Summary cards */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 18 }}>
          {[
            { label: "Total Miles", val: `${totalMiles.toLocaleString()} mi`, color: "#7eb8e8" },
            { label: `IRS Rate (${year})`, val: `$${rate.toFixed(3)}/mi`, color: "#e8c97a" },
            { label: "Deduction", val: `$${Math.round(deduction).toLocaleString()}`, color: "#7dcea0" },
          ].map((c) => (
            <div key={c.label} style={{ background: "rgba(255,255,255,0.025)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 10, padding: "12px 14px" }}>
              <div style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 4 }}>{c.label}</div>
              <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 17, fontWeight: 600, color: c.color }}>{loading ? "..." : c.val}</div>
            </div>
          ))}
        </div>

        {/* Manual / odometer form */}
        <div style={{ background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.06)", borderRadius: 12, padding: "16px", marginBottom: 18 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em" }}>Manual entry</div>
            <div style={{ display: "flex", gap: 4 }}>
              {["miles", "odometer"].map((m) => (
                <button key={m} onClick={() => setMode(m)} style={{ padding: "4px 10px", borderRadius: 6, fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", textTransform: "capitalize", border: mode === m ? "1px solid rgba(126,184,232,0.5)" : "1px solid rgba(255,255,255,0.1)", background: mode === m ? "rgba(126,184,232,0.12)" : "transparent", color: mode === m ? "#7eb8e8" : "rgba(255,255,255,0.4)" }}>{m}</button>
              ))}
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
            <div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Date</div>
              <input type="date" value={form.trip_date} onChange={(e) => setForm((p) => ({ ...p, trip_date: e.target.value }))} style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
            </div>
            {mode === "miles" ? (
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Miles</div>
                <input type="number" min="0.1" step="0.1" placeholder="e.g. 23.5" value={form.miles} onChange={(e) => setForm((p) => ({ ...p, miles: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
              </div>
            ) : (
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6 }}>
                <div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Odo start</div>
                  <input type="number" placeholder="0" value={form.odo_start} onChange={(e) => setForm((p) => ({ ...p, odo_start: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                </div>
                <div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Odo end</div>
                  <input type="number" placeholder="0" value={form.odo_end} onChange={(e) => setForm((p) => ({ ...p, odo_end: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
                </div>
              </div>
            )}
          </div>

          <div style={{ marginBottom: 8 }}>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Purpose (required)</div>
            <input placeholder="e.g. Drive to client job — 123 Main St" value={form.purpose} onChange={(e) => setForm((p) => ({ ...p, purpose: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 12 }}>
            <input placeholder="Start (optional)" value={form.start_loc} onChange={(e) => setForm((p) => ({ ...p, start_loc: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
            <input placeholder="End (optional)" value={form.end_loc} onChange={(e) => setForm((p) => ({ ...p, end_loc: e.target.value }))} style={IS} onFocus={focusGold} onBlur={blurGray} />
          </div>

          {msg && (
            <div style={{ fontSize: 11, color: msg.includes("Error") || msg.includes("Couldn't") ? "#e87e7e" : "#7dcea0", background: msg.includes("Error") || msg.includes("Couldn't") ? "rgba(232,126,126,0.08)" : "rgba(100,220,130,0.08)", border: `1px solid ${msg.includes("Error") || msg.includes("Couldn't") ? "rgba(232,126,126,0.2)" : "rgba(100,220,130,0.2)"}`, borderRadius: 7, padding: "7px 10px", marginBottom: 10 }}>{msg}</div>
          )}

          <button onClick={handleAdd} disabled={saving} style={{ width: "100%", padding: "11px", background: saving ? "rgba(255,255,255,0.03)" : "linear-gradient(135deg,rgba(126,184,232,0.18),rgba(126,184,232,0.07))", border: "1px solid rgba(126,184,232,0.35)", borderRadius: 9, color: saving ? "rgba(126,184,232,0.4)" : "#7eb8e8", fontSize: 13, fontWeight: 700, cursor: saving ? "default" : "pointer", fontFamily: "inherit" }}>
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
            <div style={{ fontSize: 11, marginTop: 4, color: "rgba(255,255,255,0.15)" }}>Start live tracking or add a trip above.</div>
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
                      <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>{[trip.start_loc, trip.end_loc].filter(Boolean).join(" → ")}</div>
                    )}
                  </div>
                  <div style={{ textAlign: "right", flexShrink: 0 }}>
                    <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 12, fontWeight: 700, color: "#7dcea0" }}>${tripDed.toFixed(2)}</div>
                    <div style={{ fontSize: 9, color: "rgba(255,255,255,0.25)" }}>deduction</div>
                  </div>
                  <button onClick={() => handleDelete(trip.id)} style={{ padding: "4px 8px", borderRadius: 6, border: "1px solid rgba(232,126,126,0.2)", background: "rgba(232,126,126,0.06)", color: "rgba(232,126,126,0.5)", fontSize: 11, cursor: "pointer", flexShrink: 0, alignSelf: "center" }}>✕</button>
                </div>
              );
            })}
          </div>
        )}

        <div style={{ textAlign: "center", marginTop: 16, fontSize: 10, color: "rgba(255,255,255,0.2)", lineHeight: 1.6 }}>
          IRS Standard Mileage Rate {year}: ${rate.toFixed(3)}/mile · Logged miles flow into your tax export (Schedule C, Part II) and invoices.
        </div>
      </div>
    </div>
  );
}
