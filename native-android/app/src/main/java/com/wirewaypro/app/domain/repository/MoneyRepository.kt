package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.AgingReport
import com.wirewaypro.app.domain.model.JobPnlReport
import com.wirewaypro.app.domain.model.MoneySnapshot

interface MoneyRepository {
    /** The money snapshot for a given month (1-12) and year. */
    suspend fun getSnapshot(userId: String, year: Int, month: Int): Result<MoneySnapshot>

    /** Accounts-receivable aging: unpaid invoice-quotes + invoiced draws. */
    suspend fun getReceivables(userId: String): Result<AgingReport>

    /** Per-job profit/loss split into winners and losers. */
    suspend fun getJobsPnl(userId: String): Result<JobPnlReport>

    /** Accountant CSV for a tax year (matches dashboard.js buildAccountantCsv columns). */
    suspend fun buildAccountantCsv(userId: String, year: Int): Result<String>
}
