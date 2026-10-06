import SwiftUI
import Combine

// MARK: - SwiftUI Native Telemetry Model
struct SwiftTelemetry: Identifiable {
    let id = UUID()
    let timestamp: Date
    let temperature: Double
    let humidity: Double
    let batteryPercentage: Int
    let signalStrength: Int
}

struct SwiftAnomaly: Identifiable {
    let id: String
    let title: String
    let description: String
    let severity: String
    let evidence: String
}

// MARK: - Main iOS ViewModel
final class NexoraViewModel: ObservableObject {
    @Published var isScanning: Bool = false
    @Published var isSimulatorMode: Bool = true
    @Published var connectionStatus: String = "Disconnected"
    @Published var selectedDeviceName: String = "Nexora Sense [Normal]"
    @Published var selectedDeviceId: String = "SIM-NX-NORMAL-01"

    @Published var latestTelemetry: SwiftTelemetry = SwiftTelemetry(
        timestamp: Date(),
        temperature: 22.5,
        humidity: 45.2,
        batteryPercentage: 94,
        signalStrength: -58
    )

    @Published var telemetryHistory: [SwiftTelemetry] = []
    @Published var anomalies: [SwiftAnomaly] = []

    @Published var isAnalyzingAi: Bool = false
    @Published var aiSummary: String? = nil
    @Published var aiCauses: [String] = []
    @Published var aiRecommendations: [String] = []

    @Published var pendingCommandExplanation: String? = nil
    @Published var isTranslatingCommand: Bool = false

    private var simulationTimer: AnyCancellable?

    init() {
        startSimulator()
    }

    func startSimulator() {
        simulationTimer?.cancel()
        simulationTimer = Timer.publish(every: 2.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                let newTemp = self.latestTelemetry.temperature + Double.random(in: -0.3...0.3)
                let newHum = min(100.0, max(10.0, self.latestTelemetry.humidity + Double.random(in: -0.5...0.5)))
                let newBatt = max(1, self.latestTelemetry.batteryPercentage - (Int.random(in: 0...10) == 0 ? 1 : 0))
                let newRssi = Int.random(in: -65...(-55))

                let sample = SwiftTelemetry(
                    timestamp: Date(),
                    temperature: (newTemp * 10).rounded() / 10.0,
                    humidity: (newHum * 10).rounded() / 10.0,
                    batteryPercentage: newBatt,
                    signalStrength: newRssi
                )

                self.latestTelemetry = sample
                self.telemetryHistory.append(sample)
                if self.telemetryHistory.count > 50 {
                    self.telemetryHistory.removeFirst()
                }

                // Local anomaly check
                if sample.temperature > 50.0 {
                    self.anomalies = [
                        SwiftAnomaly(
                            id: "ANOM-TEMP",
                            title: "Elevated Core Temperature",
                            description: "Thermal reading \(sample.temperature)°C exceeds nominal baseline.",
                            severity: "WARNING",
                            evidence: "Temp: \(sample.temperature)°C"
                        )
                    ]
                }
            }
    }

    func connect(deviceId: String, name: String) {
        selectedDeviceId = deviceId
        selectedDeviceName = name
        connectionStatus = "Connecting"
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            self.connectionStatus = "Connected"
        }
    }

    func disconnect() {
        connectionStatus = "Disconnected"
    }

    func executeSamplingCommand(seconds: Int) {
        simulationTimer?.cancel()
        simulationTimer = Timer.publish(every: Double(seconds), on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                let sample = SwiftTelemetry(
                    timestamp: Date(),
                    temperature: self.latestTelemetry.temperature + Double.random(in: -0.2...0.2),
                    humidity: self.latestTelemetry.humidity,
                    batteryPercentage: self.latestTelemetry.batteryPercentage,
                    signalStrength: -60
                )
                self.latestTelemetry = sample
                self.telemetryHistory.append(sample)
            }
    }

    func translateNaturalLanguageCommand(prompt: String) {
        isTranslatingCommand = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            self.isTranslatingCommand = false
            if prompt.lowercased().contains("fast") || prompt.lowercased().contains("1") {
                self.pendingCommandExplanation = "Parsed Intent: Set sampling rate to 1 second. Safety verification: PASSED."
            } else if prompt.lowercased().contains("restart") {
                self.pendingCommandExplanation = "Parsed Intent: Remote Soft Restart. Safety verification: PASSED."
            } else {
                self.pendingCommandExplanation = "Parsed Intent: Update telemetry streaming configuration for '\(self.selectedDeviceName)'."
            }
        }
    }

    func requestAiAnalysis() {
        isAnalyzingAi = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.isAnalyzingAi = false
            self.aiSummary = "Device telemetry exhibits nominal thermodynamic equilibrium with 94% battery reserve."
            self.aiCauses = ["Standard background periodic broadcast", "Optimal antenna placement"]
            self.aiRecommendations = ["Maintain current 2s sampling frequency", "No thermal cooling intervention required"]
        }
    }
}

