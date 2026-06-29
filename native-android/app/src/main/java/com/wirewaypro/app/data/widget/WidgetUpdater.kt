package com.wirewaypro.app.data.widget

import android.content.Context
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.repository.MoneyRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.widget.WirewayWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recomputes the home-screen widget's numbers from the live repositories and
 * pushes them to [WidgetSnapshotStore] + redraws the widget. Cheap and best-effort
 * — called when the app is foregrounded. No-op (silently) if signed out.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val moneyRepository: MoneyRepository,
    private val quoteRepository: QuoteRepository,
    private val jobRepository: JobRepository,
    private val store: WidgetSnapshotStore,
) {
    suspend fun refresh() {
        val userId = auth.currentUserId() ?: return
        runCatching {
            val now = YearMonth.now()
            val collected = moneyRepository.getSnapshot(userId, now.year, now.monthValue)
                .getOrNull()?.collected ?: 0.0

            val unpaid = quoteRepository.getInvoices(userId).getOrNull().orEmpty()
                .count { it.isInvoice && !it.invoicePaid }

            val today = LocalDate.now().toString() // yyyy-MM-dd
            val todayJobs = jobRepository.getJobs(userId).getOrNull().orEmpty()
                .count { it.scheduledDate == today }

            store.save(
                WidgetSnapshot(
                    collectedThisMonth = collected,
                    unpaidInvoices = unpaid,
                    todayJobs = todayJobs,
                    hasData = true,
                ),
            )
            WirewayWidget().updateAll(context)
        }
    }
}
