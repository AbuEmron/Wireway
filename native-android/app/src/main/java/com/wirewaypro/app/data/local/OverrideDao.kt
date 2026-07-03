package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Local access to the manual-override audit trail (see [OverrideEntity]). */
@Dao
interface OverrideDao {

    @Insert
    suspend fun insertAll(overrides: List<OverrideEntity>)

    @Query("SELECT * FROM quote_overrides WHERE quoteId = :quoteId ORDER BY atMillis ASC, id ASC")
    suspend fun forQuote(quoteId: String): List<OverrideEntity>

    /** The quote is gone — its trail goes with it. */
    @Query("DELETE FROM quote_overrides WHERE quoteId = :quoteId")
    suspend fun deleteFor(quoteId: String)
}
