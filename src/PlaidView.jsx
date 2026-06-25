// src/PlaidView.jsx — real Plaid card-linking integration (Phase 2)
import { useState, useEffect, useCallback } from "react";
import { usePlaidLink } from "react-plaid-link";
import { supabase } from "./lib/supabase";
import { EXPENSE_CATEGORIES, categoryById } from "./lib/financeApi";

const IS = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.07)",
  borderRadius: 7,
  padding: "8px 11px",
  fontSize: 13,
  color: "#fff",
  fontFamily: "inherit",
  outline: "none",
};

const fmt = (n) =>
  "$" + Number(n).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

async function authToken() {
  const { data: { session } } = await supabase.auth.getSession();
  return session?.access_token || "";
}

async function apiPost(path, body = {}) {
  const token = await authToken();
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

// ── Recategorize dropdown ─────────────────────────────────────────────────────
function RecatSelect({ currentCat, txnId, onSave }) {
  const [val, setVal] = useState(currentCat || "other");
  return (
    <div style={{ display: "flex", gap: 5, alignItems: "center" }}>
      <select value={val} onChange={(e) => setVal(e.target.value)}
        style={{ ...IS, padding: "3px 7px", fontSize: 11, width: "auto", colorScheme: "dark" }}>
        {EXPENSE_CATEGORIES.map((c) => (
          <option key={c.id} value={c.id}>{c.label}</option>
        ))}
      </select>
      <button onClick={() => onSave(txnId, val)}
        style={{ padding: "3px 8px", borderRadius: 5, border: "1px solid rgba(168,232,126,0.35)", background: "rgba(168,232,126,0.08)", color: "#a8e87e", fontSize: 10, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
        Save
      </button>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────
export default function PlaidView({ user, onClose }) {
  const [linkToken,    setLinkToken]    = useState(null);
  const [tokenError,   setTokenError]   = useState(null);
  const [errorKind,    setErrorKind]    = useState(null); // "config" | "auth" | "network"
  const [tokenLoading, setTokenLoading] = useState(true);
  const [items,        setItems]        = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [txnLoading,   setTxnLoading]   = useState(false);
  const [syncing,      setSyncing]      = useState(false);
  const [syncMsg,      setSyncMsg]      = useState("");
  const [year,         setYear]         = useState(new Date().getFullYear());
  const [recatId,      setRecatId]      = useState(null);
  const [tab,          setTab]          = useState("transactions");

  // ── Fetch Plaid link token ──────────────────────────────────────────────────
  const fetchLinkToken = useCallback(async () => {
    setTokenLoading(true);
    setTokenError(null);
    setErrorKind(null);
    try {
      const data = await apiPost("/api/plaid-create-link-token");
      if (data?.link_token) {
        setLinkToken(data.link_token);
      } else {
        const msg = (data?.error || "").toLowerCase();
        if (msg.includes("not configured")) setErrorKind("config");
        else if (msg.includes("sign in") || msg.includes("session")) setErrorKind("auth");
        else setErrorKind("network");
        setTokenError(data?.error || "Couldn't start bank linking.");
      }
    } catch {
      setErrorKind("network");
      setTokenError("Couldn't reach bank linking — check your connection.");
    }
    setTokenLoading(false);
  }, []);

  useEffect(() => { fetchLinkToken(); }, [fetchLinkToken]);

  // ── Load items + transactions ───────────────────────────────────────────────
  const loadData = useCallback(async () => {
    if (!user?.id) return;
    setTxnLoading(true);

    const { data: itemData } = await supabase
      .from("plaid_items")
      .select("id, item_id, institution_name, last_synced_at")
      .eq("user_id", user.id)
      .order("created_at", { ascending: false });
    setItems(itemData || []);

    const { data: txnData } = await supabase
      .from("plaid_transactions")
      .select("*")
      .eq("user_id", user.id)
      .gte("txn_date", `${year}-01-01`)
      .lte("txn_date", `${year}-12-31`)
      .gt("amount", 0)
      .order("txn_date", { ascending: false })
      .limit(500);
    setTransactions(txnData || []);
    setTxnLoading(false);
  }, [user?.id, year]);

  useEffect(() => { loadData(); }, [loadData]);

  // ── Plaid Link ──────────────────────────────────────────────────────────────
  const onSuccess = useCallback(async (publicToken, metadata) => {
    setSyncing(true);
    setSyncMsg("Linking bank...");
    await apiPost("/api/plaid-exchange-token", {
      public_token:       publicToken,
      institution_id:     metadata.institution.institution_id,
      institution_name:   metadata.institution.name,
    });
    setSyncMsg("Syncing transactions...");
    const syncRes = await apiPost("/api/plaid-sync");
    setSyncMsg(`Synced ${syncRes.synced || 0} transactions`);
    await loadData();
    setSyncing(false);
    setTimeout(() => setSyncMsg(""), 4000);
  }, [loadData]);

  const { open, ready } = usePlaidLink({ token: linkToken || "", onSuccess });

  // ── Sync now ────────────────────────────────────────────────────────────────
  const handleSync = async () => {
    setSyncing(true);
    setSyncMsg("Syncing...");
    const data = await apiPost("/api/plaid-sync");
    setSyncMsg(`Synced ${data.synced ?? 0} new transactions`);
    await loadData();
    setSyncing(false);
    setTimeout(() => setSyncMsg(""), 4000);
  };

  // ── Re-categorize ───────────────────────────────────────────────────────────
  const handleRecat = async (txnId, newCat) => {
    await supabase.from("plaid_transactions").update({ user_category: newCat }).eq("id", txnId);
    setTransactions((prev) =>
      prev.map((t) => t.id === txnId ? { ...t, user_category: newCat } : t)
    );
    setRecatId(null);
  };

  // ── Remove bank ─────────────────────────────────────────────────────────────
  const handleRemoveBank = async (itemId) => {
    if (!window.confirm("Remove this bank connection? Existing transactions stay.")) return;
    await supabase.from("plaid_items").delete().eq("id", itemId).eq("user_id", user.id);
    setItems((prev) => prev.filter((i) => i.id !== itemId));
  };

  // ── Category summary ────────────────────────────────────────────────────────
  const catTotals = {};
  for (const t of transactions) {
    const cat = t.user_category || t.mapped_category || "other";
    catTotals[cat] = (catTotals[cat] || 0) + Number(t.amount);
  }
  const totalAmount = transactions.reduce((s, t) => s + Number(t.amount), 0);

  const YEARS = [2022, 2023, 2024, 2025].reverse();

  // ── Error state — friendly for end users; technical detail only for the owner ──
  if (tokenError) {
    const userMsg =
      errorKind === "config"
        ? "Bank linking isn't switched on for this account yet. Please check back soon."
        : errorKind === "auth"
        ? "Your session timed out. Please sign in again, then reopen Bank Link."
        : "We couldn't reach the bank-linking service. Check your connection and try again.";

    return (
      <div style={{ position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", display: "flex", justifyContent: "center", alignItems: "center", padding: "24px 16px" }}>
        <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 480, padding: "32px 28px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 20 }}>
            <div>
              <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Connect Your Bank</div>
              <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Auto-import job purchases</div>
            </div>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
          </div>

          <div style={{ fontSize: 13, color: "rgba(255,255,255,0.7)", lineHeight: 1.7, marginBottom: 22 }}>
            {userMsg}
          </div>

          <div style={{ display: "flex", gap: 8, marginBottom: errorKind === "config" ? 18 : 0 }}>
            {errorKind !== "config" && (
              <button onClick={fetchLinkToken} disabled={tokenLoading}
                style={{ flex: 1, padding: "12px", background: "linear-gradient(135deg,rgba(126,184,232,0.18),rgba(126,184,232,0.07))", border: "1px solid rgba(126,184,232,0.4)", borderRadius: 10, color: "#7eb8e8", fontSize: 13, fontWeight: 700, cursor: tokenLoading ? "default" : "pointer", fontFamily: "inherit" }}>
                {tokenLoading ? "Trying..." : "Try Again"}
              </button>
            )}
            <button onClick={onClose}
              style={{ flex: errorKind === "config" ? 1 : "0 0 auto", padding: "12px 18px", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 10, color: "rgba(255,255,255,0.45)", fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
              Close
            </button>
          </div>

          {errorKind === "config" && (
            <details style={{ background: "rgba(232,201,122,0.04)", border: "1px solid rgba(232,201,122,0.12)", borderRadius: 10, padding: "12px 14px", marginTop: 18 }}>
              <summary style={{ fontSize: 10, color: "rgba(232,201,122,0.6)", textTransform: "uppercase", letterSpacing: "0.1em", cursor: "pointer" }}>Account owner setup</summary>
              <div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.4)", lineHeight: 1.7, margin: "10px 0" }}>
                Add these environment variables in Vercel, then redeploy:
              </div>
              {[
                { name: "PLAID_CLIENT_ID", note: "Plaid dashboard → Team Settings → Keys" },
                { name: "PLAID_SECRET",    note: "Sandbox or production secret" },
                { name: "PLAID_ENV",       note: "sandbox · development · production" },
              ].map((v) => (
                <div key={v.name} style={{ marginBottom: 8 }}>
                  <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 11, color: "#e8c97a" }}>{v.name}</div>
                  <div style={{ fontSize: 10, color: "rgba(255,255,255,0.35)" }}>{v.note}</div>
                </div>
              ))}
            </details>
          )}
        </div>
      </div>
    );
  }

  // ── Main UI ─────────────────────────────────────────────────────────────────
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 150, background: "rgba(0,0,0,0.82)", backdropFilter: "blur(8px)", overflowY: "auto", display: "flex", justifyContent: "center", alignItems: "flex-start", padding: "24px 16px" }}>
      <div style={{ background: "#111115", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 18, width: "100%", maxWidth: 760, padding: "24px" }}>

        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 }}>
          <div>
            <div style={{ fontFamily: "'Syne',sans-serif", fontSize: 17, fontWeight: 800, color: "#fff" }}>Auto Card Import</div>
            <div style={{ fontSize: 11, color: "rgba(255,255,255,0.35)", marginTop: 2 }}>Transactions auto-categorized for Schedule C</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}
              style={{ ...IS, width: "auto", fontSize: 12, padding: "5px 10px", colorScheme: "dark" }}>
              {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
            <button onClick={onClose} style={{ background: "transparent", border: "none", color: "rgba(255,255,255,0.4)", fontSize: 22, cursor: "pointer" }}>✕</button>
          </div>
        </div>

        {/* Connect bank + sync row */}
        <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
          <button onClick={() => ready && open()} disabled={!ready || !linkToken}
            style={{ flex: 1, padding: "11px 16px", borderRadius: 10, background: ready && linkToken ? "linear-gradient(135deg,rgba(126,184,232,0.18),rgba(126,184,232,0.07))" : "rgba(255,255,255,0.03)", border: `1px solid ${ready && linkToken ? "rgba(126,184,232,0.4)" : "rgba(255,255,255,0.1)"}`, color: ready && linkToken ? "#7eb8e8" : "rgba(255,255,255,0.25)", fontSize: 13, fontWeight: 700, cursor: ready && linkToken ? "pointer" : "default", fontFamily: "inherit" }}>
            + Connect Bank / Card
          </button>
          {items.length > 0 && (
            <button onClick={handleSync} disabled={syncing}
              style={{ padding: "11px 18px", borderRadius: 10, background: "rgba(168,232,126,0.07)", border: "1px solid rgba(168,232,126,0.25)", color: syncing ? "rgba(255,255,255,0.3)" : "#a8e87e", fontSize: 12, fontWeight: 700, cursor: syncing ? "default" : "pointer", fontFamily: "inherit" }}>
              {syncing ? "Syncing..." : "Sync Now"}
            </button>
          )}
        </div>

        {syncMsg && (
          <div style={{ fontSize: 11, color: "#a8e87e", background: "rgba(168,232,126,0.07)", border: "1px solid rgba(168,232,126,0.2)", borderRadius: 8, padding: "7px 12px", marginBottom: 12 }}>
            {syncMsg}
          </div>
        )}

        {/* Summary bar */}
        {transactions.length > 0 && (
          <div style={{ background: "linear-gradient(135deg,rgba(126,184,232,0.07),rgba(255,255,255,0.02))", border: "1px solid rgba(126,184,232,0.18)", borderRadius: 12, padding: "14px 16px", marginBottom: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
              <div>
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 2 }}>Bank Expenses — {year}</div>
                <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 22, fontWeight: 600, color: "#7eb8e8" }}>{fmt(totalAmount)}</div>
              </div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)" }}>{transactions.length} transactions</div>
            </div>
            <div style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>
              {EXPENSE_CATEGORIES.filter((c) => catTotals[c.id] > 0).map((cat) => (
                <div key={cat.id} style={{ fontSize: 10, color: cat.color, background: `${cat.color}14`, border: `1px solid ${cat.color}30`, borderRadius: 5, padding: "2px 7px", fontFamily: "'DM Mono',monospace" }}>
                  {cat.label}: {fmt(catTotals[cat.id])}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab bar */}
        <div style={{ display: "flex", gap: 4, marginBottom: 16, borderBottom: "1px solid rgba(255,255,255,0.07)", paddingBottom: 0 }}>
          {["transactions", "banks"].map((t) => (
            <button key={t} onClick={() => setTab(t)}
              style={{ padding: "7px 14px", borderRadius: "7px 7px 0 0", border: "none", background: tab === t ? "rgba(255,255,255,0.06)" : "transparent", color: tab === t ? "#fff" : "rgba(255,255,255,0.35)", fontSize: 12, fontWeight: tab === t ? 700 : 400, cursor: "pointer", fontFamily: "inherit", textTransform: "capitalize" }}>
              {t === "transactions" ? `Transactions (${transactions.length})` : `Linked Banks (${items.length})`}
            </button>
          ))}
        </div>

        {/* ── TRANSACTIONS TAB ── */}
        {tab === "transactions" && (
          <>
            {txnLoading && (
              <div style={{ textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, padding: "32px 0" }}>Loading...</div>
            )}
            {!txnLoading && transactions.length === 0 && (
              <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", fontSize: 12, padding: "40px 0" }}>
                {items.length === 0
                  ? "Connect a bank above to start auto-importing transactions."
                  : `No transactions found for ${year}. Try clicking Sync Now.`}
              </div>
            )}
            {!txnLoading && transactions.length > 0 && (
              <div style={{ display: "flex", flexDirection: "column", gap: 1 }}>
                {transactions.map((t) => {
                  const effCat = t.user_category || t.mapped_category || "other";
                  const cat = categoryById(effCat);
                  const isRecat = recatId === t.id;
                  return (
                    <div key={t.id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "9px 10px", borderRadius: 8, background: "rgba(255,255,255,0.02)", borderBottom: "1px solid rgba(255,255,255,0.04)", gap: 8 }}>
                      <div style={{ minWidth: 70, fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "'DM Mono',monospace" }}>
                        {t.txn_date}
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 12, color: "#fff", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                          {t.merchant_name || t.raw_name || "—"}
                        </div>
                        {!isRecat && (
                          <div style={{ fontSize: 10, color: cat.color, background: `${cat.color}14`, border: `1px solid ${cat.color}25`, borderRadius: 4, padding: "1px 6px", display: "inline-block", marginTop: 2 }}>
                            {cat.label}
                            {t.user_category && <span style={{ opacity: 0.5, marginLeft: 3 }}>✎</span>}
                          </div>
                        )}
                        {isRecat && <RecatSelect currentCat={effCat} txnId={t.id} onSave={handleRecat} />}
                      </div>
                      <div style={{ fontFamily: "'DM Mono',monospace", fontSize: 13, fontWeight: 600, color: "#fff", flexShrink: 0 }}>
                        {fmt(t.amount)}
                      </div>
                      <button
                        onClick={() => setRecatId(isRecat ? null : t.id)}
                        title={isRecat ? "Cancel" : "Change category"}
                        style={{ flexShrink: 0, background: "transparent", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 5, color: "rgba(255,255,255,0.3)", fontSize: 10, padding: "3px 7px", cursor: "pointer", fontFamily: "inherit" }}>
                        {isRecat ? "✕" : "⇄"}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </>
        )}

        {/* ── BANKS TAB ── */}
        {tab === "banks" && (
          <>
            {items.length === 0 && (
              <div style={{ textAlign: "center", color: "rgba(255,255,255,0.25)", fontSize: 12, padding: "40px 0" }}>
                No banks connected. Click "+ Connect Bank / Card" above.
              </div>
            )}
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {items.map((item) => (
                <div key={item.id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 16px", background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.08)", borderRadius: 10 }}>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: "#fff", marginBottom: 3 }}>{item.institution_name || "Bank"}</div>
                    <div style={{ fontSize: 10, color: "rgba(255,255,255,0.3)" }}>
                      Last synced: {item.last_synced_at ? new Date(item.last_synced_at).toLocaleString() : "never"}
                    </div>
                  </div>
                  <button onClick={() => handleRemoveBank(item.id)}
                    style={{ padding: "5px 12px", borderRadius: 7, border: "1px solid rgba(232,126,126,0.3)", background: "rgba(232,126,126,0.06)", color: "#e87e7e", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>
                    Remove
                  </button>
                </div>
              ))}
            </div>

            <div style={{ marginTop: 16, padding: "12px 14px", background: "rgba(232,201,122,0.04)", border: "1px solid rgba(232,201,122,0.12)", borderRadius: 10, fontSize: 11, color: "rgba(255,255,255,0.4)", lineHeight: 1.7 }}>
              Bank credentials are handled entirely by Plaid — Wireway only stores a non-sensitive token to sync transactions. Your login is never shared with us.
            </div>
          </>
        )}
      </div>
    </div>
  );
}
