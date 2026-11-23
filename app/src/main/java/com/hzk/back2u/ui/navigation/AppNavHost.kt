package com.hzk.back2u.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hzk.back2u.data.AuthRepository
import com.hzk.back2u.data.ItemRepository
import com.hzk.back2u.ui.auth.AuthViewModel
import com.hzk.back2u.ui.auth.LoginScreen
import com.hzk.back2u.ui.auth.RegisterScreen
import com.hzk.back2u.ui.items.ItemFormScreen
import com.hzk.back2u.ui.items.ItemListScreen
import com.hzk.back2u.ui.items.ItemListViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LIST = "items"
    const val FORM = "form?itemId={itemId}"

    fun form(itemId: String? = null) = if (itemId == null) "form" else "form?itemId=$itemId"
}

private val authRoutes = setOf(Routes.LOGIN, Routes.REGISTER)

// The data layer is passed in from outside (see MainActivity), so the UI never creates it.
@Composable
fun AppNavHost(authRepository: AuthRepository, itemRepository: ItemRepository) {
    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    )
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Auto-login: start on the list if a user session already exists
    val startDestination = remember {
        if (authViewModel.isLoggedIn.value) Routes.LIST else Routes.LOGIN
    }

    // Navigation reacts to the login state; screens never decide this themselves
    LaunchedEffect(isLoggedIn) {
        val current = navController.currentDestination?.route ?: return@LaunchedEffect
        val target = when {
            isLoggedIn && current in authRoutes -> Routes.LIST
            !isLoggedIn && current !in authRoutes -> Routes.LOGIN
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
                viewModel = authViewModel,
                onRegisterClick = {
                    authViewModel.resetForm()
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onLoginClick = {
                    authViewModel.resetForm()
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.LIST) {
            // Created inside this destination, so it is destroyed (and the listener closed)
            // when the list leaves the back stack, for example on logout.
            val listViewModel: ItemListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ItemListViewModel(itemRepository) }
                }
            )
            ItemListScreen(
                viewModel = listViewModel,
                onLogout = { authViewModel.logout() },
                onAddClick = { navController.navigate(Routes.form()) },
                onItemClick = { id -> navController.navigate(Routes.form(id)) }
            )
        }
        composable(
            route = Routes.FORM,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            ItemFormScreen(itemId = entry.arguments?.getString("itemId"))
        }
    }
}