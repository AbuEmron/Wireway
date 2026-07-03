package com.wirewaypro.app.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.wirewaypro.app.ui.getpaid.GetPaidScreen
import com.wirewaypro.app.ui.jobs.JobDetailScreen
import com.wirewaypro.app.ui.jobs.JobEditScreen
import com.wirewaypro.app.ui.jobs.JobsScreen
import com.wirewaypro.app.ui.schedule.ScheduleScreen
import com.wirewaypro.app.ui.load.LoadAdvisorScreen
import com.wirewaypro.app.ui.mileage.MileageScreen
import com.wirewaypro.app.ui.navigation.DashDest
import com.wirewaypro.app.ui.nec.NecReferenceScreen
import com.wirewaypro.app.ui.timetracking.TimeTrackingScreen
import com.wirewaypro.app.ui.navigation.HomeTab
import com.wirewaypro.app.ui.quotes.AssembliesScreen
import com.wirewaypro.app.ui.quotes.EstimatesScreen
import com.wirewaypro.app.ui.quotes.InvoicesScreen
import com.wirewaypro.app.ui.money.MoneyScreen
import com.wirewaypro.app.ui.quotes.QuoteBuilderScreen
import com.wirewaypro.app.ui.quotes.MaterialPullListScreen
import com.wirewaypro.app.ui.quotes.QuoteDetailScreen
import com.wirewaypro.app.ui.settings.ProfileEditScreen
import com.wirewaypro.app.ui.settings.SettingsScreen
import com.wirewaypro.app.ui.subscription.SubscriptionsScreen
import com.wirewaypro.app.ui.takeoff.AiEstimateMode
import com.wirewaypro.app.ui.takeoff.TakeoffScreen
import com.wirewaypro.app.ui.tools.BoxFillCalcScreen
import com.wirewaypro.app.ui.tools.ConduitFillCalcScreen
import com.wirewaypro.app.ui.tools.DeratingCalcScreen
import com.wirewaypro.app.ui.tools.LaborCalcScreen
import com.wirewaypro.app.ui.tools.MaterialDatabaseScreen
import com.wirewaypro.app.ui.tools.ToolsScreen
import com.wirewaypro.app.ui.tools.VoltageDropCalcScreen
import com.wirewaypro.app.ui.tools.WireSizeCalcScreen

