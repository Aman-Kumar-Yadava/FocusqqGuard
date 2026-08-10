package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.FocusGuardApp
import com.example.ui.FocusGuardViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FocusGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.worker.EnforcementWorker.schedulePeriodic(this)
        com.example.worker.EnforcementWorker.runImmediate(this)

        setContent {
            MyApplicationTheme {
                FocusGuardApp(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHealthStatus()
        com.example.worker.EnforcementWorker.runImmediate(this)
    }

}
