package com.fuerz4.assistant.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fuerz4.assistant.R
import com.fuerz4.assistant.presentation.theme.NaranjaClaro
import com.fuerz4.assistant.presentation.theme.NaranjaIndicador

@Composable
fun Fuerz4BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar(containerColor = NaranjaClaro) {
        bottomNavDestinations.forEach { destination ->
            val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon(), contentDescription = null) },
                label = { Text(stringResource(destination.labelRes())) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = NaranjaIndicador)
            )
        }
    }
}

private fun Destinations.icon() = when (this) {
    Destinations.Home -> Icons.AutoMirrored.Filled.Chat
    Destinations.Devices -> Icons.Filled.Router
    Destinations.Profile -> Icons.Filled.Person
    else -> Icons.AutoMirrored.Filled.Chat
}

private fun Destinations.labelRes() = when (this) {
    Destinations.Home -> R.string.nav_home
    Destinations.Devices -> R.string.nav_devices
    Destinations.Profile -> R.string.nav_profile
    else -> R.string.nav_home
}