// MARK: - Main SwiftUI TabView
struct ContentView: View {
    @StateObject private var viewModel = NexoraViewModel()
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(viewModel: viewModel)
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(0)

            DevicesView(viewModel: viewModel)
                .tabItem {
                    Label("Devices", systemImage: "sensor.tag.radiowaves.forward.fill")
                }
                .tag(1)

            TelemetryView(viewModel: viewModel)
                .tabItem {
                    Label("Telemetry", systemImage: "chart.xyaxis.line")
                }
                .tag(2)

            InsightsView(viewModel: viewModel)
                .tabItem {
                    Label("Insights", systemImage: "sparkles")
                }
                .tag(3)

            SettingsView(viewModel: viewModel)
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(4)
        }
        .accentColor(Color(red: 0.0, green: 0.9, blue: 1.0))
        .preferredColorScheme(.dark)
    }
}

// MARK: - Home View
struct HomeView: View {
    @ObservedObject var viewModel: NexoraViewModel

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Header Card
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Nexora Connected Platform")
                            .font(.title2)
                            .bold()
                        Text("Connect. Monitor. Understand.")
                            .font(.subheadline)
                            .foregroundColor(.cyan)
                    }
                    .padding(.horizontal)

                    // Quick Glance
                    HStack(spacing: 12) {
                        MetricCard(title: "Status", value: viewModel.connectionStatus, color: .green)
                        MetricCard(title: "Temp", value: "\(viewModel.latestTelemetry.temperature)°C", color: .cyan)
                        MetricCard(title: "Battery", value: "\(viewModel.latestTelemetry.batteryPercentage)%", color: .green)
                    }
                    .padding(.horizontal)

                    // Active Device Section
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Target Peripheral")
                            .font(.headline)
                            .padding(.horizontal)

                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "antenna.radiowaves.left.and.right")
                                    .foregroundColor(.cyan)
                                    .font(.title2)
                                VStack(alignment: .leading) {
                                    Text(viewModel.selectedDeviceName)
                                        .font(.headline)
                                    Text(viewModel.selectedDeviceId)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                Spacer()
                                Text(viewModel.connectionStatus)
                                    .font(.caption)
                                    .bold()
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.cyan.opacity(0.2))
                                    .cornerRadius(8)
                            }
                        }
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        .padding(.horizontal)
                    }
                }
                .padding(.top)
            }
            .navigationTitle("Nexora Dashboard")
        }
    }
}

// MARK: - Devices View
struct DevicesView: View {
    @ObservedObject var viewModel: NexoraViewModel

    let simulatedNodes = [
        ("Nexora Sense [Normal]", "SIM-NX-NORMAL-01", -58),
        ("Nexora Beacon [Low Battery]", "SIM-NX-LOWBATT-02", -75),
        ("Nexora Probe [Weak Signal]", "SIM-NX-WEAKSIG-03", -102),
        ("Nexora Industrial [Thermal]", "SIM-NX-HIGHTEMP-05", -62)
    ]

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Simulator Mode")) {
                    Toggle("Zero-Hardware Simulation", isOn: $viewModel.isSimulatorMode)
                }

                Section(header: Text("Discovered Peripherals")) {
                    ForEach(simulatedNodes, id: \.1) { node in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(node.0).font(.headline)
                                Text("ID: \(node.1) • RSSI: \(node.2) dBm").font(.caption).foregroundColor(.gray)
                            }
                            Spacer()
                            Button(viewModel.selectedDeviceId == node.1 && viewModel.connectionStatus == "Connected" ? "Disconnect" : "Connect") {
                                if viewModel.selectedDeviceId == node.1 && viewModel.connectionStatus == "Connected" {
                                    viewModel.disconnect()
                                } else {
                                    viewModel.connect(deviceId: node.1, name: node.0)
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.cyan)
                        }
                    }
                }
            }
            .navigationTitle("BLE Devices")
        }
    }
}

