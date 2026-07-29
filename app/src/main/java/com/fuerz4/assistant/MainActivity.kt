package com.fuerz4.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fuerz4.assistant.data.session.SessionManager
import com.fuerz4.assistant.presentation.navigation.Destinations
import com.fuerz4.assistant.presentation.navigation.Fuerz4NavGraph
import com.fuerz4.assistant.presentation.theme.Fuerz4Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (sessionManager.isLoggedIn) Destinations.Home.route else Destinations.Login.route

        setContent {
            Fuerz4Theme {
                Fuerz4NavGraph(startDestination = startDestination)
            }
        }
    }
}