/**
 * The authenticated shell. A nested NavHost drives the four bottom-nav tabs plus
 * the Jobs/Clients lists and all list→detail screens, keeping the whole app in a
 * single Activity. The bottom bar is shown only on the top-level tabs; detail and
 * pushed screens get the full height and their own back-navigating top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    intentRouter: IntentRouterViewModel = hiltViewModel(),
) {
    com.wirewaypro.app.ui.NotificationsSetup()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = HomeTab.entries.map { it.route }.toSet()
    val showBottomBar = currentRoute in tabRoutes

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    val topBarTitle = HomeTab.entries.firstOrNull { it.route == currentRoute }?.label ?: "Wireway Pro"

    // Drive the nested NavHost from inbound intents (launcher shortcuts / share).
    val pendingRoute by intentRouter.pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            intentRouter.onRouteHandled()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBottomBar || drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onTab = { tab -> navController.navigateToTab(tab) },
                onDestination = { route -> navController.navigate(route) { launchSingleTop = true } },
                closeDrawer = { scope.launch { drawerState.close() } },
            )
        },
    ) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showBottomBar) {
                CenterAlignedTopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Open menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    HomeTab.entries.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Adaptive layout: on tablets/foldables, cap content width and center it so
        // single-column screens (forms especially) don't stretch edge to edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
        NavHost(
            navController = navController,
            startDestination = HomeTab.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
        ) {
            composable(HomeTab.HOME.route) {
                HomeScreen(
                    onOpenAiQuoteBuilder = { navController.navigate(DashDest.AI_QUOTE_BUILDER) },
                    onOpenJobs = { navController.navigate(DashDest.JOBS) },
                    onOpenClients = { navController.navigate(DashDest.CLIENTS) },
                    onOpenExpenses = { navController.navigate(DashDest.EXPENSES) },
                    onOpenMileage = { navController.navigate(DashDest.MILEAGE) },
                    onOpenTimeTracking = { navController.navigate(DashDest.TIME_TRACKING) },
                    onOpenNec = { navController.navigate(DashDest.NEC) },
                    onOpenLoadAdvisor = { navController.navigate(DashDest.LOAD_ADVISOR) },
                    onOpenMoney = { navController.navigate(DashDest.MONEY) },
                    onOpenTakeoff = { navController.navigate(DashDest.TAKEOFF) },
                    onOpenAssemblies = { navController.navigate(DashDest.ASSEMBLIES) },
                    onOpenBank = { navController.navigate(DashDest.BANK) },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                    onOpenTools = { navController.navigate(DashDest.TOOLS) },
                    onOpenMaterialDb = { navController.navigate(DashDest.MATERIAL_DB) },
                    onOpenLaborCalc = { navController.navigate(DashDest.LABOR_CALC) },
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
                    onGetPaid = { navController.navigate(DashDest.GET_PAID) },
                )
            }

            composable(DashDest.JOBS) {
                JobsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { id -> navController.navigate(DashDest.jobDetail(id)) },
                    onAdd = { navController.navigate(DashDest.jobEdit()) },
                    onOpenCalendar = { navController.navigate(DashDest.SCHEDULE) },
                )
            }
            composable(DashDest.SCHEDULE) {
                ScheduleScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { id -> navController.navigate(DashDest.jobDetail(id)) },
                    onAddForDate = { date -> navController.navigate(DashDest.jobEdit(date = date)) },
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
            composable(DashDest.MILEAGE) {
                MileageScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.TIME_TRACKING) {
                TimeTrackingScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.NEC) {
                NecReferenceScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.LOAD_ADVISOR) {
                LoadAdvisorScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.TOOLS) {
                ToolsScreen(
                    onBack = { navController.popBackStack() },
                    onWireSize = { navController.navigate(DashDest.CALC_WIRE_SIZE) },
                    onVoltageDrop = { navController.navigate(DashDest.CALC_VOLTAGE_DROP) },
                    onConduitFill = { navController.navigate(DashDest.CALC_CONDUIT_FILL) },
                    onBoxFill = { navController.navigate(DashDest.CALC_BOX_FILL) },
                    onDerating = { navController.navigate(DashDest.CALC_DERATING) },
                    onMaterialDb = { navController.navigate(DashDest.MATERIAL_DB) },
                    onLaborCalc = { navController.navigate(DashDest.LABOR_CALC) },
                    onNec = { navController.navigate(DashDest.NEC) },
                    onLoadAdvisor = { navController.navigate(DashDest.LOAD_ADVISOR) },
                )
            }
            composable(DashDest.CALC_WIRE_SIZE) {
                WireSizeCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.CALC_VOLTAGE_DROP) {
                VoltageDropCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.CALC_CONDUIT_FILL) {
                ConduitFillCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.CALC_BOX_FILL) {
                BoxFillCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.CALC_DERATING) {
                DeratingCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.MATERIAL_DB) {
                MaterialDatabaseScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(DashDest.LABOR_CALC) {
                LaborCalcScreen(onBack = { navController.popBackStack() })
            }
            composable(DashDest.TAKEOFF) {
                TakeoffScreen(
                    mode = AiEstimateMode.TAKEOFF,
                    onBack = { navController.popBackStack() },
                    onCreateEstimate = {
                        navController.navigate(DashDest.quoteBuilder(invoice = false)) {
                            popUpTo(DashDest.TAKEOFF) { inclusive = true }
                        }
                    },
                )
            }
            composable(DashDest.ASSEMBLIES) {
                AssembliesScreen(
                    onBack = { navController.popBackStack() },
                    onPicked = {
                        navController.navigate(DashDest.quoteBuilder(invoice = false)) {
                            popUpTo(DashDest.ASSEMBLIES) { inclusive = true }
                        }
                    },
                )
            }
            composable(DashDest.AI_QUOTE_BUILDER) {
                TakeoffScreen(
                    mode = AiEstimateMode.QUOTE_BUILDER,
                    onBack = { navController.popBackStack() },
                    onCreateEstimate = {
                        navController.navigate(DashDest.quoteBuilder(invoice = false)) {
                            popUpTo(DashDest.AI_QUOTE_BUILDER) { inclusive = true }
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
            composable(DashDest.GET_PAID) {
                GetPaidScreen(onBack = { navController.popBackStack() })
            }

            val idArg = listOf(navArgument(DashDest.ARG_ID) { type = NavType.StringType })
            composable(DashDest.JOB_DETAIL, arguments = idArg) {
                JobDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.jobEdit(id)) },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(DashDest.ESTIMATE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.quoteBuilder(id = id)) },
                    onPullList = { id -> navController.navigate(DashDest.pullList(id)) },
                    onOpenInvoice = { id ->
                        // Replace the estimate detail with the new invoice's detail.
                        navController.navigate(DashDest.invoiceDetail(id)) {
                            popUpTo(DashDest.ESTIMATE_DETAIL) { inclusive = true }
                        }
                    },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(DashDest.INVOICE_DETAIL, arguments = idArg) {
                QuoteDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(DashDest.quoteBuilder(id = id)) },
                    onPullList = { id -> navController.navigate(DashDest.pullList(id)) },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(DashDest.PULL_LIST, arguments = idArg) {
                MaterialPullListScreen(onBack = { navController.popBackStack() })
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
                QuoteBuilderScreen(
                    onClose = { navController.popBackStack() },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
            composable(
                DashDest.JOB_EDIT,
                arguments = listOf(
                    optionalId,
                    navArgument(DashDest.ARG_QUOTE_ID) { type = NavType.StringType; defaultValue = "" },
                    navArgument(DashDest.ARG_DATE) { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                JobEditScreen(onClose = { navController.popBackStack() })
            }
            composable(DashDest.CLIENT_EDIT, arguments = listOf(optionalId)) {
                ClientEditScreen(
                    onClose = { navController.popBackStack() },
                    onOpenSubscription = { navController.navigate(DashDest.SUBSCRIPTION) },
                )
            }
        }
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
