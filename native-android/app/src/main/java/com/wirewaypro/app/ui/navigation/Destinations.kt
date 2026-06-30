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

/**
 * Routes inside the dashboard's nested nav graph that are NOT bottom-nav tabs:
 * the Jobs/Clients lists reachable from Home, and the list→detail screens.
 */
object DashDest {
    const val JOBS = "jobs"
    const val JOBS_CALENDAR = "jobs_calendar"
    const val CLIENTS = "clients"
    const val EXPENSES = "expenses"
    const val ADD_EXPENSE = "add_expense"
    const val MILEAGE = "mileage"
    const val TIME_TRACKING = "time_tracking"
    const val NEC = "nec"
    const val MONEY = "money"
    const val TAKEOFF = "takeoff"
    const val AI_QUOTE_BUILDER = "ai_quote_builder"
    const val BANK = "bank"
    const val SUBSCRIPTION = "subscription"
    const val PROFILE_EDIT = "profile_edit"
    const val GET_PAID = "get_paid"

    const val JOB_DETAIL = "job/{id}"
    const val ESTIMATE_DETAIL = "estimate/{id}"
    const val INVOICE_DETAIL = "invoice/{id}"
    const val PULL_LIST = "pull_list/{id}"

    // Create / edit routes (query args; blank id = create).
    const val QUOTE_BUILDER = "quote_builder?id={id}&invoice={invoice}"
    const val JOB_EDIT = "job_edit?id={id}&quoteId={quoteId}"
    const val CLIENT_EDIT = "client_edit?id={id}"

    const val ARG_ID = "id"
    const val ARG_INVOICE = "invoice"
    const val ARG_QUOTE_ID = "quoteId"

    fun jobDetail(id: String) = "job/$id"
    fun estimateDetail(id: String) = "estimate/$id"
    fun invoiceDetail(id: String) = "invoice/$id"
    fun pullList(id: String) = "pull_list/$id"

    fun quoteBuilder(id: String? = null, invoice: Boolean = false) =
        "quote_builder?id=${id.orEmpty()}&invoice=$invoice"

    fun jobEdit(id: String? = null, quoteId: String? = null) =
        "job_edit?id=${id.orEmpty()}&quoteId=${quoteId.orEmpty()}"

    fun clientEdit(id: String? = null) = "client_edit?id=${id.orEmpty()}"
}
