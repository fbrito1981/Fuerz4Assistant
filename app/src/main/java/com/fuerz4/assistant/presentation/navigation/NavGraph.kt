package com.fuerz4.assistant.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.fuerz4.assistant.presentation.auth.ForgotPasswordScreen
import com.fuerz4.assistant.presentation.auth.LoginScreen
import com.fuerz4.assistant.presentation.auth.RegisterScreen
import com.fuerz4.assistant.presentation.chat.ChatScreen
import com.fuerz4.assistant.presentation.devices.DeviceFormScreen
import com.fuerz4.assistant.presentation.devices.DeviceTypePickerScreen
import com.fuerz4.assistant.presentation.devices.DevicesListScreen
import com.fuerz4.assistant.presentation.profile.ProfileScreen
import com.fuerz4.assistant.presentation.splash.SplashScreen

@Composable
fun Fuerz4NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Splash.route
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = bottomNavDestinations.any { destination ->
        currentRoute?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Fuerz4BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.Splash.route) {
                SplashScreen(
                    onNavigateHome = {
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateLogin = {
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(Destinations.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destinations.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Destinations.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Destinations.ForgotPassword.route) }
                )
            }
            composable(Destinations.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Destinations.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onResetSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Destinations.Home.route) {
                ChatScreen()
            }
            composable(Destinations.Profile.route) {
                ProfileScreen(
                    onLoggedOut = {
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destinations.Devices.route) {
                DevicesListScreen(
                    onAddDevice = { navController.navigate(Destinations.DeviceTypePicker.route) },
                    onEditDevice = { device ->
                        val deviceType = device.type?.wireValue ?: "energy"
                        navController.navigate(Destinations.DeviceForm.createRoute(deviceType, device.uuid))
                    }
                )
            }

            composable(Destinations.DeviceTypePicker.route) {
                DeviceTypePickerScreen(
                    onTypeSelected = { type ->
                        navController.navigate(Destinations.DeviceForm.createRoute(type))
                    }
                )
            }
            composable(
                route = Destinations.DeviceForm.route,
                arguments = listOf(
                    navArgument("deviceType") { },
                    navArgument("uuid") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val deviceType = entry.arguments?.getString("deviceType").orEmpty()
                DeviceFormScreen(
                    deviceType = deviceType,
                    onProvisioned = {
                        navController.navigate(Destinations.Devices.route) {
                            popUpTo(Destinations.Devices.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