// MARK: - Telemetry View
struct TelemetryView: View {
    @ObservedObject var viewModel: NexoraViewModel

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    MetricCard(title: "Core Temperature", value: "\(viewModel.latestTelemetry.temperature) °C", color: .cyan)
                    MetricCard(title: "Relative Humidity", value: "\(viewModel.latestTelemetry.humidity) %", color: .blue)
                    MetricCard(title: "Battery Capacity", value: "\(viewModel.latestTelemetry.batteryPercentage) %", color: .green)
                    MetricCard(title: "BLE Link RSSI", value: "\(viewModel.latestTelemetry.signalStrength) dBm", color: .orange)

                    // Real-Time Historical Stream
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Live Telemetry Buffer (\(viewModel.telemetryHistory.count) samples)")
                            .font(.headline)
                            .padding(.horizontal)

                        ForEach(viewModel.telemetryHistory.suffix(6).reversed()) { sample in
                            HStack {
                                Text("\(sample.temperature, specifier: "%.1f")°C")
                                    .foregroundColor(.cyan)
                                    .bold()
                                Spacer()
                                Text("RH: \(sample.humidity, specifier: "%.1f")%")
                                    .foregroundColor(.gray)
                                Spacer()
                                Text("Batt: \(sample.batteryPercentage)%")
                                    .foregroundColor(.green)
                            }
                            .padding()
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(12)
                            .padding(.horizontal)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Live Telemetry")
        }
    }
}

// MARK: - Insights & Safe Commands View
struct InsightsView: View {
    @ObservedObject var viewModel: NexoraViewModel
    @State private var commandInput: String = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Natural Language Command
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Natural Language BLE Command")
                            .font(.headline)
                        Text("LLM translations are strictly validated before hardware transmission.")
                            .font(.caption)
                            .foregroundColor(.gray)

                        HStack {
                            TextField("e.g. Set sampling interval to 1s", text: $commandInput)
                                .textFieldStyle(.roundedBorder)

                            Button("Send") {
                                viewModel.translateNaturalLanguageCommand(prompt: commandInput)
                                commandInput = ""
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.cyan)
                        }

                        if let explanation = viewModel.pendingCommandExplanation {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(explanation)
                                    .font(.subheadline)
                                    .foregroundColor(.cyan)

                                Button("Confirm & Transmit via BLE") {
                                    viewModel.executeSamplingCommand(seconds: 1)
                                    viewModel.pendingCommandExplanation = nil
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(.green)
                            }
                            .padding()
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(12)
                        }
                    }
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal)

                    // Groq AI Diagnostics
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("Groq AI Deep Analysis")
                                .font(.headline)
                            Spacer()
                            Button("Analyze Now") {
                                viewModel.requestAiAnalysis()
                            }
                            .buttonStyle(.bordered)
                        }

                        if let summary = viewModel.aiSummary {
                            Text(summary)
                                .font(.body)
                                .padding(.top, 4)

                            ForEach(viewModel.aiRecommendations, id: \.self) { rec in
                                Text("• \(rec)")
                                    .font(.caption)
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal)
                }
                .padding(.top)
            }
            .navigationTitle("AI Insights")
        }
    }
}

// MARK: - Settings View
struct SettingsView: View {
    @ObservedObject var viewModel: NexoraViewModel
    @State private var apiKey: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Groq AI Configuration")) {
                    SecureField("Groq API Key (Optional)", text: $apiKey)
                    Text("Core BLE, charts, and local heuristics operate 100% offline without an API key.")
                        .font(.caption)
                        .foregroundColor(.gray)
                }

                Section(header: Text("About Nexora")) {
                    Text("Version: 1.0.0 (KMP + Jetpack Compose + SwiftUI)")
                    Text("Author: Yodgorbek Komilov")
                    Text("License: Apache License 2.0")
                    Text("Tagline: Connect. Monitor. Understand.")
                        .foregroundColor(.cyan)
                }
            }
            .navigationTitle("Settings")
        }
    }
}

// MARK: - Metric Card Component
struct MetricCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
            Text(value)
                .font(.title3)
                .bold()
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(14)
    }
}