package com.yodgorbek.nexora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yodgorbek.nexora.ai.NaturalLanguageCommandRequest
import com.yodgorbek.nexora.ai.StructuredDeviceCommand
import com.yodgorbek.nexora.ai.TelemetryAnalysisRequest
import com.yodgorbek.nexora.ai.TelemetryAnalysisResult
import com.yodgorbek.nexora.ble.AndroidBleManager
import com.yodgorbek.nexora.ble.simulator.SimulatorScenario
import com.yodgorbek.nexora.di.NexoraContainer
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.SyncState
import com.yodgorbek.nexora.domain.model.TimeRange
import com.yodgorbek.nexora.ui.screens.DeveloperScreen
import com.yodgorbek.nexora.ui.screens.DeviceDetailsScreen
import com.yodgorbek.nexora.ui.screens.DevicesScreen
import com.yodgorbek.nexora.ui.screens.HomeScreen
import com.yodgorbek.nexora.ui.screens.InsightsScreen
import com.yodgorbek.nexora.ui.screens.SettingsScreen
import com.yodgorbek.nexora.ui.screens.TelemetryScreen
import com.yodgorbek.nexora.ui.theme.NexoraDarkBackground
import com.yodgorbek.nexora.ui.theme.NexoraDarkSurfaceVariant
import com.yodgorbek.nexora.ui.theme.NexoraTeal
import com.yodgorbek.nexora.ui.theme.NexoraTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    DEVICES("Devices", Icons.Default.Devices),
    TELEMETRY("Telemetry", Icons.AutoMirrored.Filled.ShowChart),
    INSIGHTS("Insights", Icons.Default.AutoAwesome),
    DEVELOPER("Diagnostics", Icons.Default.BugReport),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val container = NexoraContainer.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register native Android BLE hardware manager
        container.setHardwareBleManager(AndroidBleManager(applicationContext))

        setContent {
            NexoraTheme(darkTheme = true) {
                MainAppScreen(container = container)
            }
        }
    }
}

