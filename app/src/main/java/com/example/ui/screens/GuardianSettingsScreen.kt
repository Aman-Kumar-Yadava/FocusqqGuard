package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.db.entities.AppSettingsEntity
import kotlinx.coroutines.launch

@Composable
fun GuardianSettingsScreen(
    appSettings: AppSettingsEntity,
    onVerifyPin: suspend (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("guardian_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Guardian Settings & Authentication",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (appSettings.isPinSet) "Guardian PIN Status: Active 🔒" else "Guardian PIN Status: Not Set 🔓",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (appSettings.isPinSet) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it; pinError = null; successMsg = null },
                        label = { Text("Current Guardian PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("guardian_current_pin_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it; pinError = null; successMsg = null },
                    label = { Text("New 4-Digit Guardian PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("guardian_pin_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it; pinError = null; successMsg = null },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("guardian_confirm_pin_input")
                )

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(pinError!!, color = MaterialTheme.colorScheme.error)
                }

                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(successMsg!!, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            pinError = "PIN must be at least 4 digits."
                        } else if (newPin != confirmPin) {
                            pinError = "PINs do not match."
                        } else {
                            scope.launch {
                                if (appSettings.isPinSet) {
                                    val verified = onVerifyPin(currentPin)
                                    if (!verified) {
                                        pinError = "Current Guardian PIN is incorrect."
                                        return@launch
                                    }
                                }
                                onSetPin(newPin)
                                currentPin = ""
                                newPin = ""
                                confirmPin = ""
                                successMsg = "Guardian PIN successfully updated!"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Guardian PIN")
                }
            }
        }
    }
}
