package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DashboardUiState

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToFocus: () -> Unit,
    onNavigateToProtectedApps: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGuardianSettings: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToDeviceMgmt: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Bar Header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("dashboard_status_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isBlocked) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (state.isBlocked) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.size(12.dp)
                ) {}
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isBlocked) "🔴 Gaming limit reached" else "🟢 Gaming available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (state.isBlocked) Color(0xFF991B1B) else Color(0xFF065F46)
                )
            }
        }

        // Today's Gaming Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("todays_gaming_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Gaming",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val usedMinutes = state.totalUsedSeconds / 60
                val totalLimitMinutes = state.totalLimitSeconds / 60
                val usedHours = usedMinutes / 60
                val usedMinsRem = usedMinutes % 60
                val limitHours = totalLimitMinutes / 60
                val limitMinsRem = totalLimitMinutes % 60

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "🎮 %02dh %02dm".format(usedHours, usedMinsRem),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "of %02dh %02dm".format(limitHours, limitMinsRem),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (state.totalLimitSeconds > 0) {
                    (state.totalUsedSeconds.toFloat() / state.totalLimitSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (progress >= 1.0f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val remMins = state.remainingSeconds / 60
                    Text(
                        text = "Remaining: ${remMins / 60}h ${remMins % 60}m",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (state.earnedMinutes > 0) {
                        Text(
                            text = "+${state.earnedMinutes}m earned",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Focus Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("📚 ${state.earnedMinutes}m", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sessions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${state.sessionCount}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Main Action Buttons
        Button(
            onClick = onNavigateToFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("dashboard_start_focus_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Focus Session", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToProtectedApps,
                modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_protected_apps_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AppBlocking, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apps")
            }

            OutlinedButton(
                onClick = onNavigateToStats,
                modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_stats_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stats")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToSchedule,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Nightlight, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule")
            }

            OutlinedButton(
                onClick = onNavigateToHealth,
                modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_health_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Protection")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToDeviceMgmt,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Device Admin")
            }

            OutlinedButton(
                onClick = onNavigateToGuardianSettings,
                modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_settings_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Settings")
            }
        }

        // Developer / Debug shortcut
        OutlinedButton(
            onClick = onNavigateToDebug,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dashboard_debug_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simulation & Developer Tools")
        }
    }
}
