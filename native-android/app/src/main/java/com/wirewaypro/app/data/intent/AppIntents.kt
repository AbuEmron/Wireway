package com.wirewaypro.app.data.intent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-process bridge between [com.wirewaypro.app.MainActivity]'s intent handling
 * (launcher shortcuts, share-sheet, deep links) and the dashboard's nested
 * NavHost. The Activity parses an inbound intent into a route string and parks it
 * here; the dashboard collects it once it's mounted (i.e. once the user is signed
 * in) and navigates, then clears it. A [StateFlow] so a request that arrives
 * before the dashboard exists is replayed the moment it does.
 */
@Singleton
class AppIntents @Inject constructor() {

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /** Park a nested-nav route (a [com.wirewaypro.app.ui.navigation.DashDest] value). */
    fun requestNav(route: String) {
        _pendingRoute.value = route
    }

    /** Called by the dashboard after it has consumed the pending route. */
    fun consumeRoute() {
        _pendingRoute.value = null
    }
}