@Composable
fun MainAppScreen(container: NexoraContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var selectedDeviceId by remember { mutableStateOf<String?>("SIM-NX-NORMAL-01") }
    var isScanning by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var telemetryJob by remember { mutableStateOf<Job?>(null) }

    // State collections from Container / Repositories
    val discoveredDevices by container.deviceRepository.getDiscoveredDevices().collectAsState(initial = emptyList())
    val connectedDevices by container.deviceRepository.getConnectedDevices().collectAsState(initial = emptyList())
    val isSimulatorMode by container.settingsRepository.isSimulatorModeEnabled().collectAsState(initial = true)
    val apiKey by container.settingsRepository.getGroqApiKey().collectAsState(initial = null)
    val modelName by container.settingsRepository.getGroqModel().collectAsState(initial = "llama-3.3-70b-versatile")

    // Filtered devices: In Simulator mode, show SIM-* devices. In Hardware mode, show ONLY real physical BLE devices.
    val filteredDiscoveredDevices = remember(discoveredDevices, isSimulatorMode) {
        if (isSimulatorMode) {
            discoveredDevices.filter { it.id.startsWith("SIM-") }
        } else {
            discoveredDevices.filterNot { it.id.startsWith("SIM-") }
        }
    }

    val currentDeviceId = selectedDeviceId 
        ?: filteredDiscoveredDevices.firstOrNull()?.id 
        ?: if (isSimulatorMode) "SIM-NX-NORMAL-01" else ""

    val connectionState by container.bleManager.observeConnectionState(currentDeviceId).collectAsState(initial = ConnectionState.Disconnected)
    val latestTelemetry by container.telemetryRepository.observeLatestTelemetry(currentDeviceId).collectAsState(initial = null)
    var selectedTimeRange by remember { mutableStateOf(TimeRange.FIVE_MINUTES) }
    val telemetryHistory by container.telemetryRepository.observeTelemetryHistory(currentDeviceId, selectedTimeRange).collectAsState(initial = emptyList())
    val anomalies by container.telemetryRepository.observeAnomalies(currentDeviceId).collectAsState(initial = emptyList())

    var deviceHealth by remember {
        mutableStateOf<DeviceHealth?>(null)
    }

    // Permission launcher for Android 12+ and location
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            isScanning = true
            scanJob?.cancel()
            scanJob = scope.launch {
                container.deviceRepository.clearDiscoveredDevices()
                try {
                    container.scanDevicesUseCase().collect { }
                } catch (_: Exception) {
                    isScanning = false
                }
            }
        } else {
            Toast.makeText(context, "Bluetooth scan permissions are required to discover physical devices.", Toast.LENGTH_LONG).show()
        }
    }

    fun startScanningWithPermissions() {
        if (isSimulatorMode) {
            isScanning = true
            scanJob?.cancel()
            scanJob = scope.launch {
                container.deviceRepository.clearDiscoveredDevices()
                try {
                    container.scanDevicesUseCase().collect { }
                } catch (_: Exception) {
                    isScanning = false
                }
            }
        } else {
            // Check real Bluetooth permissions
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

            val missing = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isEmpty()) {
                isScanning = true
                scanJob?.cancel()
                scanJob = scope.launch {
                    container.deviceRepository.clearDiscoveredDevices()
                    try {
                        container.scanDevicesUseCase().collect { }
                    } catch (_: Exception) {
                        isScanning = false
                    }
                }
            } else {
                permissionsLauncher.launch(requiredPermissions)
            }
        }
    }

    LaunchedEffect(isSimulatorMode) {
        container.setSimulatorActive(isSimulatorMode)
        container.deviceRepository.clearDiscoveredDevices()
        scanJob?.cancel()
        isScanning = false
        selectedDeviceId = if (isSimulatorMode) "SIM-NX-NORMAL-01" else null
    }

    LaunchedEffect(latestTelemetry, anomalies) {
        deviceHealth = container.healthCalculator.calculateHealth(
            deviceId = currentDeviceId,
            recentTelemetry = telemetryHistory,
            activeAnomalies = anomalies
        )
    }

    // AI States
    var isAnalyzingAi by remember { mutableStateOf(false) }
    var aiAnalysisResult by remember { mutableStateOf<TelemetryAnalysisResult?>(null) }
    var isTranslatingCommand by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<StructuredDeviceCommand?>(null) }

    // Developer Diagnostics State
    var currentScenario by remember { mutableStateOf(SimulatorScenario.NORMAL) }
    var localServerRunning by remember { mutableStateOf(false) }

    // Initial default scan
    LaunchedEffect(Unit) {
        startScanningWithPermissions()
    }

    // Auto-observe telemetry when connected
    LaunchedEffect(connectionState, currentDeviceId) {
        if (connectionState is ConnectionState.Connected) {
            telemetryJob?.cancel()
            telemetryJob = scope.launch {
                try {
                    container.observeTelemetryUseCase(currentDeviceId).collect { }
                } catch (_: Exception) { }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NexoraDarkSurfaceVariant,
                contentColor = NexoraTeal
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NexoraTeal,
                            indicatorColor = NexoraTeal,
                            unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NexoraDarkBackground)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        connectedDevices = connectedDevices,
                        latestTelemetry = latestTelemetry,
                        deviceHealth = deviceHealth,
                        onNavigateToDevice = { id ->
                            selectedDeviceId = id
                            selectedTab = AppTab.DEVICES
                        },
                        onNavigateToScanner = { selectedTab = AppTab.DEVICES },
                        onDisconnectDevice = { id ->
                            scope.launch { container.disconnectDeviceUseCase(id) }
                        }
                    )
                }

                AppTab.DEVICES -> {
                    val activeDevice = filteredDiscoveredDevices.find { it.id == selectedDeviceId }
                    if (activeDevice != null && connectionState is ConnectionState.Connected) {
                        DeviceDetailsScreen(
                            device = activeDevice,
                            details = null,
                            connectionState = connectionState,
                            latestTelemetry = latestTelemetry,
                            health = deviceHealth,
                            onBack = { selectedDeviceId = null },
                            onConnect = {
                                scope.launch { container.connectDeviceUseCase(activeDevice.id) }
                            },
                            onDisconnect = {
                                scope.launch { container.disconnectDeviceUseCase(activeDevice.id) }
                            },
                            onExecuteCommand = { cmd ->
                                scope.launch {
                                    container.executeDeviceCommandUseCase(
                                        deviceId = activeDevice.id,
                                        command = cmd,
                                        capabilities = activeDevice.capabilities,
                                        currentState = connectionState
                                    )
                                }
                            }
                        )
                    } else {
                        val connMap = filteredDiscoveredDevices.associate { it.id to (if (it.id == currentDeviceId) connectionState else ConnectionState.Disconnected) }
                        DevicesScreen(
                            isScanning = isScanning,
                            isSimulatorMode = isSimulatorMode,
                            discoveredDevices = filteredDiscoveredDevices,
                            connectionStates = connMap,
                            onToggleScan = {
                                if (isScanning) {
                                    scanJob?.cancel()
                                    isScanning = false
                                } else {
                                    startScanningWithPermissions()
                                }
                            },
                            onToggleSimulatorMode = { enabled ->
                                scope.launch { container.settingsRepository.setSimulatorModeEnabled(enabled) }
                            },
                            onConnect = { id ->
                                selectedDeviceId = id
                                scope.launch { container.connectDeviceUseCase(id) }
                            },
                            onDisconnect = { id ->
                                scope.launch { container.disconnectDeviceUseCase(id) }
                            },
                            onDeviceClick = { id ->
                                selectedDeviceId = id
                            }
                        )
                    }
                }

                AppTab.TELEMETRY -> {
                    TelemetryScreen(
                        deviceId = currentDeviceId,
                        history = telemetryHistory,
                        latest = latestTelemetry,
                        selectedRange = selectedTimeRange,
                        onSelectTimeRange = { selectedTimeRange = it }
                    )
                }

                AppTab.INSIGHTS -> {
                    val activeDevice = discoveredDevices.find { it.id == currentDeviceId }
                    InsightsScreen(
                        anomalies = anomalies,
                        isAnalyzing = isAnalyzingAi,
                        analysisResult = aiAnalysisResult,
                        isTranslatingCommand = isTranslatingCommand,
                        pendingCommand = pendingCommand,
                        onRequestAiAnalysis = {
                            scope.launch {
                                isAnalyzingAi = true
                                aiAnalysisResult = container.aiService.analyzeTelemetry(
                                    TelemetryAnalysisRequest(
                                        deviceId = currentDeviceId,
                                        deviceName = activeDevice?.name ?: "Nexora Node",
                                        firmwareVersion = "1.2.4",
                                        recentTelemetry = telemetryHistory,
                                        anomalies = anomalies,
                                        health = deviceHealth ?: container.healthCalculator.calculateHealth(currentDeviceId, telemetryHistory, anomalies)
                                    )
                                )
                                isAnalyzingAi = false
                            }
                        },
                        onSubmitPrompt = { prompt ->
                            scope.launch {
                                isTranslatingCommand = true
                                pendingCommand = container.interpretNaturalLanguageCommandUseCase(
                                    request = NaturalLanguageCommandRequest(
                                        deviceId = currentDeviceId,
                                        deviceName = activeDevice?.name ?: "Nexora Node",
                                        userPrompt = prompt,
                                        currentSamplingInterval = 2,
                                        isTelemetryEnabled = true
                                    ),
                                    capabilities = activeDevice?.capabilities ?: com.yodgorbek.nexora.domain.model.DeviceCapabilities.DEFAULT,
                                    currentState = connectionState
                                )
                                isTranslatingCommand = false
                            }
                        },
                        onConfirmCommand = { cmd ->
                            cmd.command?.let { realCmd ->
                                scope.launch {
                                    container.executeDeviceCommandUseCase(
                                        deviceId = cmd.targetDeviceId,
                                        command = realCmd,
                                        capabilities = activeDevice?.capabilities ?: com.yodgorbek.nexora.domain.model.DeviceCapabilities.DEFAULT,
                                        currentState = connectionState
                                    )
                                    pendingCommand = null
                                }
                            }
                        },
                        onDismissCommand = { pendingCommand = null }
                    )
                }

                AppTab.DEVELOPER -> {
                    DeveloperScreen(
                        currentScenario = currentScenario,
                        syncState = SyncState.OFFLINE,
                        localServerRunning = localServerRunning,
                        onSelectScenario = { scenario ->
                            currentScenario = scenario
                            selectedDeviceId = scenario.targetDeviceId
                            scope.launch { container.connectDeviceUseCase(scenario.targetDeviceId) }
                        },
                        onToggleLocalServer = { localServerRunning = it }
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        currentApiKey = apiKey,
                        currentModel = modelName,
                        onSaveApiKey = { newKey ->
                            scope.launch {
                                container.settingsRepository.setGroqApiKey(newKey)
                                container.updateAiConfig(newKey, modelName)
                            }
                        },
                        onSaveModel = { newModel ->
                            scope.launch {
                                container.settingsRepository.setGroqModel(newModel)
                                container.updateAiConfig(apiKey, newModel)
                            }
                        }
                    )
                }
            }
        }
    }
}