package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Local access to job-walk site photos (see [QuotePhotoEntity]). */
@Dao
interface QuotePhotoDao {

    @Insert
    suspend fun insert(photo: QuotePhotoEntity): Long

    @Query("SELECT * FROM quote_photos WHERE quoteId = :quoteId ORDER BY atMillis ASC, id ASC")
    suspend fun forQuote(quoteId: String): List<QuotePhotoEntity>

    @Query("DELETE FROM quote_photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM quote_photos WHERE quoteId = :quoteId")
    suspend fun deleteFor(quoteId: String)
}
