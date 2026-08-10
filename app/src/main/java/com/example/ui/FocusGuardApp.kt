package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    viewModel: FocusGuardViewModel,
    initialRoute: String = Destinations.DASHBOARD
) {
    val navController = rememberNavController()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destinations.DASHBOARD

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
                                Destinations.HEALTH -> "Protection Health"
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
                        onClick = { navController.navigate(Destinations.DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.PROTECTED_APPS,
                        onClick = { navController.navigate(Destinations.PROTECTED_APPS) },
                        icon = { Icon(Icons.Default.AppBlocking, contentDescription = "Apps") },
                        label = { Text("Apps") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.HEALTH,
                        onClick = {
                            viewModel.refreshHealthStatus()
                            navController.navigate(Destinations.HEALTH)
                        },
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
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.DASHBOARD) {
                DashboardScreen(
                    state = dashboardState,
                    onNavigateToProtectedApps = { navController.navigate(Destinations.PROTECTED_APPS) },
                    onNavigateToStats = { navController.navigate(Destinations.STATISTICS) },
                    onNavigateToSchedule = { navController.navigate(Destinations.SCHEDULE) },
                    onNavigateToGuardianSettings = { navController.navigate(Destinations.GUARDIAN_SETTINGS) },
                    onNavigateToHealth = {
                        viewModel.refreshHealthStatus()
                        navController.navigate(Destinations.HEALTH)
                    },
                    onNavigateToDeviceMgmt = { navController.navigate(Destinations.DEVICE_MGMT) },
                    onNavigateToDebug = { navController.navigate(Destinations.DEBUG) }
                )
            }

            composable(Destinations.PROTECTED_APPS) {
                ProtectedAppsScreen(
                    protectedApps = dashboardState.protectedApps,
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
                    devicePolicyWrapper = viewModel.devicePolicyWrapper
                )
            }

            composable(Destinations.SCHEDULE) {
                ScheduleScreen(
                    appSettings = dashboardState.appSettings,
                    onSaveSettings = { viewModel.updateSettings(it) }
                )
            }

            composable(Destinations.GUARDIAN_SETTINGS) {
                GuardianSettingsScreen(
                    appSettings = dashboardState.appSettings,
                    onSetPin = { viewModel.setGuardianPin(it) },
                    onSaveSettings = { viewModel.updateSettings(it) }
                )
            }

            composable(Destinations.ONBOARDING) {
                OnboardingScreen(
                    health = healthStatus,
                    onCompleteOnboarding = {
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
}
