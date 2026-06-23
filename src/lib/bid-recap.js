// src/lib/bid-recap.js
// ═══ WIREWAY ELITE — BID RECAP & EXPORT ═══
// The recap sheet is where industrial bids are won or lost: labor burden,
// overhead, profit, contingency, bond, and tax — structured the way GCs and
// estimators expect, with one-tap CSV export for the Excel world they live in.

// Compute the full bid recap from estimate totals and business percentages.
// All pct inputs are whole numbers (10 = 10%).
export function computeBidRecap({
  matTotal = 0,
  laborHrs = 0,
  rate = 95,
  burdenPct = 28,        // payroll taxes, insurance, benefits on labor
  matTaxPct = 8,         // sales tax on material
  contingencyPct = 0,    // risk allowance on direct cost
  overheadPct = 10,      // company overhead on direct cost
  profitPct = 10,        // profit on cost + overhead
  bondPct = 0,           // bond premium on the bid total
  mobilization = 0,      // flat mobilization/GC line
}) {
  const laborBase   = laborHrs * rate;
  const laborBurden = laborBase * (burdenPct / 100);
  const matTax      = matTotal * (matTaxPct / 100);
  const directCost  = matTotal + matTax + laborBase + laborBurden + mobilization;

  const contingency = directCost * (contingencyPct / 100);
  const overhead    = (directCost + contingency) * (overheadPct / 100);
  const costPlusOH  = directCost + contingency + overhead;
  const profit      = costPlusOH * (profitPct / 100);
  const beforeBond  = costPlusOH + profit;
  const bond        = beforeBond * (bondPct / 100);
  const bidTotal    = beforeBond + bond;

  return {
    lines: [
      { key: "material",    label: "Material",                          amount: matTotal },
      { key: "matTax",      label: `Sales Tax (${matTaxPct}%)`,         amount: matTax },
      { key: "labor",       label: `Labor — ${laborHrs.toFixed(1)} hrs @ $${rate}/hr`, amount: laborBase },
      { key: "burden",      label: `Labor Burden (${burdenPct}%)`,      amount: laborBurden },
      ...(mobilization ? [{ key: "mob", label: "Mobilization / General Conditions", amount: mobilization }] : []),
      { key: "direct",      label: "Direct Cost",                       amount: directCost, subtotal: true },
      ...(contingencyPct ? [{ key: "contingency", label: `Contingency (${contingencyPct}%)`, amount: contingency }] : []),
      { key: "overhead",    label: `Overhead (${overheadPct}%)`,        amount: overhead },
      { key: "profit",      label: `Profit (${profitPct}%)`,            amount: profit },
      ...(bondPct ? [{ key: "bond", label: `Bond (${bondPct}%)`,        amount: bond }] : []),
      { key: "total",       label: "BID TOTAL",                         amount: bidTotal, total: true },
    ],
    bidTotal,
    directCost,
    effectiveRate: laborHrs > 0 ? (bidTotal - matTotal - matTax) / laborHrs : 0,
    markupOnCost: directCost > 0 ? ((bidTotal / directCost) - 1) * 100 : 0,
  };
}

// ── CSV EXPORT ── one-tap Excel handoff for GCs and bid leveling.
function csvCell(v) {
  const s = String(v ?? "");
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

export function buildCSV(headers, rows) {
  return [headers, ...rows].map(r => r.map(csvCell).join(",")).join("\r\n");
}

// Line items + recap into a single bid workbook CSV.
export function exportBidCSV({ jobName = "Estimate", items = [], recap = null }) {
  let csv = buildCSV(
    ["Item", "Variant/Spec", "Qty", "Unit", "Material", "Labor Hrs", "Labor $", "Line Total"],
    items.map(i => [
      i.label, i.variant || i.variantLabel || "", i.qty, i.unit || "ea",
      (i.matTotal ?? i.mat ?? 0).toFixed(2),
      (i.laborHrs ?? i.hrs ?? 0).toFixed(2),
      (i.labTotal ?? i.lab ?? 0).toFixed(2),
      (i.total ?? i.lineTotal ?? 0).toFixed(2),
    ])
  );
  if (recap) {
    csv += "\r\n\r\n" + buildCSV(["Bid Recap", "Amount"], recap.lines.map(l => [l.label, l.amount.toFixed(2)]));
  }
  return { filename: `${jobName.replace(/[^a-z0-9 _-]/gi, "")} - Bid.csv`, csv };
}

// Trigger a browser download of the CSV (call from UI).
export function downloadCSV({ filename, csv }) {
  try {
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = filename;
    document.body.appendChild(a); a.click();
    document.body.removeChild(a); URL.revokeObjectURL(url);
    return true;
  } catch { return false; }
}
