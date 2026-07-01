package com.wirewaypro.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wirewaypro.app.domain.model.AuthState
import com.wirewaypro.app.ui.auth.LoginScreen
import com.wirewaypro.app.ui.dashboard.DashboardScreen
import com.wirewaypro.app.ui.navigation.Routes

/**
 * Root composable. Owns the top-level NavHost and keeps it in sync with the
 * Supabase session: a fresh sign-in jumps to the dashboard, a sign-out (or
 * expiry) returns to login. The persisted session means a returning user lands
 * straight on the dashboard.
 */
@Composable
fun WirewayApp(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // While the persisted session is still loading, hold on a splash spinner so
    // we don't flash the login screen at an already-signed-in user.
    if (authState is AuthState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isAuthed = authState is AuthState.Authenticated
    val startRoute = if (isAuthed) Routes.DASHBOARD else Routes.LOGIN

    // React to auth transitions that happen while the app is open.
    LaunchedEffect(isAuthed) {
        val target = if (isAuthed) Routes.DASHBOARD else Routes.LOGIN
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Routes.LOGIN) {
            // Navigation away happens via the auth-state effect above.
            LoginScreen()
        }
        composable(Routes.DASHBOARD) {
            BiometricGate(onUsePassword = sessionViewModel::signOut) {
                DashboardScreen()
            }
        }
    }
}
