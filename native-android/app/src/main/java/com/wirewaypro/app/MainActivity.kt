package com.wirewaypro.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wirewaypro.app.ui.WirewayApp
import com.wirewaypro.app.ui.theme.WirewayTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host. All real UI lives in Compose ([WirewayApp]); navigation
 * between Login and the Dashboard shell is handled by Navigation-Compose.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WirewayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WirewayApp()
                }
            }
        }
    }
}
