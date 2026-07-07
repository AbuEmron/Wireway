package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.UserTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Contractor-authored job templates. Offline-first: Room is the source of truth
 * (a template built on a job site survives with no signal), Supabase is the
 * synced mirror. Never loses a local edit — unsynced rows stay PENDING and push
 * on the next online opportunity.
 */
interface UserTemplateRepository {
    /** Live list of the signed-in user's templates (newest edits first). */
    fun observe(userId: String): Flow<List<UserTemplate>>

    /** Pull the server mirror into Room, preserving unsynced local rows (LWW). */
    suspend fun refresh(userId: String): Result<Unit>

    suspend fun get(id: String): UserTemplate?

    /** Create or update a template (write-through to Room, push if online). */
    suspend fun save(userId: String, template: UserTemplate): Result<UserTemplate>

    suspend fun delete(userId: String, id: String): Result<Unit>
}
