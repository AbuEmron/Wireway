# Push notifications (Firebase Cloud Messaging)

The FCM plumbing is built but **dormant until you add a Firebase project**. The
build stays green without it: the `com.google.gms.google-services` Gradle plugin
is declared but applied **only when `google-services.json` exists**, and every
Firebase runtime call is guarded (`FirebaseApp.getApps(...).isNotEmpty()`), so a
build/run without Firebase configured is a safe no-op.

## What's already wired
- `firebase-messaging` dependency + `WirewayMessagingService` (manifest-registered).
- `PushNotifications` — notification channel, display, guarded token registration.
- `NotificationsSetup` — requests `POST_NOTIFICATIONS` (Android 13+) and registers
  the token on the dashboard.

## To enable push
1. Create a Firebase project and add an Android app with the applicationId
   **`com.wirewaypro.app.native`** (and `.dev` for the debug build, or add both).
2. Download **`google-services.json`** and drop it in **`native-android/app/`**.
3. Rebuild. The google-services plugin auto-applies, `FirebaseApp` initializes,
   and `registerToken()` starts returning a token (wire it to a backend endpoint
   in `WirewayMessagingService.onNewToken` / `PushNotifications.registerToken`).
4. Test on a real device (emulators need Google Play services).

No code changes are needed to flip it on — just the `google-services.json` file.
