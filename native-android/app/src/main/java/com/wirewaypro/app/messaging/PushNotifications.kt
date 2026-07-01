package com.wirewaypro.app.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.wirewaypro.app.R

/**
 * Notification channel + display + FCM token registration. Every Firebase call is
 * guarded by [firebaseReady] so the app is completely safe until push is
 * configured (no google-services.json => these are no-ops, never a crash).
 */
object PushNotifications {

    const val CHANNEL_ID = "wireway_general"

    private fun firebaseReady(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Job, invoice, and payment alerts" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            // areNotificationsEnabled() covers the POST_NOTIFICATIONS runtime grant.
            val mgr = NotificationManagerCompat.from(context)
            if (mgr.areNotificationsEnabled()) mgr.notify(System.identityHashCode(body), notification)
        }
    }

    /** Fetches + logs the FCM token if Firebase is configured; otherwise a no-op. */
    fun registerToken(context: Context) {
        if (!firebaseReady(context)) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { /* TODO: POST to backend once an endpoint exists */ }
        }
    }
}
