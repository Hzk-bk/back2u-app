package com.hzk.back2u.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hzk.back2u.ui.auth.AuthViewModel
import com.hzk.back2u.ui.auth.LoginScreen
import com.hzk.back2u.ui.auth.RegisterScreen
import com.hzk.back2u.ui.home.HomeScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNavHost(viewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Auto-login: start on home if a user session already exists
    val startDestination = remember {
        if (viewModel.isLoggedIn.value) Routes.HOME else Routes.LOGIN
    }

    // Navigation reacts to the login state; screens never decide this themselves
    LaunchedEffect(isLoggedIn) {
        val current = navController.currentDestination?.route ?: return@LaunchedEffect
        val target = when {
            isLoggedIn && current != Routes.HOME -> Routes.HOME
            !isLoggedIn && current == Routes.HOME -> Routes.LOGIN
            else -> null
        }
        if (target != null) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onRegisterClick = {
                    viewModel.resetForm()
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = viewModel,
                onLoginClick = {
                    viewModel.resetForm()
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(viewModel = viewModel)
        }
    }
}