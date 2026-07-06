package com.wirewaypro.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.BuildConfig
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.TabTopBar
import com.wirewaypro.app.ui.components.pressScale
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.MotionTokens
import com.wirewaypro.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditProfile: () -> Unit,
    onGetPaid: () -> Unit,
    onJurisdiction: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IdentityHeader(email = email)

            SectionCard(title = "Account") {
                SettingsRow(
                    icon = Icons.Outlined.Edit,
                    title = "Edit profile & business",
                    subtitle = "Name, license, logo — what your quotes show",
                    onClick = onEditProfile,
                )
                SettingsRow(
                    icon = Icons.Outlined.Payments,
                    title = "Get paid",
                    subtitle = "Connect Stripe to take card payments",
                    onClick = onGetPaid,
                )
                SettingsRow(
                    icon = Icons.Outlined.Place,
                    title = "Jurisdiction (AHJ)",
                    subtitle = "Set your state & local AHJ — the code your inspector enforces",
                    onClick = onJurisdiction,
                )
            }

            SectionCard(title = "Appearance") {
                ThemeSelector(selected = themeMode, onSelect = viewModel::setThemeMode)
            }

            OutlinedButton(
                onClick = viewModel::signOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
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

/** Gradient-avatar identity strip: who's signed in, at a glance. */
@Composable
private fun IdentityHeader(email: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .drawBehind { drawRect(brush = BrandGradients.primary) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (email?.firstOrNull()?.uppercaseChar() ?: '⚡').toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "Signed in",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = email ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * A tappable settings row: icon chip, title + one-line hint, trailing chevron.
 * Minimum 52dp tall — a gloved thumb can't miss it.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val haptics = rememberWirewayHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.tap()
                onClick()
            }
            .defaultMinSize(minHeight = 52.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRoundRect(
                        brush = BrandGradients.primary,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        alpha = 0.14f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The mockup's Appearance picker: three icon tiles (Light / Dark / System).
 * The selected tile lifts with a brand border + tinted fill on a color spring,
 * pops on the bouncy spring, and every switch ticks. The theme itself crossfades
 * app-wide via WirewayTheme, so the tap feels like flipping a physical switch.
 */
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    data class Option(val mode: ThemeMode, val label: String, val icon: ImageVector)
    val options = listOf(
        Option(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        Option(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
        Option(ThemeMode.SYSTEM, "System", Icons.Outlined.PhoneAndroid),
    )
    val haptics = rememberWirewayHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { opt ->
            val isSelected = selected == opt.mode
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = MotionTokens.standardSpec(),
                label = "theme-border",
            )
            val fill by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                },
                animationSpec = MotionTokens.standardSpec(),
                label = "theme-fill",
            )
            val tint by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = MotionTokens.standardSpec(),
                label = "theme-tint",
            )
            val pop by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                animationSpec = MotionTokens.springBouncy(),
                label = "theme-pop",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(pressedScale = 0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                    .drawBehind { drawRect(fill) }
                    .clickable {
                        if (!isSelected) {
                            haptics.tick()
                            onSelect(opt.mode)
                        }
                    }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    opt.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                        },
                )
                Spacer(Modifier.padding(top = 6.dp))
                Text(
                    opt.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}
