package com.fuerz4.assistant.presentation.navigation

sealed class Destinations(val route: String) {
    data object Login : Destinations("login")
    data object Register : Destinations("register")
    data object ForgotPassword : Destinations("forgot_password")

    data object Home : Destinations("home")
    data object Profile : Destinations("profile")
    data object Devices : Destinations("devices")

    data object DeviceTypePicker : Destinations("device_type_picker")
    data object DeviceForm : Destinations("device_form/{deviceType}") {
        fun createRoute(deviceType: String) = "device_form/$deviceType"
    }
}

val bottomNavDestinations = listOf(Destinations.Home, Destinations.Devices, Destinations.Profile)
