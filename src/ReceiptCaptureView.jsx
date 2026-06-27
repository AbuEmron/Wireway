/* eslint-disable react-hooks/exhaustive-deps */
// src/ReceiptCaptureView.jsx — Snap a receipt → OCR → expense on today's job  ·  Feature 2
import { useState, useEffect, useRef } from "react";
import { EXPENSE_CATEGORIES } from "./lib/financeApi";
import {
  processReceiptImage, ocrReceipt, saveReceiptExpense,
  getTodaysJobs, getRecentJobs,
} from "./lib/receipts";

const IS = {
  background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.09)",
  borderRadius: 7, padding: "9px 11px", fontSize: 13, color: "#fff",
  fontFamily: "inherit", outline: "none", width: "100%",
};
const focusGold = (e) => (e.target.style.borderColor = "rgba(232,201,122,0.4)");
const blurGray  = (e) => (e.target.style.borderColor = "rgba(255,255,255,0.09)");
const GREEN = "#7dcea0", RED = "#e87e7e";

export default function ReceiptCaptureView({ user, onClose }) {
  const [stage,   setStage]   = useState("capture"); // capture | scanning | review | saving
  const [img,     setImg]     = useState(null);       // { dataUrl, blob, base64, mediaType }
  const [fields,  setFields]  = useState({ vendor: "", date: "", amount: "", category: "materials", summary: "" });
  const [jobs,    setJobs]    = useState([]);
  const [jobId,   setJobId]   = useState("");
  const [todayId, setTodayId] = useState("");
  const [err,     setErr]     = useState("");
  const [done,    setDone]    = useState(0);
  const fileRef = useRef(null);

  // Load candidate jobs once; auto-select today's job if there is one.
  useEffect(() => {
    if (!user?.id) return;
    (async () => {
      const [today, recent] = await Promise.all([getTodaysJobs(user.id), getRecentJobs(user.id)]);
      const merged = [...today];
      for (const r of recent) if (!merged.find((j) => j.id === r.id)) merged.push(r);
      setJobs(merged);
      if (today[0]) { setJobId(today[0].id); setTodayId(today[0].id); }
    })();
  }, [user?.id]);

  const onPick = async (file) => {
    if (!file) return;
    setErr("");
    try {
      const processed = await processReceiptImage(file);
      setImg(processed);
      setStage("scanning");
      const ocr = await ocrReceipt(processed);
      setFields({
        vendor: ocr.vendor || "",
        date: ocr.date || new Date().toISOString().split("T")[0],
        amount: ocr.amount === "" ? "" : String(ocr.amount),
        category: ocr.category || "materials",
        summary: ocr.summary || "",
      });
      setStage("review");
    } catch (e) {
      // OCR failed — still let them enter it by hand against the photo.
      setErr(e.message || "Couldn't read that receipt — enter the details below.");
      setFields((f) => ({ ...f, date: f.date || new Date().toISOString().split("T")[0] }));
      setStage("review");
    }
  };

  const save = async () => {
    const amt = parseFloat(fields.amount);
    if (!amt || amt <= 0) { setErr("Enter the amount."); return; }
    setStage("saving");
    setErr("");
    const { error } = await saveReceiptExpense(user.id, { fields: { ...fields, amount: amt }, blob: img?.blob, jobId: jobId || null });
    if (error) { setErr("Could not save. Try again."); setStage("review"); return; }
    setDone((n) => n + 1);
    // Reset for the next receipt — contractors snap several at a time.
    setImg(null);
    setFields({ vendor: "", date: "", amount: "", category: "materials", summary: "" });
    setJobId(todayId);
    setStage("capture");
  };

  const wrap = { position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" };
  const panel = { background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 520, padding: "24px" };
  const jobLabel = (j) => `${j.title}${j.client_name ? ` · ${j.client_name}` : ""}${j.id === todayId ? "  (today)" : ""}`;

  return (
    <div style={wrap}>
      <div style={panel}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 18 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Snap a Receipt</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>
              Photo → auto-read → tagged to the job{done > 0 ? ` · ${done} saved` : ""}
            </div>
          </div>
          <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
        </div>

        <input ref={fileRef} type="file" accept="image/*" capture="environment" style={{ display: "none" }}
          onChange={(e) => onPick(e.target.files?.[0])} />

        {/* CAPTURE */}
        {stage === "capture" && (
          <>
            {done > 0 && (
              <div style={{ marginBottom: 14, fontSize: 12, color: GREEN, background: "rgba(125,206,160,0.08)", border: "1px solid rgba(125,206,160,0.2)", borderRadius: 9, padding: "10px 12px" }}>
                ✓ Receipt saved as an expense{jobs.find((j) => j.id === jobId) ? " on the job" : ""}. Snap another or close.
              </div>
            )}
            <button onClick={() => fileRef.current?.click()}
              style={{ width: "100%", padding: "40px 20px", borderRadius: 14, border: "1px dashed rgba(232,201,122,0.35)", background: "rgba(232,201,122,0.05)", color: "#e8c97a", cursor: "pointer", fontFamily: "inherit", display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 38 }}>📸</span>
              <span style={{ fontSize: 14, fontWeight: 700 }}>Take photo / choose receipt</span>
              <span style={{ fontSize: 11, color: "rgba(255,255,255,0.35)" }}>Cash &amp; supply-house receipts — we read the date, amount &amp; vendor</span>
            </button>
            {err && <div style={{ marginTop: 12, fontSize: 11, color: RED }}>{err}</div>}
          </>
        )}

        {/* SCANNING */}
        {stage === "scanning" && (
          <div style={{ textAlign: "center", padding: "10px 0" }}>
            {img?.dataUrl && <img src={img.dataUrl} alt="receipt" style={{ maxHeight: 240, maxWidth: "100%", borderRadius: 10, marginBottom: 16, objectFit: "contain" }} />}
            <div style={{ fontSize: 13, color: "#e8c97a", fontWeight: 700 }}>Reading receipt…</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.3)", marginTop: 4 }}>Extracting date, amount &amp; vendor</div>
          </div>
        )}

        {/* REVIEW */}
        {(stage === "review" || stage === "saving") && (
          <>
            {img?.dataUrl && (
              <img src={img.dataUrl} alt="receipt" style={{ maxHeight: 180, maxWidth: "100%", borderRadius: 10, marginBottom: 14, objectFit: "contain", display: "block", marginLeft: "auto", marginRight: "auto" }} />
            )}
            {err && <div style={{ marginBottom: 12, fontSize: 11, color: RED, background: "rgba(232,126,126,0.08)", border: "1px solid rgba(232,126,126,0.2)", borderRadius: 7, padding: "7px 10px" }}>{err}</div>}

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Amount ($)</div>
                <input type="number" min="0.01" step="0.01" value={fields.amount} placeholder="0.00"
                  onChange={(e) => setFields((f) => ({ ...f, amount: e.target.value }))}
                  style={{ ...IS, fontFamily: "'DM Mono',monospace" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Date</div>
                <input type="date" value={fields.date}
                  onChange={(e) => setFields((f) => ({ ...f, date: e.target.value }))}
                  style={{ ...IS, colorScheme: "dark" }} onFocus={focusGold} onBlur={blurGray} />
              </div>
            </div>

            <div style={{ marginBottom: 8 }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Vendor</div>
              <input value={fields.vendor} placeholder="Home Depot, supply house…"
                onChange={(e) => setFields((f) => ({ ...f, vendor: e.target.value }))}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Category</div>
                <select value={fields.category} onChange={(e) => setFields((f) => ({ ...f, category: e.target.value }))}
                  style={{ ...IS, colorScheme: "dark" }}>
                  {EXPENSE_CATEGORIES.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
                </select>
              </div>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>Job</div>
                <select value={jobId} onChange={(e) => setJobId(e.target.value)}
                  style={{ ...IS, colorScheme: "dark" }}>
                  <option value="">— No job —</option>
                  {jobs.map((j) => <option key={j.id} value={j.id}>{jobLabel(j)}</option>)}
                </select>
              </div>
            </div>

            <div style={{ marginBottom: 14 }}>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", marginBottom: 4 }}>What was bought (optional)</div>
              <input value={fields.summary} placeholder="Wire, conduit, breakers…"
                onChange={(e) => setFields((f) => ({ ...f, summary: e.target.value }))}
                style={IS} onFocus={focusGold} onBlur={blurGray} />
            </div>

            <div style={{ display: "flex", gap: 8 }}>
              <button onClick={() => { setStage("capture"); setImg(null); setErr(""); }}
                style={{ padding: "12px 16px", borderRadius: 9, border: "1px solid rgba(255,255,255,0.12)", background: "transparent", color: "rgba(255,255,255,0.5)", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
                Retake
              </button>
              <button onClick={save} disabled={stage === "saving"}
                style={{ flex: 1, padding: "12px", borderRadius: 9, background: "linear-gradient(135deg,rgba(125,206,160,0.2),rgba(125,206,160,0.08))", border: "1px solid rgba(125,206,160,0.4)", color: GREEN, fontSize: 13, fontWeight: 700, cursor: stage === "saving" ? "default" : "pointer", fontFamily: "inherit" }}>
                {stage === "saving" ? "Saving…" : "Save to job"}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
