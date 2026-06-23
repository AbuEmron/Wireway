// src/CustomersView.jsx
// Customer CRM — full-screen overlay listing every client with quote history,
// revenue stats, and one-tap new estimate with their info pre-filled.

import { useState, useMemo } from "react";
import { updateClient, deleteClient } from "./lib/supabase";

const STATUS_LABEL = {
  draft:"Draft", sent:"Sent", accepted:"Accepted", deposit_paid:"Deposit Paid",
  paid:"Paid", invoiced:"Invoiced", completed:"Complete", cancelled:"Cancelled",
};

export default function CustomersView({ user, clients, savedQuotes, onLoadQuote, onNewEstimate, onClientsChange, onClose }) {
  const [search,   setSearch]   = useState("");
  const [selected, setSelected] = useState(null); // client object
  const [editing,  setEditing]  = useState(false);
  const [draft,    setDraft]    = useState({ name:"", email:"", phone:"" });
  const [confirmDel, setConfirmDel] = useState(false);
  const [busy,     setBusy]     = useState(false);

  const startEdit = () => { setDraft({ name:selected.name||"", email:selected.email||"", phone:selected.phone||"" }); setEditing(true); };

  const saveEdit = async () => {
    if (!draft.name.trim()) return;
    setBusy(true);
    const { data, error } = await updateClient(user.id, selected.id, draft);
    setBusy(false);
    if (!error && data) {
      const next = clients.map(cl => cl.id === selected.id ? { ...cl, ...data } : cl);
      if (onClientsChange) onClientsChange(next);
      setSelected(prev => ({ ...prev, ...data }));
      setEditing(false);
    }
  };

  const doDelete = async () => {
    setBusy(true);
    const { error } = await deleteClient(user.id, selected.id);
    setBusy(false);
    if (!error) {
      if (onClientsChange) onClientsChange(clients.filter(cl => cl.id !== selected.id));
      setConfirmDel(false);
      setSelected(null);
    }
  };

  // Group quotes by client (match on name, fall back to email)
  const enriched = useMemo(() => {
    return (clients || []).map(c => {
      const quotes = (savedQuotes || []).filter(q => {
        const qName = (q.client_name || q.clientName || "").toLowerCase().trim();
        const qEmail = (q.client_email || q.clientEmail || "").toLowerCase().trim();
        return (c.name && qName === c.name.toLowerCase().trim()) ||
               (c.email && qEmail && qEmail === c.email.toLowerCase().trim());
      });
      const won = quotes.filter(q => ["accepted","deposit_paid","paid","invoiced","completed"].includes(q.status));
      const revenue = won.reduce((a,q) => a + (q.total || 0), 0);
      const lastDate = quotes.length ? quotes.map(q => q.created_at || q.savedAt).sort().slice(-1)[0] : null;
      return { ...c, quotes, quoteCount: quotes.length, wonCount: won.length, revenue, lastDate,
               winRate: quotes.length ? Math.round(won.length / quotes.length * 100) : 0 };
    }).sort((a,b) => (b.lastDate || "").localeCompare(a.lastDate || ""));
  }, [clients, savedQuotes]);

  const filtered = enriched.filter(c =>
    !search ||
    (c.name || "").toLowerCase().includes(search.toLowerCase()) ||
    (c.email || "").toLowerCase().includes(search.toLowerCase()) ||
    (c.phone || "").includes(search)
  );

  const card = { background:"var(--card)", border:"1px solid var(--line)", borderRadius:12 };

  return (
    <div style={{ position:"fixed", inset:0, zIndex:300, background:"#0a0a0c", overflowY:"auto", fontFamily:"'DM Sans',sans-serif", color:"#fff" }}>
      <style>{`@keyframes fadeUp{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        .cust-row:hover{background:rgba(255,255,255,0.045)!important}`}</style>

      {/* Header */}
      <div style={{ borderBottom:"1px solid var(--line)", background:"rgba(10,10,12,0.92)", backdropFilter:"blur(20px)", WebkitBackdropFilter:"blur(20px)", position:"sticky", top:0, zIndex:10, padding:"0 20px" }}>
        <div style={{ maxWidth:680, margin:"0 auto", height:54, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
          <div style={{ display:"flex", alignItems:"center", gap:10 }}>
            {selected && (
              <button onClick={() => { setSelected(null); setEditing(false); setConfirmDel(false); }} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.5)", fontSize:18, cursor:"pointer", padding:"0 4px" }}>←</button>
            )}
            <span style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, letterSpacing:"-0.02em" }}>
              {selected ? selected.name : "Customers"}
            </span>
            {!selected && <span style={{ fontSize:11, color:"rgba(255,255,255,0.3)" }}>{enriched.length} total</span>}
          </div>
          <button onClick={onClose} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid var(--line-strong)", background:"transparent", color:"rgba(255,255,255,0.45)", fontSize:11, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>✕ Close</button>
        </div>
      </div>

      <div style={{ maxWidth:680, margin:"0 auto", padding:"20px 20px 60px" }}>

        {/* ── LIST VIEW ── */}
        {!selected && (
          <div style={{ animation:"fadeUp 0.3s ease both" }}>
            <input
              placeholder="Search name, email, or phone..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ width:"100%", padding:"12px 14px", background:"var(--card)", border:"1px solid var(--line-strong)", borderRadius:10, color:"#fff", fontSize:14, fontFamily:"inherit", outline:"none", marginBottom:14, boxSizing:"border-box" }}
            />

            {filtered.length === 0 && (
              <div style={{ textAlign:"center", padding:"50px 20px", color:"rgba(255,255,255,0.25)" }}>
                <div style={{ fontSize:30, marginBottom:10 }}>👥</div>
                <div style={{ fontSize:13 }}>{search ? "No customers match your search" : "No customers yet — save a quote with client info and they'll appear here"}</div>
              </div>
            )}

            {filtered.map(c => (
              <div key={c.id} className="cust-row" onClick={() => { setSelected(c); setEditing(false); setConfirmDel(false); }}
                style={{ ...card, display:"flex", alignItems:"center", gap:12, padding:"13px 15px", marginBottom:7, cursor:"pointer", transition:"background 0.15s" }}>
                <div style={{ width:38, height:38, borderRadius:10, background:"rgba(var(--accent-rgb),0.1)", border:"1px solid rgba(var(--accent-rgb),0.2)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:15, fontWeight:800, color:"var(--accent)", flexShrink:0 }}>
                  {(c.name || "?").charAt(0).toUpperCase()}
                </div>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontSize:14, fontWeight:700 }}>{c.name}</div>
                  <div style={{ fontSize:10.5, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                    {c.quoteCount} quote{c.quoteCount !== 1 ? "s" : ""}{c.quoteCount > 0 && ` · ${c.winRate}% won`}{c.phone && ` · ${c.phone}`}
                  </div>
                </div>
                <div style={{ textAlign:"right", flexShrink:0 }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:14, fontWeight:600, color: c.revenue > 0 ? "var(--accent)" : "rgba(255,255,255,0.25)" }}>
                    ${Math.round(c.revenue).toLocaleString()}
                  </div>
                  <div style={{ fontSize:9, color:"rgba(255,255,255,0.25)" }}>lifetime</div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* ── DETAIL VIEW ── */}
        {selected && (
          <div style={{ animation:"fadeUp 0.3s ease both" }}>
            {/* Contact card */}
            <div style={{ ...card, padding:"16px 18px", marginBottom:12 }}>
              {!editing ? (
                <>
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start" }}>
                    <div style={{ minWidth:0 }}>
                      <div style={{ fontSize:17, fontWeight:800, fontFamily:"'Syne',sans-serif", marginBottom:5 }}>{selected.name}</div>
                      <div style={{ fontSize:12, color:"rgba(255,255,255,0.45)", lineHeight:1.8 }}>
                        {selected.phone && <div>📞 <a href={`tel:${selected.phone}`} style={{ color:"#7eb8e8", textDecoration:"none" }}>{selected.phone}</a></div>}
                        {selected.email && <div>✉️ <a href={`mailto:${selected.email}`} style={{ color:"#7eb8e8", textDecoration:"none" }}>{selected.email}</a></div>}
                      </div>
                    </div>
                    <button onClick={() => onNewEstimate(selected)} style={{ padding:"9px 16px", borderRadius:9, background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.2),rgba(var(--accent-rgb),0.07))", border:"1px solid rgba(var(--accent-rgb),0.4)", color:"var(--accent)", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"inherit", whiteSpace:"nowrap", flexShrink:0 }}>
                      + New Estimate
                    </button>
                  </div>
                  <div style={{ display:"flex", gap:6, marginTop:14, paddingTop:12, borderTop:"1px solid var(--line)" }}>
                    <button onClick={startEdit} style={{ flex:1, padding:"8px", borderRadius:8, background:"var(--card)", border:"1px solid var(--line-strong)", color:"rgba(255,255,255,0.6)", fontSize:11.5, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                      ✏️ Edit Info
                    </button>
                    {!confirmDel ? (
                      <button onClick={() => setConfirmDel(true)} style={{ flex:1, padding:"8px", borderRadius:8, background:"rgba(232,126,126,0.05)", border:"1px solid rgba(232,126,126,0.2)", color:"#e87e7e", fontSize:11.5, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>
                        🗑 Delete
                      </button>
                    ) : (
                      <button onClick={doDelete} disabled={busy} style={{ flex:1.4, padding:"8px", borderRadius:8, background:"rgba(232,126,126,0.15)", border:"1px solid rgba(232,126,126,0.45)", color:"#e87e7e", fontSize:11.5, fontWeight:800, cursor:"pointer", fontFamily:"inherit" }}>
                        {busy ? "Deleting..." : "Tap again to confirm delete"}
                      </button>
                    )}
                    {confirmDel && (
                      <button onClick={() => setConfirmDel(false)} style={{ padding:"8px 12px", borderRadius:8, background:"transparent", border:"1px solid var(--line)", color:"rgba(255,255,255,0.4)", fontSize:11.5, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                        Cancel
                      </button>
                    )}
                  </div>
                  {confirmDel && (
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginTop:8, lineHeight:1.6 }}>
                      Removes the customer card only — their saved quotes stay in your quote history.
                    </div>
                  )}
                </>
              ) : (
                <>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:10 }}>Edit Customer</div>
                  {[
                    { key:"name",  label:"Name",  type:"text" },
                    { key:"phone", label:"Phone", type:"tel" },
                    { key:"email", label:"Email", type:"email" },
                  ].map(f => (
                    <input key={f.key} type={f.type} placeholder={f.label} value={draft[f.key]}
                      onChange={e => setDraft(d => ({ ...d, [f.key]: e.target.value }))}
                      style={{ width:"100%", padding:"11px 13px", marginBottom:8, background:"var(--card)", border:"1px solid var(--line-strong)", borderRadius:9, color:"#fff", fontSize:13.5, fontFamily:"inherit", outline:"none", boxSizing:"border-box" }} />
                  ))}
                  <div style={{ display:"flex", gap:6, marginTop:4 }}>
                    <button onClick={saveEdit} disabled={busy || !draft.name.trim()} style={{ flex:1, padding:"10px", borderRadius:9, background:"linear-gradient(135deg,rgba(var(--accent-rgb),0.22),rgba(var(--accent-rgb),0.08))", border:"1px solid rgba(var(--accent-rgb),0.45)", color:"var(--accent)", fontSize:12, fontWeight:800, cursor:"pointer", fontFamily:"inherit", opacity: !draft.name.trim() ? 0.5 : 1 }}>
                      {busy ? "Saving..." : "Save Changes"}
                    </button>
                    <button onClick={() => setEditing(false)} style={{ padding:"10px 16px", borderRadius:9, background:"transparent", border:"1px solid var(--line)", color:"rgba(255,255,255,0.4)", fontSize:12, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                      Cancel
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Stats */}
            <div style={{ display:"grid", gridTemplateColumns:"repeat(4,1fr)", gap:7, marginBottom:14 }}>
              {[
                { l:"Quotes",   v: selected.quoteCount,                                  c:"#7eb8e8" },
                { l:"Won",      v: selected.wonCount,                                    c:"#7dcea0" },
                { l:"Win rate", v: `${selected.winRate}%`,                               c:"#b87ee8" },
                { l:"Revenue",  v: `$${Math.round(selected.revenue).toLocaleString()}`,  c:"var(--accent)" },
              ].map(s => (
                <div key={s.l} style={{ ...card, padding:"10px 8px", textAlign:"center" }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:15, fontWeight:600, color:s.c }}>{s.v}</div>
                  <div style={{ fontSize:8.5, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.08em", marginTop:3 }}>{s.l}</div>
                </div>
              ))}
            </div>

            {/* Quote history */}
            <div style={{ fontSize:10, color:"rgba(255,255,255,0.28)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:9 }}>Quote History</div>
            {selected.quotes.length === 0 && (
              <div style={{ ...card, padding:"24px", textAlign:"center", fontSize:12, color:"rgba(255,255,255,0.25)" }}>
                No quotes for this customer yet
              </div>
            )}
            {[...selected.quotes].sort((a,b) => (b.created_at || "").localeCompare(a.created_at || "")).map(q => (
              <div key={q.id} className="cust-row" onClick={() => onLoadQuote(q)}
                style={{ ...card, display:"flex", alignItems:"center", gap:10, padding:"11px 14px", marginBottom:6, cursor:"pointer", transition:"background 0.15s" }}>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontSize:13, fontWeight:600 }}>{q.job_name || q.jobName || q.quote_number || "Quote"}</div>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                    {q.quote_number}{q.created_at && ` · ${new Date(q.created_at).toLocaleDateString()}`}
                  </div>
                </div>
                <div style={{ textAlign:"right", flexShrink:0 }}>
                  <div style={{ fontFamily:"'DM Mono',monospace", fontSize:13, fontWeight:600, color:"var(--accent)" }}>${(q.total || 0).toLocaleString()}</div>
                  <div style={{ fontSize:9, fontWeight:700, color:"rgba(255,255,255,0.4)", marginTop:1 }}>{STATUS_LABEL[q.status] || "Draft"}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
