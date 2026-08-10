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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.db.entities.ProtectedAppEntity

@Composable
fun ProtectedAppsScreen(
    protectedApps: List<ProtectedAppEntity>,
    onSaveApp: (ProtectedAppEntity) -> Unit,
    onDeleteApp: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAppForEdit by remember { mutableStateOf<ProtectedAppEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("protected_apps_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Protected Apps",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Manage gaming apps and limit allowances",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(protectedApps) { app ->
                ProtectedAppCard(
                    app = app,
                    onEdit = { selectedAppForEdit = app },
                    onToggleEnabled = { enabled ->
                        onSaveApp(app.copy(isEnabled = enabled))
                    },
                    onDelete = { onDeleteApp(app.packageName) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddOrEditAppDialog(
            existingApp = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { app ->
                onSaveApp(app)
                showAddDialog = false
            }
        )
    }

    selectedAppForEdit?.let { app ->
        AddOrEditAppDialog(
            existingApp = app,
            onDismiss = { selectedAppForEdit = null },
            onConfirm = { updatedApp ->
                onSaveApp(updatedApp)
                selectedAppForEdit = null
            }
        )
    }
}

@Composable
fun ProtectedAppCard(
    app: ProtectedAppEntity,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("protected_app_card_${app.packageName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Limit: ${app.dailyLimitMinutes} mins (${app.dailyLimitMinutes / 60}h ${app.dailyLimitMinutes % 60}m)")
                    Text("Continuous Break: ${app.continuousLimitMinutes} mins")
                    Text("Allowed Hours: %02d:%02d - %02d:%02d".format(
                        app.allowedStartHour, app.allowedStartMinute,
                        app.allowedEndHour, app.allowedEndMinute
                    ))
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    if (app.packageName != "com.dts.freefireth") {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditAppDialog(
    existingApp: ProtectedAppEntity?,
    onDismiss: () -> Unit,
    onConfirm: (ProtectedAppEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingApp?.displayName ?: "Free Fire") }
    var pkg by remember { mutableStateOf(existingApp?.packageName ?: "com.dts.freefireth") }
    var dailyLimit by remember { mutableStateOf(existingApp?.dailyLimitMinutes?.toString() ?: "120") }
    var continuousLimit by remember { mutableStateOf(existingApp?.continuousLimitMinutes?.toString() ?: "45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingApp == null) "Add Protected App" else "Edit Limit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package Name") },
                    enabled = existingApp == null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dailyLimit,
                    onValueChange = { dailyLimit = it },
                    label = { Text("Daily Limit (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = continuousLimit,
                    onValueChange = { continuousLimit = it },
                    label = { Text("Continuous Limit (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val app = ProtectedAppEntity(
                        packageName = pkg.trim(),
                        displayName = name.trim(),
                        dailyLimitMinutes = dailyLimit.toIntOrNull() ?: 120,
                        continuousLimitMinutes = continuousLimit.toIntOrNull() ?: 45
                    )
                    onConfirm(app)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
