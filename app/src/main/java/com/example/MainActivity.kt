package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.Destinations
import com.example.ui.FocusGuardApp
import com.example.ui.FocusGuardViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FocusGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateTo = intent.getStringExtra("navigate_to")
        val initialRoute = if (navigateTo == "focus_session") Destinations.FOCUS_SESSION else Destinations.DASHBOARD

        setContent {
            MyApplicationTheme {
                FocusGuardApp(
                    viewModel = viewModel,
                    initialRoute = initialRoute
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHealthStatus()
    }
}


