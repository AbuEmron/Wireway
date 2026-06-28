package com.wirewaypro.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation graph routes. */
object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

/** Tabs shown in the dashboard's bottom navigation bar. */
enum class HomeTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Outlined.Home),
    ESTIMATES("estimates", "Estimates", Icons.Outlined.Description),
    INVOICES("invoices", "Invoices", Icons.Outlined.Receipt),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
}
