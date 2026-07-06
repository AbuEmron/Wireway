package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Jurisdiction
import com.wirewaypro.app.domain.model.JurisdictionInput
import kotlinx.coroutines.flow.Flow

/**
 * The user's selected AHJ jurisdiction — offline-first (Room source of truth)
 * with Supabase sync, mirroring [CrewRepository]. Exactly one active jurisdiction
 * per user (the picker updates in place). Broadly available (not tier-gated): the
 * jurisdiction + adopted-edition baseline is the trust magnet.
 */
interface JurisdictionRepository {
    /** Live count of local jurisdiction changes still waiting to sync. */
    fun pendingSyncCount(): Flow<Int>

    /** The user's current jurisdiction as a live stream (null until they pick one). */
    fun observeJurisdiction(userId: String): Flow<Jurisdiction?>

    /** The user's current jurisdiction from the local cache after a best-effort refresh. */
    suspend fun getJurisdiction(userId: String): Result<Jurisdiction?>

    /** Creates or replaces the user's jurisdiction. Returns the saved record. */
    suspend fun saveJurisdiction(userId: String, input: JurisdictionInput): Result<Jurisdiction>
}
