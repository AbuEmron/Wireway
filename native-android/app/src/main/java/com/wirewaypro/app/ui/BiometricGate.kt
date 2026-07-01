package com.wirewaypro.app.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.wirewaypro.app.ui.components.WirewayWordmark

/**
 * Locks [content] behind a biometric prompt when the device has a biometric
 * enrolled. If biometrics are unavailable the gate is transparent (always
 * unlocked). The "use password instead" fallback signs out → normal login.
 * Never blocks the auth flow: a missing FragmentActivity or no biometric simply
 * means no lock.
 */
@Composable
fun BiometricGate(
    onUsePassword: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val canBiometric = remember(activity) {
        activity != null &&
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    var unlocked by rememberSaveable { mutableStateOf(!canBiometric) }

    if (unlocked) {
        content()
        return
    }

    // Auto-show the prompt once when first locked.
    LaunchedEffect(Unit) {
        activity?.let { promptBiometric(it) { unlocked = true } }
    }

    LockScreen(
        onUnlock = { activity?.let { promptBiometric(it) { unlocked = true } } },
        onUsePassword = onUsePassword,
    )
}

private fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            // onAuthenticationError / onAuthenticationFailed: stay locked; the
            // user can retry or fall back to password from the lock screen.
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Wireway Pro")
        .setSubtitle("Confirm it's you to continue")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText("Cancel")
        .build()
    runCatching { prompt.authenticate(info) }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun LockScreen(onUnlock: () -> Unit, onUsePassword: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WirewayWordmark()
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Locked",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Unlock with your fingerprint or face to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Unlock")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onUsePassword) { Text("Use password instead") }
        }
    }
}
