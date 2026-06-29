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
import com.wirewaypro.app.ui.bank.BankScreen
import com.wirewaypro.app.ui.clients.ClientEditScreen
import com.wirewaypro.app.ui.clients.ClientsScreen
import com.wirewaypro.app.ui.expenses.AddExpenseScreen
import com.wirewaypro.app.ui.expenses.ExpensesScreen
import com.wirewaypro.app.ui.jobs.JobDetailScreen
import com.wirewaypro.app.ui.jobs.JobEditScreen
import com.wirewaypro.app.ui.jobs.JobsCalendarScreen
import com.wirewaypro.app.ui.jobs.JobsScreen
import com.wirewaypro.app.ui.navigation.DashDest
import com.wirewaypro.app.ui.navigation.HomeTab
import com.wirewaypro.app.ui.quotes.EstimatesScreen
import com.wirewaypro.app.ui.quotes.InvoicesScreen
import com.wirewaypro.app.ui.money.MoneyScreen
import com.wirewaypro.app.ui.quotes.QuoteBuilderScreen
import com.wirewaypro.app.ui.quotes.QuoteDetailScreen
import com.wirewaypro.app.ui.settings.ProfileEditScreen
import com.wirewaypro.app.ui.settings.SettingsScreen
import com.wirewaypro.app.ui.subscription.SubscriptionsScreen
import com.wirewaypro.app.ui.takeoff.TakeoffScreen

/**
 * The authenticated shell. A nested NavHost drives the four bottom-nav tabs plus
 * the Jobs/Clients lists and all list→detail screens, keeping the whole app in a
 * single Activity. The bottom bar is shown only on the top-level tabs; detail and
 * pushed screens get the full height and their own back-navigating top bar.
 */
@Composable
fun DashboardScreen() {
    com.wirewaypro.app.ui.NotificationsSetup()
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
                    onOpenExpenses = { navController.navigate(DashDest.EXPENSES) },
                    onOpenMoney = { navController.navigate(DashDest.MONEY) },
                    onOpenTakeoff = { navController.navigate(DashDest.TAKEOFF) },
                    onOpenBank = { navController.navigate(DashDest.BANK) },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(HomeTab.ESTIMATES.route) {
                EstimatesScreen(
                    onOpenEstimate = { id -> navController.navigate(DashDest.estimateDetail(id)) },
                    onAdd = { navController.navigate(DashDest.quoteBuilder(invoice = false)) },
                )
            }
            composable(HomeTab.INVOICES.route) {
                InvoicesScreen(
                    onOpenInvoice = { id -> navController.navigate(DashDest.invoiceDetail(id)) },
                    onAdd = { navController.navigate(DashDest.quoteBuilder(invoice = true)) },
                )
            }
            composable(HomeTab.SETTINGS.route) {
                SettingsScreen(
                    onEditProfile = { navController.navigate(DashDest.PROFILE_EDIT) },
                )
            }

            composable(DashDest.JOBS) {
                JobsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { id -> navController.navigate(DashDest.jobDetail(id)) },
                    onAdd = { navController.navigate(DashDest.jobEdit()) },
                    onOpenCalendar = { navController.navigate(DashDest.JOBS_CALENDAR) },
                )
            }
            composable(DashDest.JOBS_CALENDAR) {
                JobsCalendarScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { id -> navController.navigate(DashDest.jobDetail(id)) },
                )
            }
            composable(DashDest.CLIENTS) {
                ClientsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenClient = { id -> navController.navigate(DashDest.clientEdit(id)) },
                    onAdd = { navController.navigate(DashDest.clientEdit()) },
                )
            }
            composable(DashDest.EXPENSES) {
                ExpensesScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(DashDest.ADD_EXPENSE) },
                )
            }
            composable(DashDest.ADD_EXPENSE) {
                AddExpenseScreen(onClose = { navController.popBackStack() })
            }
            composable(DashDest.MONEY) {
                MoneyScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.TAKEOFF) {
                TakeoffScreen(
                    onBack = { navController.popBackStack() },
                    onCreateEstimate = {
                        navController.navigate(DashDest.quoteBuilder(invoice = false)) {
                            popUpTo(DashDest.TAKEOFF) { inclusive = true }
                        }
                    },
                )
            }
            composable(DashDest.BANK) {
                BankScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.SUBSCRIPTION) {
                SubscriptionsScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.PROFILE_EDIT) {
                ProfileEditScreen(onClose = { navController.popBackStack() })
            }

            val idArg = listOf(navArgument(DashDest.ARG_ID) { type = NavType.StringType })
            composable(DashDest.JOB_DETAIL, arguments = idArg) {
                JobDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.jobEdit(id)) },
                )
            }
            composable(DashDest.ESTIMATE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.quoteBuilder(id = id)) },
                )
            }
            composable(DashDest.INVOICE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.quoteBuilder(id = id)) },
                )
            }

            // ── Create / edit screens ───────────────────────────────────────────
            val optionalId = navArgument(DashDest.ARG_ID) {
                type = NavType.StringType; defaultValue = ""
            }
            composable(
                DashDest.QUOTE_BUILDER,
                arguments = listOf(
                    optionalId,
                    navArgument(DashDest.ARG_INVOICE) { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                QuoteBuilderScreen(onClose = { navController.popBackStack() })
            }
            composable(
                DashDest.JOB_EDIT,
                arguments = listOf(
                    optionalId,
                    navArgument(DashDest.ARG_QUOTE_ID) { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                JobEditScreen(onClose = { navController.popBackStack() })
            }
            composable(DashDest.CLIENT_EDIT, arguments = listOf(optionalId)) {
                ClientEditScreen(onClose = { navController.popBackStack() })
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
