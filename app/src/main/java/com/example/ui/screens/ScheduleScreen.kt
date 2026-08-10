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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.entities.AppSettingsEntity

@Composable
fun ScheduleScreen(
    appSettings: AppSettingsEntity,
    onSaveSettings: (AppSettingsEntity) -> Unit
) {
    var enabled by remember { mutableStateOf(appSettings.globalNightLockEnabled) }
    var startHour by remember { mutableStateOf(appSettings.nightLockStartHour.toString()) }
    var startMin by remember { mutableStateOf(appSettings.nightLockStartMinute.toString()) }
    var endHour by remember { mutableStateOf(appSettings.nightLockEndHour.toString()) }
    var endMin by remember { mutableStateOf(appSettings.nightLockEndMinute.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("schedule_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Night Gaming Lock Schedule",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Sleep Schedule Lock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Block games automatically during night hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (enabled) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Lock Start Time (24h format):", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { startHour = it },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startMin,
                            onValueChange = { startMin = it },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Lock End Time (24h format):", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { endHour = it },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endMin,
                            onValueChange = { endMin = it },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val updated = appSettings.copy(
                    globalNightLockEnabled = enabled,
                    nightLockStartHour = startHour.toIntOrNull() ?: 22,
                    nightLockStartMinute = startMin.toIntOrNull() ?: 30,
                    nightLockEndHour = endHour.toIntOrNull() ?: 7,
                    nightLockEndMinute = endMin.toIntOrNull() ?: 0
                )
                onSaveSettings(updated)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Schedule Settings")
        }
    }
}
