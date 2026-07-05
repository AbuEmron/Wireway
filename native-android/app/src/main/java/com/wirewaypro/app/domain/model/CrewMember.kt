package com.wirewaypro.app.domain.model

/**
 * A crew member on the contractor's team (`crew_members` table). The
 * [hourlyCostRate] is what the contractor PAYS this person per hour — the cost
 * side of true job costing (never a client-facing bill rate). Logging this
 * person's hours against a job contributes hours × [hourlyCostRate] as real
 * labor cost, deterministically (see [TimeEntry.laborCost] / [JobCosting]).
 *
 * Offline-first: mirrored into Room ([com.wirewaypro.app.data.local.CrewMemberEntity])
 * and synced to Supabase with the same LWW pattern as jobs/clients.
 */
data class CrewMember(
    val id: String,
    val name: String,
    val role: String?,
    val hourlyCostRate: Double,
    val active: Boolean = true,
    val createdAt: String? = null,
    val syncState: SyncState = SyncState.SYNCED,
)

/** Everything the crew editor can write. Mirrors the `crew_members` columns. id == null → create. */
data class CrewMemberInput(
    val id: String?,
    val name: String,
    val role: String?,
    val hourlyCostRate: Double,
    val active: Boolean = true,
)
