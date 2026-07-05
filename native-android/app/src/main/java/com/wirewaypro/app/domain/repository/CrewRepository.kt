package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.CrewMemberInput
import kotlinx.coroutines.flow.Flow

/**
 * The crew roster — offline-first (Room source of truth) with Supabase sync,
 * mirroring [JobRepository]. Elite-tier feature; gating happens in the UI layer.
 */
interface CrewRepository {
    /** Live count of crew rows with local changes still waiting to sync. */
    fun pendingSyncCount(): Flow<Int>

    /** All of the user's crew members, active first then by name. */
    suspend fun getCrew(userId: String): Result<List<CrewMember>>

    /** One crew member by id (from the local cache). */
    suspend fun getCrewMember(id: String): Result<CrewMember>

    /** Creates (id == null) or updates a crew member. Returns the saved record. */
    suspend fun saveCrew(userId: String, input: CrewMemberInput): Result<CrewMember>

    /** Deletes a crew member (tombstoned locally, removed on sync). */
    suspend fun deleteCrew(userId: String, crewId: String): Result<Unit>
}
