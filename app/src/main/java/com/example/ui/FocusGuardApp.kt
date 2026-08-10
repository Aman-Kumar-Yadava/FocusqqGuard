package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebugScreen
import com.example.ui.screens.DeviceManagementScreen
import com.example.ui.screens.GuardianSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProtectedAppsScreen
import com.example.ui.screens.ProtectionHealthScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.StatisticsScreen
import kotlinx.coroutines.launch

object Destinations {
    const val DASHBOARD = "dashboard"
    const val PROTECTED_APPS = "protected_apps"
    const val STATISTICS = "statistics"
    const val HEALTH = "health"
    const val DEVICE_MGMT = "device_mgmt"
    const val SCHEDULE = "schedule"
    const val GUARDIAN_SETTINGS = "guardian_settings"
    const val ONBOARDING = "onboarding"
    const val DEBUG = "debug"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGuardApp(
    viewModel: FocusGuardViewModel
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destinations.DASHBOARD

    val startRoute = if (!dashboardState.appSettings.isPinSet) Destinations.ONBOARDING else Destinations.DASHBOARD

    // Global Nav Guard State
    var targetDestination by remember { mutableStateOf<String?>(null) }
    var showNavPinDialog by remember { mutableStateOf(false) }
    var navPinInput by remember { mutableStateOf("") }
    var navPinError by remember { mutableStateOf<String?>(null) }

    val navigateWithGuard: (String) -> Unit = { dest ->
        val sensitiveRoutes = listOf(
            Destinations.PROTECTED_APPS,
            Destinations.SCHEDULE,
            Destinations.DEVICE_MGMT,
            Destinations.GUARDIAN_SETTINGS
        )
        val isPinRequired = dashboardState.appSettings.requirePinOnOpen &&
                dashboardState.appSettings.isPinSet &&
                dest in sensitiveRoutes

        if (isPinRequired) {
            targetDestination = dest
            navPinInput = ""
            navPinError = null
            showNavPinDialog = true
        } else {
            if (dest == Destinations.HEALTH) {
                viewModel.refreshHealthStatus()
            }
            navController.navigate(dest)
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute != Destinations.ONBOARDING) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentRoute) {
                                Destinations.DASHBOARD -> "FocusGuard"
                                Destinations.PROTECTED_APPS -> "Protected Apps"
                                Destinations.STATISTICS -> "Statistics"
                                Destinations.HEALTH -> "Protection Setup"
                                Destinations.DEVICE_MGMT -> "Device Management"
                                Destinations.SCHEDULE -> "Night Schedule"
                                Destinations.GUARDIAN_SETTINGS -> "Guardian Settings"
                                Destinations.DEBUG -> "Developer Tools"
                                else -> "FocusGuard"
                            }
                        )
                    },
                    navigationIcon = {
                        if (currentRoute != Destinations.DASHBOARD) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute in listOf(
                    Destinations.DASHBOARD,
                    Destinations.PROTECTED_APPS,
                    Destinations.HEALTH
                )
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Destinations.DASHBOARD,
                        onClick = { navigateWithGuard(Destinations.DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.PROTECTED_APPS,
                        onClick = { navigateWithGuard(Destinations.PROTECTED_APPS) },
                        icon = { Icon(Icons.Default.AppBlocking, contentDescription = "Apps") },
                        label = { Text("Apps") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.HEALTH,
                        onClick = { navigateWithGuard(Destinations.HEALTH) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Protection") },
                        label = { Text("Health") }
                    )
                }
            }
        },
        modifier = Modifier.testTag("focus_guard_scaffold")
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.DASHBOARD) {
                DashboardScreen(
                    state = dashboardState,
                    onNavigateToProtectedApps = { navigateWithGuard(Destinations.PROTECTED_APPS) },
                    onNavigateToStats = { navigateWithGuard(Destinations.STATISTICS) },
                    onNavigateToSchedule = { navigateWithGuard(Destinations.SCHEDULE) },
                    onNavigateToGuardianSettings = { navigateWithGuard(Destinations.GUARDIAN_SETTINGS) },
                    onNavigateToHealth = { navigateWithGuard(Destinations.HEALTH) },
                    onNavigateToDeviceMgmt = { navigateWithGuard(Destinations.DEVICE_MGMT) },
                    onNavigateToDebug = { navigateWithGuard(Destinations.DEBUG) }
                )
            }

            composable(Destinations.PROTECTED_APPS) {
                ProtectedAppsScreen(
                    protectedApps = dashboardState.protectedApps,
                    isPinSet = dashboardState.appSettings.isPinSet,
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onGetInstalledApps = { viewModel.getInstalledNonSystemApps() },
                    onSaveApp = { viewModel.saveProtectedApp(it) },
                    onDeleteApp = { viewModel.deleteProtectedApp(it) }
                )
            }

            composable(Destinations.STATISTICS) {
                StatisticsScreen(
                    todayUsedSeconds = dashboardState.totalUsedSeconds,
                    sessionCount = dashboardState.sessionCount
                )
            }

            composable(Destinations.HEALTH) {
                ProtectionHealthScreen(
                    health = healthStatus,
                    onRefresh = { viewModel.refreshHealthStatus() }
                )
            }

            composable(Destinations.DEVICE_MGMT) {
                DeviceManagementScreen(
                    appSettings = dashboardState.appSettings,
                    devicePolicyWrapper = viewModel.devicePolicyWrapper,
                    onSetUninstallationBlocked = { viewModel.setAppUninstallationBlocked(it) },
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onRemoveDeviceOwner = { viewModel.removeDeviceOwner() }
                )
            }

            composable(Destinations.SCHEDULE) {
                ScheduleScreen(
                    appSettings = dashboardState.appSettings,
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onSaveSettings = { viewModel.updateSettings(it) }
                )
            }

            composable(Destinations.GUARDIAN_SETTINGS) {
                GuardianSettingsScreen(
                    appSettings = dashboardState.appSettings,
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onSetPin = { viewModel.setGuardianPin(it) },
                    onSaveSettings = { viewModel.updateSettings(it) }
                )
            }

            composable(Destinations.ONBOARDING) {
                val context = androidx.compose.ui.platform.LocalContext.current
                OnboardingScreen(
                    health = healthStatus,
                    isPinSet = dashboardState.appSettings.isPinSet,
                    onSetPin = { viewModel.setGuardianPin(it) },
                    onCompleteOnboarding = {
                        com.example.worker.EnforcementWorker.runImmediate(context)
                        navController.navigate(Destinations.DASHBOARD) {
                            popUpTo(Destinations.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.DEBUG) {
                DebugScreen(
                    simulatedUsageSeconds = dashboardState.appSettings.simulatedUsageSeconds,
                    isDebugSimulationEnabled = dashboardState.appSettings.isDebugSimulationEnabled,
                    onAddSimulatedMinutes = { viewModel.incrementSimulatedUsage(it) },
                    onResetSimulation = { viewModel.resetSimulatedUsage() }
                )
            }
        }
    }

    if (showNavPinDialog) {
        AlertDialog(
            onDismissRequest = { showNavPinDialog = false },
            title = { Text("Guardian PIN Required") },
            text = {
                Column {
                    Text("Enter Guardian PIN to open application settings:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = navPinInput,
                        onValueChange = { navPinInput = it; navPinError = null },
                        label = { Text("Guardian PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("nav_pin_input")
                    )
                    if (navPinError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(navPinError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val verified = viewModel.verifyPin(navPinInput)
                            if (verified) {
                                showNavPinDialog = false
                                targetDestination?.let { dest ->
                                    if (dest == Destinations.HEALTH) {
                                        viewModel.refreshHealthStatus()
                                    }
                                    navController.navigate(dest)
                                }
                            } else {
                                navPinError = "Incorrect Guardian PIN."
                            }
                        }
                    }
                ) {
                    Text("Unlock & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNavPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
