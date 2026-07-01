package com.wirewaypro.app.ui.dashboard

import androidx.lifecycle.ViewModel
import com.wirewaypro.app.data.intent.AppIntents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Surfaces the route requested by an inbound intent (launcher shortcut / share
 * sheet) so the dashboard can drive its nested NavHost once it's on screen. Kept
 * separate from [DashboardViewModel] so collecting it doesn't re-trigger the
 * home-data load.
 */
@HiltViewModel
class IntentRouterViewModel @Inject constructor(
    private val appIntents: AppIntents,
) : ViewModel() {

    val pendingRoute: StateFlow<String?> = appIntents.pendingRoute

    fun onRouteHandled() = appIntents.consumeRoute()
}
