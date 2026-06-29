package com.wirewaypro.app.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM pushes. Only ever instantiated when Firebase is configured
 * (google-services.json present); otherwise the system never routes messages here.
 */
class WirewayMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO(phase: push backend): POST the token to a server endpoint once one
        // exists so the backend can target this device.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Wireway Pro"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        PushNotifications.show(applicationContext, title, body)
    }
}
