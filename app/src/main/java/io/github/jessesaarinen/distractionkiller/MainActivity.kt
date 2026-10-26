package io.github.jessesaarinen.distractionkiller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jessesaarinen.distractionkiller.ui.HomeScreen
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistractionKillerTheme {
                HomeScreen()
            }
        }
    }
}
