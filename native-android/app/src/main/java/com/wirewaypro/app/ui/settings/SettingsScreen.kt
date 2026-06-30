package com.wirewaypro.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.BuildConfig
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.TabTopBar
import com.wirewaypro.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditProfile: () -> Unit,
    onGetPaid: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TabTopBar("Settings") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Account") {
                InfoRow("Email", email ?: "—")
            }

            SectionCard(title = "Appearance") {
                ThemeSelector(selected = themeMode, onSelect = viewModel::setThemeMode)
            }

            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Edit profile & business")
            }

            Button(
                onClick = onGetPaid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Payments,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Get paid (connect Stripe)")
            }

            OutlinedButton(
                onClick = viewModel::signOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Sign out")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Wireway Pro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Segmented System / Light / Dark theme override. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    data class Option(val mode: ThemeMode, val label: String, val icon: ImageVector)
    val options = listOf(
        Option(ThemeMode.SYSTEM, "System", Icons.Outlined.PhoneAndroid),
        Option(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        Option(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, opt ->
            SegmentedButton(
                selected = selected == opt.mode,
                onClick = { onSelect(opt.mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(opt.icon, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                },
            ) {
                Text(opt.label)
            }
        }
    }
}
