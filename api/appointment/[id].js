// api/appointment/[id].js
// Public appointment endpoint — lets a client view and respond to their appointment.
// No auth required — the job UUID is the unguessable access token (same model as /api/quote/[id]).
//
// GET  /api/appointment/[id] — returns appointment + electrician contact info
// POST /api/appointment/[id] — body { action: "confirm" | "reschedule" | "cancel", ... }
//   confirm   — client confirms the appointment as-is
//   reschedule— body { proposedDate, proposedTime } — client requests a new time
//   cancel    — body { reason } — client cancels
// Every client action sets seen_by_owner=false so the electrician is flagged in-app.

const { createClient } = require("@supabase/supabase-js");

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

const PUBLIC_FIELDS =
  "id, title, client_name, job_address, scheduled_date, scheduled_time, duration_hours, status, change_status, proposed_date, proposed_time, user_id";

module.exports = async function handler(req, res) {
  const { id } = req.query;

  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(200).end();

  if (!id) return res.status(400).json({ error: "Appointment ID required" });

  // ── GET — appointment + electrician contact ──
  if (req.method === "GET") {
    const { data: job, error } = await supabase
      .from("jobs")
      .select(PUBLIC_FIELDS)
      .eq("id", id)
      .single();
    if (error || !job) return res.status(404).json({ error: "Appointment not found" });

    const { data: electrician } = await supabase
      .from("profiles")
      .select("company_name, company_phone, company_email, logo_url")
      .eq("id", job.user_id)
      .single();

    const { user_id, ...safeJob } = job; // don't expose the electrician's user id
    return res.status(200).json({ appointment: safeJob, electrician: electrician || {} });
  }

  // ── POST — client action ──
  if (req.method === "POST") {
    const { action, proposedDate, proposedTime, reason } = req.body || {};
    const now = new Date().toISOString();

    // Load current schedule so we can record the original date/time on a reschedule.
    const { data: current, error: loadErr } = await supabase
      .from("jobs")
      .select("scheduled_date, scheduled_time, status")
      .eq("id", id)
      .single();
    if (loadErr || !current) return res.status(404).json({ error: "Appointment not found" });

    let patch;
    if (action === "confirm") {
      patch = {
        change_status: "confirmed",
        change_requested_by: "client",
        change_requested_at: now,
        seen_by_owner: false,
      };
    } else if (action === "reschedule") {
      if (!proposedDate) return res.status(400).json({ error: "A proposed date is required" });
      patch = {
        change_status: "reschedule_requested",
        proposed_date: proposedDate,
        proposed_time: proposedTime || null,
        original_date: current.scheduled_date,
        original_time: current.scheduled_time,
        change_requested_by: "client",
        change_requested_at: now,
        seen_by_owner: false,
      };
    } else if (action === "cancel") {
      patch = {
        status: "cancelled",
        change_status: "cancel_requested",
        cancel_reason: reason || null,
        change_requested_by: "client",
        change_requested_at: now,
        seen_by_owner: false,
      };
    } else {
      return res.status(400).json({ error: "Unknown action" });
    }

    const { data, error } = await supabase
      .from("jobs")
      .update(patch)
      .eq("id", id)
      .select(PUBLIC_FIELDS)
      .single();
    if (error) return res.status(500).json({ error: error.message });

    const { user_id, ...safeJob } = data;
    return res.status(200).json({ success: true, appointment: safeJob });
  }

  return res.status(405).json({ error: "Method not allowed" });
};
