package com.fuerz4.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fuerz4.assistant.presentation.navigation.Fuerz4NavGraph
import com.fuerz4.assistant.presentation.theme.Fuerz4Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Fuerz4Theme {
                Fuerz4NavGraph()
            }
        }
    }
}
