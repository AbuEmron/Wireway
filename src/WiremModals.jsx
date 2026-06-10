// src/WiremModals.jsx
export default function WiremModals({
  wireCalcOpen,setWireCalcOpen,wireAmps,setWireAmps,wireLen,setWireLen,wireVolt,setWireVolt,wireMat,setWireMat,wireResult,
  loadCalcOpen,setLoadCalcOpen,sqft,setSqft,smallAppl,setSmallAppl,laundry,setLaundry,dryer,setDryer,range,setRange,acTons,setAcTons,heatKw,setHeatKw,loadResult,
  checklistOpen,setChecklistOpen,checklistType,setChecklistType,checkedItems,toggleCheck,CHECKLISTS,
  showClientDB,setShowClientDB,clientSearch,setClientSearch,clients,loadClient,
  signModal,setSignModal,sigName,setSigName,sigDate,setSigDate,sigSaved,acceptQuote,quoteNumber,total,activeItems,company,inputStyle,focusGold,blurGray,currentQuoteStatus,
  editingCompany,setEditingCompany,companyDraft,setCompanyDraft,logoDataUrl,setLogoDataUrl,saveCompany,handleLogoUpload,companySaving,
  showAccount,setShowAccount,user,profile,savedQuotes,onShowPricing,paymentBanner,paymentSuccess,setPaymentSuccess,onClearBanner,
}) {
  return (
    <>
      {/* ════════════ WIRE SIZE CALCULATOR MODAL ════════════ */}
      {wireCalcOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setWireCalcOpen(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Wire Size Calculator</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>NEC Table 310.15(B)(16) · 60°C column</div>
              </div>
              <button onClick={() => setWireCalcOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:14 }}>
              {[
                { label:"Load (amps)", val:wireAmps, set:setWireAmps, ph:"e.g. 20", type:"number" },
                { label:"One-way run (ft)", val:wireLen, set:setWireLen, ph:"e.g. 75", type:"number" },
              ].map(f => (
                <div key={f.label}>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>{f.label}</div>
                  <input type={f.type} placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)} style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
                </div>
              ))}
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Voltage</div>
                <div style={{ display:"flex", gap:5 }}>
                  {["120","240"].map(v => <button key={v} onClick={() => setWireVolt(v)} style={{ flex:1, padding:"8px", borderRadius:6, border: wireVolt===v ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)", background: wireVolt===v ? "rgba(232,201,122,0.1)" : "rgba(255,255,255,0.03)", color: wireVolt===v ? "#e8c97a" : "rgba(255,255,255,0.4)", fontSize:12, fontWeight:700, cursor:"pointer", fontFamily:"'DM Mono',monospace" }}>{v}V</button>)}
                </div>
              </div>
              <div>
                <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>Conductor</div>
                <div style={{ display:"flex", gap:5 }}>
                  {["copper","aluminum"].map(m => <button key={m} onClick={() => setWireMat(m)} style={{ flex:1, padding:"8px", borderRadius:6, border: wireMat===m ? "1px solid rgba(232,201,122,0.5)" : "1px solid rgba(255,255,255,0.08)", background: wireMat===m ? "rgba(232,201,122,0.1)" : "rgba(255,255,255,0.03)", color: wireMat===m ? "#e8c97a" : "rgba(255,255,255,0.4)", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit" }}>{m[0].toUpperCase()+m.slice(1)}</button>)}
                </div>
              </div>
            </div>
            {wireResult && (
              <div style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:12, padding:"16px" }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:12, marginBottom:12 }}>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:28, fontWeight:700, color:"#e8c97a" }}># {wireResult.awg}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>AWG minimum</div>
                  </div>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:22, fontWeight:600, color:"#a8e87e" }}>{wireResult.ampacity}A</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>ampacity</div>
                  </div>
                  <div style={{ textAlign:"center" }}>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:18, fontWeight:600, color: wireResult.vDropOk ? "#a8e87e" : "#e87e7e" }}>
                      {wireLen ? `${wireResult.vDropPct}%` : "—"}
                    </div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>voltage drop</div>
                  </div>
                </div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", lineHeight:1.7, borderTop:"1px solid rgba(255,255,255,0.06)", paddingTop:10 }}>
                  <span style={{ color:"rgba(232,201,122,0.7)", fontFamily:"'DM Mono',monospace" }}>{wireResult.nec}</span> — continuous load ({wireResult.continuous}A at 125%) requires #{wireResult.awg} AWG {wireMat}.
                  {wireLen && !wireResult.vDropOk && <span style={{ color:"#e87e7e" }}> ⚠ Voltage drop exceeds 3% — consider upsizing one AWG.</span>}
                  {wireLen && wireResult.vDropOk && <span style={{ color:"#a8e87e" }}> ✓ Voltage drop within 3% limit.</span>}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ════════════ LOAD CALCULATOR MODAL ════════════ */}
      {loadCalcOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setLoadCalcOpen(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Load Calculator</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>NEC 220.82 Optional Method — dwelling units</div>
              </div>
              <button onClick={() => setLoadCalcOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:14 }}>
              {[
                { label:"Sq footage", val:sqft, set:setSqft, ph:"e.g. 2400" },
                { label:"Small appl. circuits", val:smallAppl, set:setSmallAppl, ph:"2 (min)" },
                { label:"Laundry circuits", val:laundry, set:setLaundry, ph:"1 (min)" },
                { label:"Electric dryers", val:dryer, set:setDryer, ph:"0" },
                { label:"Electric ranges", val:range, set:setRange, ph:"0" },
                { label:"AC (tons)", val:acTons, set:setAcTons, ph:"0" },
                { label:"Heat (kW)", val:heatKw, set:setHeatKw, ph:"0" },
              ].map(f => (
                <div key={f.label}>
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", marginBottom:5 }}>{f.label}</div>
                  <input type="number" min="0" placeholder={f.ph} value={f.val} onChange={e => f.set(e.target.value)} style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
                </div>
              ))}
            </div>
            {loadResult && (
              <div style={{ background:"rgba(255,255,255,0.03)", border:"1px solid rgba(255,255,255,0.08)", borderRadius:12, padding:"16px" }}>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:12, marginBottom:12, textAlign:"center" }}>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:24, fontWeight:700, color:"#e8c97a" }}>{loadResult.amps240}A</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>calculated load @240V</div>
                  </div>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:24, fontWeight:700, color:"#a8e87e" }}>{loadResult.panelSize}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>min panel size</div>
                  </div>
                  <div>
                    <div style={{ fontFamily:"'DM Mono',monospace", fontSize:20, fontWeight:600, color:"#7eb8e8" }}>{(loadResult.totalVA/1000).toFixed(1)}kVA</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.4)" }}>total demand</div>
                  </div>
                </div>
                {[
                  { label:"General lighting (3 VA/sqft)", val: loadResult.lighting },
                  { label:"Small appliance + laundry", val: loadResult.sabc },
                  { label:"After demand factors", val: loadResult.gen },
                  { label:"Dryers", val: loadResult.dryerVA },
                  { label:"Ranges", val: loadResult.rangeVA },
                  { label:"HVAC (larger of AC/heat)", val: loadResult.hvac },
                ].filter(r => r.val > 0).map(row => (
                  <div key={row.label} style={{ display:"flex", justifyContent:"space-between", fontSize:11, padding:"3px 0", borderBottom:"1px solid rgba(255,255,255,0.04)" }}>
                    <span style={{ color:"rgba(255,255,255,0.4)" }}>{row.label}</span>
                    <span style={{ fontFamily:"'DM Mono',monospace", color:"rgba(255,255,255,0.6)" }}>{row.val.toLocaleString()} VA</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ════════════ INSPECTION CHECKLIST MODAL ════════════ */}
      {checklistOpen && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setChecklistOpen(false)}>
          <div className="modal-box" style={{ padding:"24px", maxHeight:"85vh", overflowY:"auto" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:16 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>NEC 2023 Inspection Checklist</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>Pre-inspection verification</div>
              </div>
              <button onClick={() => setChecklistOpen(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <div style={{ display:"flex", gap:6, flexWrap:"wrap", marginBottom:16 }}>
              {Object.entries(CHECKLISTS).map(([key, val]) => (
                <button key={key} onClick={() => setChecklistType(key)} style={{ padding:"5px 10px", borderRadius:6, fontSize:10, fontWeight:700, border: checklistType===key ? "1px solid rgba(232,120,120,0.5)" : "1px solid rgba(255,255,255,0.08)", background: checklistType===key ? "rgba(232,120,120,0.1)" : "rgba(255,255,255,0.03)", color: checklistType===key ? "#e87e7e" : "rgba(255,255,255,0.4)", cursor:"pointer", fontFamily:"inherit" }}>{val.label}</button>
              ))}
            </div>
            {(() => {
              const cl = CHECKLISTS[checklistType];
              const done = cl.items.filter(i => checkedItems[i.id]).length;
              return (
                <>
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)" }}>{done} / {cl.items.length} complete</div>
                    <div style={{ height:4, flex:1, marginLeft:12, background:"rgba(255,255,255,0.06)", borderRadius:2, overflow:"hidden" }}>
                      <div style={{ height:"100%", width:`${(done/cl.items.length)*100}%`, background:"#7dcea0", borderRadius:2, transition:"width 0.3s" }}/>
                    </div>
                  </div>
                  {cl.items.map(item => (
                    <div key={item.id} onClick={() => toggleCheck(item.id)} style={{ display:"flex", alignItems:"flex-start", gap:10, padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,0.04)", cursor:"pointer" }}>
                      <div style={{ width:20, height:20, borderRadius:4, flexShrink:0, border: checkedItems[item.id] ? "1px solid #7dcea0" : "1px solid rgba(255,255,255,0.2)", background: checkedItems[item.id] ? "rgba(100,220,130,0.15)" : "transparent", display:"flex", alignItems:"center", justifyContent:"center", marginTop:1, transition:"all 0.15s" }}>
                        {checkedItems[item.id] && <span style={{ fontSize:11, color:"#7dcea0" }}>✓</span>}
                      </div>
                      <div style={{ flex:1 }}>
                        <div style={{ fontSize:12, color: checkedItems[item.id] ? "rgba(255,255,255,0.4)" : "rgba(255,255,255,0.8)", textDecoration: checkedItems[item.id] ? "line-through" : "none", lineHeight:1.4 }}>{item.text}</div>
                        <div style={{ fontSize:9, color:"rgba(232,201,122,0.5)", fontFamily:"'DM Mono',monospace", marginTop:2 }}>{item.nec}</div>
                      </div>
                    </div>
                  ))}
                  {done === cl.items.length && done > 0 && (
                    <div style={{ textAlign:"center", padding:"16px", background:"rgba(100,220,130,0.06)", border:"1px solid rgba(100,220,130,0.2)", borderRadius:10, marginTop:12, color:"#7dcea0", fontSize:13, fontWeight:700 }}>
                      ✓ All checks complete — ready for inspection
                    </div>
                  )}
                </>
              );
            })()}
          </div>
        </div>
      )}

      {/* ════════════ CLIENT DATABASE MODAL ════════════ */}
      {showClientDB && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setShowClientDB(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:16 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Client Database</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>{clients.length} saved client{clients.length !== 1 ? "s" : ""}</div>
              </div>
              <button onClick={() => setShowClientDB(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:22, cursor:"pointer" }}>✕</button>
            </div>
            <input placeholder="Search clients..." value={clientSearch} onChange={e => setClientSearch(e.target.value)} style={{ ...inputStyle, marginBottom:12 }} onFocus={focusGold} onBlur={blurGray} />
            {clients.length === 0 ? (
              <div style={{ textAlign:"center", padding:"30px 20px", color:"rgba(255,255,255,0.2)", fontSize:12 }}>
                No clients yet. Save a quote to add a client automatically.
              </div>
            ) : (
              clients.filter(c => !clientSearch || c.name.toLowerCase().includes(clientSearch.toLowerCase())).map(c => (
                <div key={c.id} style={{ display:"flex", alignItems:"center", gap:10, padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,0.05)" }}>
                  <div style={{ width:36, height:36, borderRadius:8, background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.2)", display:"flex", alignItems:"center", justifyContent:"center", fontFamily:"'Syne',sans-serif", fontSize:14, fontWeight:800, color:"#e8c97a", flexShrink:0 }}>
                    {c.name[0].toUpperCase()}
                  </div>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600, color:"#fff" }}>{c.name}</div>
                    <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", fontFamily:"'DM Mono',monospace" }}>
                      {[c.phone, c.email].filter(Boolean).join(" · ")} {c.jobCount > 1 ? `· ${c.jobCount} jobs` : ""}
                    </div>
                  </div>
                  <button onClick={() => loadClient(c)} style={{ padding:"6px 12px", borderRadius:7, border:"1px solid rgba(232,201,122,0.3)", background:"rgba(232,201,122,0.08)", color:"#e8c97a", fontSize:11, fontWeight:700, cursor:"pointer", fontFamily:"inherit", flexShrink:0 }}>
                    Load
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
      {editingCompany && (
        <div className="modal-overlay" onClick={e => e.target===e.currentTarget && setEditingCompany(false)}>
          <div className="modal-box" style={{ padding:"24px" }}>
            <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:20 }}>
              <div>
                <div style={{ fontFamily:"'Syne',sans-serif", fontSize:16, fontWeight:800, color:"#fff" }}>Company Profile</div>
                <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginTop:2 }}>Appears on every quote you send</div>
              </div>
              <button onClick={() => setEditingCompany(false)} style={{ background:"transparent", border:"none", color:"rgba(255,255,255,0.4)", fontSize:20, cursor:"pointer", padding:"4px 8px" }}>✕</button>
            </div>

            {/* Logo upload */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:8 }}>Company Logo</div>
              <div style={{ display:"flex", alignItems:"center", gap:12 }}>
                {logoDataUrl
                  ? <img src={logoDataUrl} alt="logo" style={{ height:52, width:"auto", maxWidth:140, objectFit:"contain", borderRadius:6, border:"1px solid rgba(255,255,255,0.1)" }} />
                  : <div style={{ width:52, height:52, borderRadius:10, overflow:"hidden" }}><img src="/logo192.png" alt="Wireway" style={{ width:"100%", height:"100%", objectFit:"contain" }} /></div>
                }
                <div style={{ flex:1 }}>
                  <label style={{ display:"inline-block", padding:"8px 14px", background:"rgba(232,201,122,0.1)", border:"1px solid rgba(232,201,122,0.3)", borderRadius:7, color:"#e8c97a", fontSize:12, fontWeight:600, cursor:"pointer" }}>
                    {logoDataUrl ? "Change Logo" : "Upload Logo"}
                    <input type="file" accept="image/*" onChange={handleLogoUpload} style={{ display:"none" }} />
                  </label>
                  {logoDataUrl && (
                    <button onClick={() => setLogoDataUrl("")} style={{ marginLeft:8, padding:"8px 12px", background:"transparent", border:"1px solid rgba(255,255,255,0.1)", borderRadius:7, color:"rgba(255,255,255,0.4)", fontSize:11, cursor:"pointer", fontFamily:"inherit" }}>Remove</button>
                  )}
                  <div style={{ fontSize:10, color:"rgba(255,255,255,0.2)", marginTop:5 }}>PNG, JPG, SVG · max 2MB · appears on quotes</div>
                </div>
              </div>
            </div>

            {/* Company fields */}
            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, marginBottom:12 }}>
              {[
                { ph:"Company name",        key:"name" },
                { ph:"Phone number",        key:"phone" },
                { ph:"Email address",       key:"email" },
                { ph:"Website",             key:"website" },
                { ph:"Street address",      key:"address" },
                { ph:"License number",      key:"license" },
                { ph:"Google Review URL",   key:"reviewUrl" },
              ].map(f => (
                <input key={f.key} placeholder={f.ph} value={companyDraft[f.key]||""} onChange={e => setCompanyDraft(p => ({ ...p, [f.key]: e.target.value }))}
                  style={inputStyle} onFocus={focusGold} onBlur={blurGray} />
              ))}
            </div>

            {/* Terms */}
            <div style={{ marginBottom:16 }}>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.3)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:6 }}>Terms & Conditions (optional)</div>
              <textarea placeholder="Payment due within 30 days. 50% deposit required before work begins. Estimate valid for 30 days..."
                value={companyDraft.terms||""} onChange={e => setCompanyDraft(p => ({ ...p, terms: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize:"vertical", lineHeight:1.6 }} onFocus={focusGold} onBlur={blurGray} />
            </div>

            {/* Stripe */}
            <div style={{ marginBottom:16, padding:"14px", background:"rgba(99,102,241,0.06)", border:"1px solid rgba(99,102,241,0.18)", borderRadius:10 }}>
              <div style={{ fontSize:10, color:"rgba(129,140,248,0.8)", textTransform:"uppercase", letterSpacing:"0.1em", marginBottom:6 }}>⚡ Stripe Integration</div>
              <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", marginBottom:8, lineHeight:1.6 }}>
                Add your Stripe Secret Key to enable online payment collection. Get it from <span style={{ color:"#818cf8" }}>dashboard.stripe.com → Developers → API Keys</span>. Use test key (sk_test_...) first, then switch to live (sk_live_...) when ready.
              </div>
              <input
                placeholder="sk_live_... or sk_test_..."
                value={companyDraft.stripeKey||""}
                onChange={e => setCompanyDraft(p => ({ ...p, stripeKey: e.target.value }))}
                type="password"
                style={{ ...inputStyle, fontFamily:"'DM Mono',monospace", fontSize:11 }}
                onFocus={focusGold} onBlur={blurGray}
              />
              <div style={{ fontSize:9, color:"rgba(255,255,255,0.2)", marginTop:5 }}>
                Your key is stored locally on this device and never sent to Wireway servers — only to Stripe when a payment is requested.
              </div>
            </div>

            <div style={{ display:"flex", gap:8 }}>
              <button onClick={saveCompany} disabled={companySaving} style={{ flex:1, padding:"12px", background:"linear-gradient(135deg,rgba(232,201,122,0.2),rgba(232,201,122,0.08))", border:"1px solid rgba(232,201,122,0.4)", borderRadius:10, color: companySaving ? "rgba(232,201,122,0.4)" : "#e8c97a", fontSize:13, fontWeight:700, cursor: companySaving ? "default" : "pointer", fontFamily:"inherit" }}>
                {companySaving ? "Saving..." : "Save Profile"}
              </button>
              <button onClick={() => setEditingCompany(false)} style={{ padding:"12px 20px", background:"transparent", border:"1px solid rgba(255,255,255,0.08)", borderRadius:10, color:"rgba(255,255,255,0.4)", fontSize:13, fontWeight:600, cursor:"pointer", fontFamily:"inherit" }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}