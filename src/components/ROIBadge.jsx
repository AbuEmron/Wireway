// src/components/ROIBadge.jsx — small always-visible "Wireway +$X" pill  ·  Phase 2 · Feature 2
import { useState, useEffect } from "react";
import { getROI } from "../lib/roi";

const compact = (n) => {
  const v = Number(n) || 0;
  if (v >= 1000) return "$" + (v / 1000).toFixed(v >= 10000 ? 0 : 1) + "k";
  return "$" + Math.round(v);
};

export default function ROIBadge({ user, onOpen }) {
  const [total, setTotal] = useState(null);

  useEffect(() => {
    let alive = true;
    if (!user?.id) return;
    getROI(user.id).then((d) => { if (alive) setTotal(d.total); }).catch(() => {});
    return () => { alive = false; };
  }, [user?.id]);

  if (total == null || total <= 0) return null;

  return (
    <button onClick={onOpen} title="What Wireway has made & saved you"
      style={{ display: "inline-flex", alignItems: "center", gap: 4, padding: "4px 9px", borderRadius: 6, border: "1px solid rgba(125,206,160,0.3)", background: "rgba(125,206,160,0.08)", color: "#7dcea0", fontSize: 10.5, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", whiteSpace: "nowrap" }}>
      <span style={{ fontSize: 11 }}>📈</span> {compact(total)}
    </button>
  );
}
