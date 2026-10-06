import SwiftUI
import CoreBluetooth
import Combine

// MARK: - Native iOS CoreBluetooth Manager
final class CoreBluetoothManager: NSObject, ObservableObject, CBCentralManagerDelegate, CBPeripheralDelegate {

    @Published var isScanning = false
    @Published var discoveredPeripherals: [CBPeripheral] = []
    @Published var connectionState: String = "Disconnected"
    @Published var latestTelemetryBytes: Data? = nil

    private var centralManager: CBCentralManager!
    private var connectedPeripheral: CBPeripheral?

    // Standard Nexora UUIDs
    private let telemetryServiceUUID = CBUUID(string: "0000FFF0-0000-1000-8000-00805F9B34FB")
    private let telemetryStreamCharUUID = CBUUID(string: "0000FFF1-0000-1000-8000-00805F9B34FB")
    private let commandServiceUUID = CBUUID(string: "0000FFE0-0000-1000-8000-00805F9B34FB")
    private let commandWriteCharUUID = CBUUID(string: "0000FFE1-0000-1000-8000-00805F9B34FB")

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    func startScan() {
        guard centralManager.state == .poweredOn else { return }
        isScanning = true
        discoveredPeripherals.removeAll()
        centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
    }

    func stopScan() {
        centralManager.stopScan()
        isScanning = false
    }

    func connect(peripheral: CBPeripheral) {
        connectedPeripheral = peripheral
        connectionState = "Connecting"
        centralManager.connect(peripheral, options: nil)
    }

    func disconnect() {
        if let peripheral = connectedPeripheral {
            connectionState = "Disconnecting"
            centralManager.cancelPeripheralConnection(peripheral)
        }
    }

    func writeCommand(bytes: [UInt8]) {
        guard let peripheral = connectedPeripheral else { return }
        for service in peripheral.services ?? [] where service.uuid == commandServiceUUID {
            for char in service.characteristics ?? [] where char.uuid == commandWriteCharUUID {
                let data = Data(bytes)
                peripheral.writeValue(data, for: char, type: .withResponse)
            }
        }
    }

    // MARK: - CBCentralManagerDelegate
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            print("[CoreBluetooth] Bluetooth Powered On")
        case .poweredOff:
            connectionState = "Bluetooth Disabled"
        case .unauthorized:
            connectionState = "Permission Denied"
        default:
            connectionState = "Unavailable"
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if !discoveredPeripherals.contains(where: { $0.identifier == peripheral.identifier }) {
            discoveredPeripherals.append(peripheral)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connectionState = "Connected"
        peripheral.delegate = self
        peripheral.discoverServices([telemetryServiceUUID, commandServiceUUID])
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        connectionState = "Connection Failed"
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        connectionState = "Disconnected"
        connectedPeripheral = nil
    }

    // MARK: - CBPeripheralDelegate
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else { return }
        for char in characteristics where char.uuid == telemetryStreamCharUUID {
            peripheral.setNotifyValue(true, for: char)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if characteristic.uuid == telemetryStreamCharUUID, let value = characteristic.value {
            DispatchQueue.main.async {
                self.latestTelemetryBytes = value
            }
        }
    }
}
