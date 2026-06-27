// src/lib/entitlements.js — Phase 2 paywall model
// The wall sits at the MONEY MOMENT (collect payment, premium tax/accountant
// exports, unlimited active jobs). The free tier stays genuinely useful, and a
// raw "export everything" escape hatch is ALWAYS free so a user is never trapped.
import { isPro } from "./supabase";

export const FREE_ACTIVE_JOB_LIMIT = 3;

// Money moment — collecting payment online runs through Pro.
export const canCollectPayments = (profile) => isPro(profile);

// Premium, formatted tax / accountant exports (Schedule C, QuickBooks/Xero CSV).
export const canPremiumExport = (profile) => isPro(profile);

// Unlimited active jobs on Pro; a useful handful on free.
export const activeJobLimit = (profile) => (isPro(profile) ? Infinity : FREE_ACTIVE_JOB_LIMIT);
export const isActiveJob = (j) => j?.status !== "complete" && j?.status !== "cancelled";
export const countActiveJobs = (jobs = []) => jobs.filter(isActiveJob).length;
export const atActiveJobLimit = (profile, jobs = []) => countActiveJobs(jobs) >= activeJobLimit(profile);

// The "never trapped" guarantee — raw data export is free for everyone.
export const canExportRawData = () => true;
