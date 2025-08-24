package com.example.sosapp.domain.usecase

import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.service.BluetoothService
import javax.inject.Inject

class BluetoothUseCase @Inject constructor(
    private val bluetoothService: BluetoothService
) {

    companion object {
        private const val HC05_MAC = "29:50:0E:A7:8A:54"
    }

    /**
     * Connect to HC-05 using serial communication
     */
    suspend fun connect(): AuthResult<Unit> {
        return bluetoothService.connectToHC05(HC05_MAC)
    }

    /**
     * Connect by MAC address
     */
    suspend fun connectByMac(macAddress: String = HC05_MAC): AuthResult<Unit> {
        return bluetoothService.connectToHC05(macAddress)
    }

    /**
     * Send SOS command to Arduino
     */
    suspend fun sendSOS(): AuthResult<String> {
        return bluetoothService.sendSOSTrigger()
    }

    /**
     * Send location to Arduino
     */
    suspend fun sendLocationToArduino(latitude: Double, longitude: Double): AuthResult<String> {
        return bluetoothService.sendLocation(latitude, longitude)
    }

    /**
     * Update emergency contacts on Arduino
     */
    suspend fun updateArduinoContacts(contacts: List<String>): AuthResult<Unit> {
        return bluetoothService.updateEmergencyContacts(contacts)
    }

    /**
     * Test Arduino connection
     */
    suspend fun testArduinoConnection(): AuthResult<String> {
        return bluetoothService.testConnection()
    }

    /**
     * Send custom command to Arduino (like typing in serial terminal)
     */
    suspend fun sendCommand(command: String): AuthResult<String> {
        return bluetoothService.sendSerialCommand(command)
    }

    /**
     * Read data from Arduino
     */
    suspend fun readDataFromArduino(): AuthResult<String> {
        return bluetoothService.readData()
    }

    /**
     * Cancel SOS
     */
    suspend fun cancelArduinoSOS(): AuthResult<String> {
        return bluetoothService.cancelSOS()
    }

    /**
     * Disconnect from Arduino
     */
    fun disconnect() {
        bluetoothService.disconnect()
    }

    /**
     * Check connection status
     */
    fun isConnectedToArduino(): Boolean {
        return bluetoothService.getConnectionInfo().first
    }

    /**
     * Get connection status
     */
    fun getConnectionStatus(): Triple<Boolean, String, String?> {
        return bluetoothService.getConnectionInfo()
    }

    /**
     * Check if HC-05 is paired
     */
    fun isHC05Paired(): Boolean {
        return bluetoothService.isHC05Paired(HC05_MAC)
    }

    /**
     * Get HC-05 device info
     */
    fun getHC05Info(): Pair<String?, String?> {
        return bluetoothService.getHC05Info(HC05_MAC)
    }

    /**
     * Check Bluetooth support
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothService.isBluetoothSupported()
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothService.isBluetoothEnabled()
    }

    /**
     * Get state flows for UI updates
     */
    fun getConnectionStateFlow() = bluetoothService.isConnected
    fun getConnectionStatusFlow() = bluetoothService.connectionStatus
    fun getArduinoStatusFlow() = bluetoothService.arduinoStatus
    fun getReceivedDataFlow() = bluetoothService.receivedData
    fun getSentDataFlow() = bluetoothService.sentData
    fun getLastUpdateTimeFlow() = bluetoothService.lastUpdateTime

    // Legacy methods for backward compatibility
    suspend fun connectToArduino(): AuthResult<Unit> = connect()
    suspend fun autoReconnectByMac(macAddress: String = HC05_MAC): AuthResult<Unit> = connectByMac(macAddress)
    fun disconnectFromArduino() = disconnect()
    fun findDevice(deviceName: String) = bluetoothService.findHC05DeviceByMac(HC05_MAC)
    fun findDeviceByMac(macAddress: String) = bluetoothService.findHC05DeviceByMac(macAddress)
    fun getPairedDevices() = bluetoothService.getPairedDevices()
}