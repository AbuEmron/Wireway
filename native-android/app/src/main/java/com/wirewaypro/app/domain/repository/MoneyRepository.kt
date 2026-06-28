package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.MoneySnapshot

interface MoneyRepository {
    /** The money snapshot for a given month (1-12) and year. */
    suspend fun getSnapshot(userId: String, year: Int, month: Int): Result<MoneySnapshot>
}
