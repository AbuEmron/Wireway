package com.wirewaypro.app.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wirewaypro.app.ui.clients.ClientsScreen
import com.wirewaypro.app.ui.jobs.JobDetailScreen
import com.wirewaypro.app.ui.jobs.JobsScreen
import com.wirewaypro.app.ui.navigation.DashDest
import com.wirewaypro.app.ui.navigation.HomeTab
import com.wirewaypro.app.ui.quotes.EstimatesScreen
import com.wirewaypro.app.ui.quotes.InvoicesScreen
import com.wirewaypro.app.ui.quotes.QuoteDetailScreen
import com.wirewaypro.app.ui.settings.SettingsScreen

/**
 * The authenticated shell. A nested NavHost drives the four bottom-nav tabs plus
 * the Jobs/Clients lists and all list→detail screens, keeping the whole app in a
 * single Activity. The bottom bar is shown only on the top-level tabs; detail and
 * pushed screens get the full height and their own back-navigating top bar.
 */
@Composable
fun DashboardScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = HomeTab.entries.map { it.route }.toSet()
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    HomeTab.entries.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeTab.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HomeTab.HOME.route) {
                HomeScreen(
                    onOpenJobs = { navController.navigate(DashDest.JOBS) },
                    onOpenClients = { navController.navigate(DashDest.CLIENTS) },
                )
            }
            composable(HomeTab.ESTIMATES.route) {
                EstimatesScreen(
                    onOpenEstimate = { id -> navController.navigate(DashDest.estimateDetail(id)) },
                )
            }
            composable(HomeTab.INVOICES.route) {
                InvoicesScreen(
                    onOpenInvoice = { id -> navController.navigate(DashDest.invoiceDetail(id)) },
                )
            }
            composable(HomeTab.SETTINGS.route) {
                SettingsScreen()
            }

            composable(DashDest.JOBS) {
                JobsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { id -> navController.navigate(DashDest.jobDetail(id)) },
                )
            }
            composable(DashDest.CLIENTS) {
                ClientsScreen(onBack = { navController.popBackStack() })
            }

            val idArg = listOf(navArgument(DashDest.ARG_ID) { type = NavType.StringType })
            composable(DashDest.JOB_DETAIL, arguments = idArg) {
                JobDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.ESTIMATE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.INVOICE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Standard bottom-nav switch: single-top, save/restore each tab's own state. */
private fun NavHostController.navigateToTab(tab: HomeTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
