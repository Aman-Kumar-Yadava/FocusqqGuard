package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DebugScreen(
    simulatedUsageSeconds: Long,
    isDebugSimulationEnabled: Boolean,
    onAddSimulatedMinutes: (Long) -> Unit,
    onResetSimulation: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("debug_simulation_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Developer & Simulation Tools",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Free Fire Usage Simulation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val mins = simulatedUsageSeconds / 60
                Text("Current Simulated Usage: ${mins / 60}h ${mins % 60}m (${simulatedUsageSeconds}s)")
                Text("Simulation Active: $isDebugSimulationEnabled")

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onAddSimulatedMinutes(1L) },
                        modifier = Modifier.weight(1f).testTag("sim_add_1m_button")
                    ) {
                        Text("+1 min")
                    }

                    Button(
                        onClick = { onAddSimulatedMinutes(15L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+15 min")
                    }

                    Button(
                        onClick = { onAddSimulatedMinutes(60L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1 hour")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onAddSimulatedMinutes(120L) },
                    modifier = Modifier.fillMaxWidth().testTag("sim_trigger_limit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Trigger Daily Limit (120 min)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onResetSimulation,
                    modifier = Modifier.fillMaxWidth().testTag("sim_reset_button")
                ) {
                    Text("Reset Simulation")
                }
            }
        }
    }
}
