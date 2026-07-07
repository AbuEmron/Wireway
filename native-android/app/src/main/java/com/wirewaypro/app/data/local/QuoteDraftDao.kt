package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Local access to autosaved quote-builder drafts (see [QuoteDraftEntity]). */
@Dao
interface QuoteDraftDao {

    @Query("SELECT * FROM quote_drafts WHERE draftKey = :key LIMIT 1")
    suspend fun get(key: String): QuoteDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: QuoteDraftEntity)

    @Query("DELETE FROM quote_drafts WHERE draftKey = :key")
    suspend fun delete(key: String)
}
