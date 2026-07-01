package com.wirewaypro.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wirewaypro.app.data.intent.AppIntents
import com.wirewaypro.app.data.intent.SharedAttachment
import com.wirewaypro.app.data.intent.SharedAttachmentHandoff
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.shortcuts.AppShortcuts
import com.wirewaypro.app.ui.WirewayApp
import com.wirewaypro.app.ui.navigation.DashDest
import com.wirewaypro.app.ui.theme.ThemeMode
import com.wirewaypro.app.ui.theme.WirewayTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-Activity host. All real UI lives in Compose ([WirewayApp]); navigation
 * between Login and the Dashboard shell is handled by Navigation-Compose.
 *
 * Also the app's single entry point for external intents — launcher shortcuts and
 * the share sheet — which it parses into a nested-nav route via [AppIntents]
 * (consumed by the dashboard once the user is signed in).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appIntents: AppIntents
    @Inject lateinit var sharedAttachmentHandoff: SharedAttachmentHandoff
    @Inject lateinit var settingsPrefs: SettingsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppShortcuts.register(this)
        handleIntent(intent)
        setContent {
            val themeName by settingsPrefs.themeMode.collectAsState(initial = "SYSTEM")
            WirewayTheme(themeMode = ThemeMode.fromName(themeName)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WirewayApp()
                }
            }
        }
    }

    /** launchMode=singleTop means re-launches (shortcut tap, new share) arrive here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> handleShare(intent)
            Intent.ACTION_VIEW -> routeForHost(intent.data?.host)?.let(appIntents::requestNav)
        }
    }

    /** Maps a `wireway://<host>` launcher-shortcut deep link to a nested-nav route. */
    private fun routeForHost(host: String?): String? = when (host) {
        Deeplinks.HOST_NEW_ESTIMATE -> DashDest.quoteBuilder(invoice = false)
        Deeplinks.HOST_SNAP_RECEIPT -> DashDest.ADD_EXPENSE
        Deeplinks.HOST_AI_TAKEOFF -> DashDest.TAKEOFF
        else -> null
    }

    /** A photo or PDF shared from another app → route into AI Takeoff, pre-attached. */
    private fun handleShare(intent: Intent) {
        val type = intent.type ?: return
        val uri = extractStream(intent) ?: return
        val isPdf = type == "application/pdf"
        val isImage = type.startsWith("image/")
        if (!isPdf && !isImage) return
        sharedAttachmentHandoff.put(SharedAttachment(uri, isPdf = isPdf))
        appIntents.requestNav(DashDest.TAKEOFF)
    }

    @Suppress("DEPRECATION")
    private fun extractStream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
}
