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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wirewaypro.app.domain.model.AuthState
import com.wirewaypro.app.ui.auth.LoginScreen
import com.wirewaypro.app.ui.auth.SignUpScreen
import com.wirewaypro.app.ui.auth.WelcomeScreen
import com.wirewaypro.app.ui.dashboard.DashboardScreen
import com.wirewaypro.app.ui.navigation.Routes
import com.wirewaypro.app.ui.navigation.WirewayTransitions

/**
 * Root composable. Owns the top-level NavHost and keeps it in sync with the
 * Supabase session: a fresh sign-in jumps to the dashboard, a sign-out (or
 * expiry) returns to the signed-out Welcome screen. The persisted session means
 * a returning user lands straight on the dashboard, skipping the auth flow.
 */
@Composable
fun WirewayApp(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // While the persisted session is still loading, hold on a splash spinner so
    // we don't flash the login screen at an already-signed-in user.
    if (authState is AuthState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isAuthed = authState is AuthState.Authenticated
    val startRoute = if (isAuthed) Routes.DASHBOARD else Routes.WELCOME

    // React to auth transitions that happen while the app is open. Signed-out
    // users land on Welcome (the auth entry point); Login/Sign Up branch off it.
    //
    // Guard on the current destination so this only fires on a genuine auth
    // *transition* — not on every recomposition or Activity recreation. Blindly
    // navigating here would reset the restored back stack (and the dashboard's
    // nested nav state) on an in-memory resume, sending the user back to the
    // start screen. rememberNavController restores the previous screen/tab, and
    // the NavHost's startDestination already lands a cold start on the right
    // side, so we only correct a real mismatch (e.g. mid-session sign-in/out).
    // A null route means the NavHost hasn't composed yet — leave it to the
    // startDestination rather than force-navigating.
    LaunchedEffect(isAuthed, currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        val onAuthedSide = route == Routes.DASHBOARD
        if (isAuthed == onAuthedSide) return@LaunchedEffect

        val target = if (isAuthed) Routes.DASHBOARD else Routes.WELCOME
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        // Auth surfaces are siblings — fade-through keeps sign-in calm.
        enterTransition = WirewayTransitions.tabEnter,
        exitTransition = WirewayTransitions.tabExit,
        popEnterTransition = WirewayTransitions.tabEnter,
        popExitTransition = WirewayTransitions.tabExit,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.SIGNUP) },
                onSignIn = { navController.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                // Toggle to Sign Up, replacing Login so Back returns to Welcome.
                onCreateAccount = {
                    navController.navigate(Routes.SIGNUP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
            // A successful sign-in routes to the dashboard via the auth-state effect.
        }
        composable(Routes.SIGNUP) {
            SignUpScreen(
                // Toggle to Login, replacing Sign Up so Back returns to Welcome.
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            BiometricGate(onUsePassword = sessionViewModel::signOut) {
                DashboardScreen()
            }
        }
    }
}
