package com.wirewaypro.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.messaging.PushNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Smart local reminders — the functional version of "smart notifications" with no
 * push server. Runs periodically; when notifications are enabled and a user is
 * signed in, it checks Supabase for outstanding work and posts local
 * notifications: overdue invoices, jobs scheduled tomorrow, and progress draws
 * due. [ReminderLog] dedupes so each item notifies at most once per day.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsPrefs: SettingsPrefs,
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    private val jobRepository: JobRepository,
    private val reminderLog: ReminderLog,
) : CoroutineWorker(appContext, params) {

    private data class Reminder(val key: String, val title: String, val body: String)

    override suspend fun doWork(): Result {
        // Gated on the same DataStore toggle the Settings screen writes.
        if (!settingsPrefs.notificationsEnabled.first()) return Result.success()
        val userId = auth.currentUserId() ?: return Result.success()

        return try {
            val today = LocalDate.now()
            val todayStr = today.toString()
            val tomorrowStr = today.plusDays(1).toString()

            val reminders = buildList {
                quoteRepository.getInvoices(userId).getOrNull().orEmpty()
                    .filter {
                        it.isInvoice && !it.invoicePaid &&
                            it.invoiceDueDate != null && it.invoiceDueDate.take(10) < todayStr
                    }
                    .forEach {
                        add(
                            Reminder(
                                key = "invoice_overdue:${it.id}:$todayStr",
                                title = "Invoice overdue",
                                body = "${it.clientName ?: it.quoteNumber ?: "Invoice"} — ${money(it.total)} past due",
                            ),
                        )
                    }

                // Quote-expiry follow-ups: an estimate that's neither accepted nor
                // dead gets a nudge at 7, 14, 21 and 28 days old — follow-ups win
                // jobs, and after a month it stops nagging.
                quoteRepository.getEstimates(userId).getOrNull().orEmpty()
                    .filter { !it.isInvoice && (it.status ?: "draft") !in setOf("accepted", "paid", "completed", "declined") }
                    .forEach { est ->
                        val created = est.createdAt?.take(10) ?: return@forEach
                        val age = runCatching {
                            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(created), today)
                        }.getOrNull() ?: return@forEach
                        if (age in 7..28 && age % 7 == 0L) {
                            add(
                                Reminder(
                                    key = "quote_followup:${est.id}:$todayStr",
                                    title = "Follow up on your estimate",
                                    body = "${est.clientName ?: est.quoteNumber ?: "Estimate"} \u2014 ${money(est.total)} is $age days old. A quick follow-up can close it.",
                                ),
                            )
                        }
                    }

                jobRepository.getJobs(userId).getOrNull().orEmpty()
                    .filter { it.scheduledDate == tomorrowStr }
                    .forEach {
                        add(
                            Reminder(
                                key = "job_tomorrow:${it.id}:$todayStr",
                                title = "Job tomorrow",
                                body = it.title,
                            ),
                        )
                    }

                jobRepository.getDuePendingDraws(userId, todayStr).getOrNull().orEmpty()
                    .forEach {
                        add(
                            Reminder(
                                key = "draw_due:${it.id}:$todayStr",
                                title = "Progress draw due",
                                body = "${it.label} — ${money(it.net)}",
                            ),
                        )
                    }
            }

            val unseenKeys = reminderLog.unseen(reminders.map { it.key }).toSet()
            reminders.filter { it.key in unseenKeys }.forEach {
                PushNotifications.show(applicationContext, it.title, it.body)
            }
            reminderLog.markSeen(reminders.map { it.key }, todayStr)
            Result.success()
        } catch (e: Exception) {
            if (isConnectivityError(e)) Result.retry() else Result.success()
        }
    }

    private fun money(v: Double?): String =
        NumberFormat.getCurrencyInstance(Locale.US)
            .apply { maximumFractionDigits = 0 }
            .format(v ?: 0.0)
}
