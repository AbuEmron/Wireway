package com.wirewaypro.app.ui.dashboard

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.navigation.DashDest
import com.wirewaypro.app.ui.navigation.HomeTab
import com.wirewaypro.app.ui.theme.BrandGradients

private const val PACKAGE = "com.wirewaypro.app"
private const val STORE_WEB = "https://play.google.com/store/apps/details?id=$PACKAGE"

/**
 * The app's primary navigation menu, presented as a slide-in drawer. Home plus
 * every destination that makes sense to reach directly, grouped by area, then the
 * native app actions (Share, Review, Exit) at the bottom.
 *
 * Selecting any row runs [onTab] (for the four bottom-nav tabs) or [onDestination]
 * (for a pushed route) and closes the drawer. Share/Review/Exit are handled here
 * with platform intents so no navigation is needed.
 */
@Composable
fun AppDrawerContent(
    currentRoute: String?,
    onTab: (HomeTab) -> Unit,
    onDestination: (String) -> Unit,
    closeDrawer: () -> Unit,
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            DrawerHeader()

            Spacer(Modifier.height(8.dp))

            // ── Navigate ──────────────────────────────────────────────────────
            DrawerSection("Navigate")
            DrawerTab("Home", Icons.Outlined.Home, HomeTab.HOME, currentRoute, onTab, closeDrawer)
            DrawerTab("Estimates", Icons.Outlined.Description, HomeTab.ESTIMATES, currentRoute, onTab, closeDrawer)
            DrawerTab("Invoices", Icons.Outlined.Receipt, HomeTab.INVOICES, currentRoute, onTab, closeDrawer)
            DrawerLink("Jobs", Icons.Outlined.Work, DashDest.JOBS, currentRoute, onDestination, closeDrawer)
            DrawerLink("Clients", Icons.Outlined.Groups, DashDest.CLIENTS, currentRoute, onDestination, closeDrawer)

            // ── Money ─────────────────────────────────────────────────────────
            DrawerSection("Money")
            DrawerLink("Money dashboard", Icons.Outlined.Payments, DashDest.MONEY, currentRoute, onDestination, closeDrawer)
            DrawerLink("Get paid", Icons.Outlined.AccountBalanceWallet, DashDest.GET_PAID, currentRoute, onDestination, closeDrawer)
            DrawerLink("Expenses & receipts", Icons.Outlined.ReceiptLong, DashDest.EXPENSES, currentRoute, onDestination, closeDrawer)
            DrawerLink("Mileage", Icons.Outlined.DirectionsCar, DashDest.MILEAGE, currentRoute, onDestination, closeDrawer)
            DrawerLink("Time tracking", Icons.Outlined.Schedule, DashDest.TIME_TRACKING, currentRoute, onDestination, closeDrawer)
            DrawerLink("Bank", Icons.Outlined.AccountBalance, DashDest.BANK, currentRoute, onDestination, closeDrawer)

            // ── Tools ─────────────────────────────────────────────────────────
            DrawerSection("Tools")
            DrawerLink("NEC code reference", Icons.Outlined.MenuBook, DashDest.NEC, currentRoute, onDestination, closeDrawer)
            DrawerLink("Load advisor", Icons.Outlined.Bolt, DashDest.LOAD_ADVISOR, currentRoute, onDestination, closeDrawer)

            // ── Account ───────────────────────────────────────────────────────
            DrawerSection("Account")
            DrawerLink("Subscription", Icons.Outlined.WorkspacePremium, DashDest.SUBSCRIPTION, currentRoute, onDestination, closeDrawer)
            DrawerTab("Settings", Icons.Outlined.Settings, HomeTab.SETTINGS, currentRoute, onTab, closeDrawer)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // ── App actions ───────────────────────────────────────────────────
            DrawerAction("Share app", Icons.Outlined.Share) { closeDrawer(); shareApp(context) }
            DrawerAction("Review app", Icons.Outlined.Star) { closeDrawer(); reviewApp(context) }
            DrawerAction("Exit app", Icons.AutoMirrored.Outlined.ExitToApp) { closeDrawer(); exitApp(context) }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .drawBehind { drawRect(brush = BrandGradients.primary) }
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind { drawRect(color = Color.White.copy(alpha = 0.22f)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("W", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(10.dp))
            Text("Wireway Pro", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Electrical estimator", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DrawerSection(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerTab(
    label: String,
    icon: ImageVector,
    tab: HomeTab,
    currentRoute: String?,
    onTab: (HomeTab) -> Unit,
    closeDrawer: () -> Unit,
) {
    Row(label, icon, selected = currentRoute == tab.route) { closeDrawer(); onTab(tab) }
}

@Composable
private fun DrawerLink(
    label: String,
    icon: ImageVector,
    route: String,
    currentRoute: String?,
    onDestination: (String) -> Unit,
    closeDrawer: () -> Unit,
) {
    Row(label, icon, selected = currentRoute == route) { closeDrawer(); onDestination(route) }
}

@Composable
private fun DrawerAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(label, icon, selected = false, onClick = onClick)
}

@Composable
private fun Row(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

// ── Native actions ───────────────────────────────────────────────────────────

private fun shareApp(context: android.content.Context) {
    val text = "Wireway Pro — fast, accurate electrical estimates and get paid on the spot. $STORE_WEB"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Wireway Pro")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, "Share Wireway Pro")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Opens the Play store listing (native store app first, web fallback). */
private fun reviewApp(context: android.content.Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(STORE_WEB)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/** Fully exits the app (finishes all activities) rather than backgrounding it. */
private fun exitApp(context: android.content.Context) {
    (context as? Activity)?.finishAffinity()
}
