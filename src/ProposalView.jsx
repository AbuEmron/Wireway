// src/ProposalView.jsx
// Premium branded proposal — full-screen view with native PDF export (window.print)
// Zero dependencies: uses the browser's print-to-PDF engine with print-perfect CSS.

const DEFAULT_TERMS = "This proposal is valid for 30 days from the date above. All work will be performed in accordance with NEC 2023 and applicable local codes. Any changes to the scope of work require a written change order. Permits and inspection fees are included unless otherwise noted. Warranty: one (1) year on workmanship from date of completion.";

export default function ProposalView({
  company, clientName, clientEmail, clientPhone, jobName, notes,
  quoteNumber, activeItems, subtotal, markupAmt, taxAmt, taxEnabled, total,
  depositPercent, sigName, sigDate, onClose,
}) {
  const today = new Date().toLocaleDateString("en-US", { year:"numeric", month:"long", day:"numeric" });
  const deposit = Math.round(total * (depositPercent || 50) / 100);
  const balance = total - deposit;
  const sellTotal = subtotal + markupAmt; // line items grossed up so they sum to sellTotal
  const factor = subtotal > 0 ? sellTotal / subtotal : 1;

  return (
    <div style={{ position:"fixed", inset:0, zIndex:400, background:"#3a3a3e", overflowY:"auto" }} className="proposal-root">
      <style>{`
        .proposal-page{max-width:760px;margin:24px auto;background:#fff;color:#1a1a1c;font-family:Georgia,'Times New Roman',serif;padding:56px 60px;box-shadow:0 8px 40px rgba(0,0,0,0.4)}
        .pv-bar{position:sticky;top:0;z-index:10;background:#111115;border-bottom:1px solid rgba(18,115,210,0.25);padding:10px 16px;display:flex;justify-content:space-between;align-items:center;font-family:'DM Sans',sans-serif}
        .pv-table{width:100%;border-collapse:collapse;font-size:13px}
        .pv-table th{text-align:left;font-size:10px;letter-spacing:0.1em;text-transform:uppercase;color:#8a8a8e;border-bottom:2px solid #1a1a1c;padding:6px 4px}
        .pv-table td{padding:8px 4px;border-bottom:1px solid #e8e8ea;vertical-align:top}
        .pv-num{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}
        @media print{
          .pv-bar{display:none!important}
          .proposal-root{position:static!important;background:#fff!important;overflow:visible!important}
          .proposal-page{box-shadow:none!important;margin:0!important;max-width:none!important;padding:24px 8px!important}
          body{background:#fff!important}
        }
      `}</style>

      {/* Toolbar (hidden in print) */}
      <div className="pv-bar no-print">
        <span style={{ color:"#1273d2", fontWeight:700, fontSize:13 }}>Proposal Preview</span>
        <div style={{ display:"flex", gap:8 }}>
          <button onClick={() => window.print()} style={{ padding:"8px 18px", borderRadius:8, background:"linear-gradient(135deg,rgba(18,115,210,0.25),rgba(18,115,210,0.1))", border:"1px solid rgba(18,115,210,0.45)", color:"#1273d2", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
            ⬇ Download PDF
          </button>
          <button onClick={onClose} style={{ padding:"8px 14px", borderRadius:8, background:"transparent", border:"1px solid rgba(255,255,255,0.15)", color:"rgba(255,255,255,0.6)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
            ✕ Close
          </button>
        </div>
      </div>

      <div className="proposal-page">
        {/* Letterhead */}
        <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start", marginBottom:40, paddingBottom:24, borderBottom:"3px solid #1a1a1c" }}>
          <div style={{ display:"flex", gap:14, alignItems:"center" }}>
            {company?.logoDataUrl
              ? <img src={company.logoDataUrl} alt="" style={{ height:54, width:"auto", maxWidth:140, objectFit:"contain" }} />
              : <img src="/logo192.png" alt="" style={{ height:54, width:54, borderRadius:10, objectFit:"cover" }} />}
            <div>
              <div style={{ fontSize:20, fontWeight:700, letterSpacing:"-0.01em" }}>{company?.name || "Your Company Name"}</div>
              <div style={{ fontSize:11, color:"#6a6a6e", marginTop:3, lineHeight:1.6 }}>
                {company?.address && <>{company.address}<br/></>}
                {[company?.phone, company?.email].filter(Boolean).join("  ·  ")}
                {company?.license && <><br/>License #{company.license}</>}
              </div>
            </div>
          </div>
          <div style={{ textAlign:"right" }}>
            <div style={{ fontSize:26, fontWeight:700, letterSpacing:"0.12em", color:"#1273d2" }}>PROPOSAL</div>
            <div style={{ fontSize:11, color:"#6a6a6e", marginTop:6, lineHeight:1.7 }}>
              {quoteNumber && <>No. {quoteNumber}<br/></>}
              {today}
            </div>
          </div>
        </div>

        {/* Prepared for */}
        <div style={{ display:"flex", gap:48, marginBottom:36 }}>
          <div>
            <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:6 }}>Prepared For</div>
            <div style={{ fontSize:15, fontWeight:700 }}>{clientName || "Client Name"}</div>
            <div style={{ fontSize:12, color:"#6a6a6e", marginTop:2, lineHeight:1.6 }}>
              {clientPhone}{clientPhone && clientEmail && " · "}{clientEmail}
            </div>
          </div>
          {jobName && (
            <div>
              <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:6 }}>Project</div>
              <div style={{ fontSize:15, fontWeight:700 }}>{jobName}</div>
            </div>
          )}
        </div>

        {/* Scope of work */}
        <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:10 }}>Scope of Work</div>
        <table className="pv-table" style={{ marginBottom:28 }}>
          <thead>
            <tr>
              <th>Description</th>
              <th style={{ textAlign:"right" }}>Qty</th>
              <th style={{ textAlign:"right" }}>Amount</th>
            </tr>
          </thead>
          <tbody>
            {activeItems.map((item, idx) => (
              <tr key={idx}>
                <td>
                  <span style={{ fontWeight:600 }}>{item.label}</span>
                  {item.variantLabel && item.variantLabel !== "Standard" && <span style={{ color:"#8a8a8e" }}> — {item.variantLabel}</span>}
                  {item.nec && <div style={{ fontSize:10, color:"#1273d2", marginTop:2 }}>Per {item.nec}</div>}
                </td>
                <td className="pv-num">{item.qty}</td>
                <td className="pv-num">${Math.round(item.lineTotal * factor).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Totals */}
        <div style={{ display:"flex", justifyContent:"flex-end", marginBottom:36 }}>
          <div style={{ width:280 }}>
            {taxEnabled && taxAmt > 0 && (
              <>
                <div style={{ display:"flex", justifyContent:"space-between", fontSize:13, padding:"5px 0" }}>
                  <span style={{ color:"#6a6a6e" }}>Subtotal</span><span className="pv-num">${Math.round(sellTotal).toLocaleString()}</span>
                </div>
                <div style={{ display:"flex", justifyContent:"space-between", fontSize:13, padding:"5px 0" }}>
                  <span style={{ color:"#6a6a6e" }}>Sales Tax</span><span className="pv-num">${Math.round(taxAmt).toLocaleString()}</span>
                </div>
              </>
            )}
            <div style={{ display:"flex", justifyContent:"space-between", fontSize:17, fontWeight:700, padding:"10px 0", borderTop:"2px solid #1a1a1c", marginTop:6 }}>
              <span>Total Investment</span><span className="pv-num">${Math.round(total).toLocaleString()}</span>
            </div>
          </div>
        </div>

        {/* Payment schedule */}
        <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:10 }}>Payment Schedule</div>
        <table className="pv-table" style={{ marginBottom:36 }}>
          <tbody>
            <tr>
              <td style={{ fontWeight:600 }}>Deposit due upon acceptance ({depositPercent || 50}%)</td>
              <td className="pv-num" style={{ fontWeight:700 }}>${deposit.toLocaleString()}</td>
            </tr>
            <tr>
              <td style={{ fontWeight:600 }}>Balance due upon completion</td>
              <td className="pv-num" style={{ fontWeight:700 }}>${balance.toLocaleString()}</td>
            </tr>
          </tbody>
        </table>

        {/* Notes */}
        {notes && (
          <>
            <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:8 }}>Notes</div>
            <div style={{ fontSize:12.5, lineHeight:1.8, color:"#3a3a3e", marginBottom:30, whiteSpace:"pre-wrap" }}>{notes}</div>
          </>
        )}

        {/* Terms */}
        <div style={{ fontSize:10, letterSpacing:"0.12em", textTransform:"uppercase", color:"#8a8a8e", marginBottom:8 }}>Terms & Conditions</div>
        <div style={{ fontSize:11, lineHeight:1.8, color:"#5a5a5e", marginBottom:44, whiteSpace:"pre-wrap" }}>
          {company?.terms || DEFAULT_TERMS}
        </div>

        {/* Signatures */}
        <div style={{ display:"flex", gap:40 }}>
          <div style={{ flex:1 }}>
            <div style={{ borderBottom:"1px solid #1a1a1c", height:34, fontFamily:"'Brush Script MT',cursive", fontSize:22, paddingLeft:4 }}>
              {sigName || ""}
            </div>
            <div style={{ fontSize:10, color:"#8a8a8e", marginTop:6, display:"flex", justifyContent:"space-between" }}>
              <span>CLIENT SIGNATURE</span><span>{sigDate || "DATE"}</span>
            </div>
          </div>
          <div style={{ flex:1 }}>
            <div style={{ borderBottom:"1px solid #1a1a1c", height:34 }} />
            <div style={{ fontSize:10, color:"#8a8a8e", marginTop:6, display:"flex", justifyContent:"space-between" }}>
              <span>{(company?.name || "CONTRACTOR").toUpperCase()}</span><span>DATE</span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div style={{ textAlign:"center", fontSize:9, color:"#b0b0b4", marginTop:48, paddingTop:16, borderTop:"1px solid #e8e8ea", letterSpacing:"0.08em" }}>
          {company?.website ? company.website.toUpperCase() + " · " : ""}GENERATED BY WIREWAY · NEC 2023 PROFESSIONAL ESTIMATING
        </div>
      </div>
    </div>
  );
}
