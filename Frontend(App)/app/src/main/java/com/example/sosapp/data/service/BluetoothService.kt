package com.example.sosapp.data.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.domain.model.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SerialBluetooth"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB" // Standard Serial Port Profile UUID
        private const val HC05_MAC_ADDRESS = "29:50:0E:A7:8A:54" // Your HC-05 MAC address
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val SERIAL_READ_TIMEOUT_MS = 1000L
        private const val MAX_BUFFER_SIZE = 1024
        private const val NEWLINE = "\n"
        private const val CARRIAGE_RETURN = "\r"
        private const val CRLF = "\r\n"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectedDevice: BluetoothDevice? = null

    // Serial Terminal State
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow<String?>(null)
    val lastUpdateTime: StateFlow<String?> = _lastUpdateTime.asStateFlow()

    // Serial data streams
    private val _receivedData = MutableStateFlow<String?>(null)
    val receivedData: StateFlow<String?> = _receivedData.asStateFlow()

    private val _sentData = MutableStateFlow<String?>(null)
    val sentData: StateFlow<String?> = _sentData.asStateFlow()

    // Arduino status
    private val _arduinoStatus = MutableStateFlow<String?>(null)
    val arduinoStatus: StateFlow<String?> = _arduinoStatus.asStateFlow()

    private val uuid: UUID = UUID.fromString(SPP_UUID)

    /**
     * Connect to HC-05 module like a serial terminal
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToHC05(macAddress: String = HC05_MAC_ADDRESS): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _connectionStatus.value = "Connecting..."
                Log.i(TAG, "Connecting to HC-05 at $macAddress as serial terminal...")

                if (!isBluetoothSupported()) {
                    return@withContext AuthResult.Error(AppError.LocationError("Bluetooth not supported"))
                }

                if (!isBluetoothEnabled()) {
                    return@withContext AuthResult.Error(AppError.LocationError("Bluetooth not enabled - please enable Bluetooth"))
                }

                // Get the HC-05 device
                connectedDevice = bluetoothAdapter?.getRemoteDevice(macAddress)
                if (connectedDevice == null) {
                    return@withContext AuthResult.Error(AppError.LocationError("HC-05 device not found at $macAddress"))
                }

                // Check if device is paired
                val bondState = connectedDevice!!.bondState
                if (bondState != BluetoothDevice.BOND_BONDED) {
                    return@withContext AuthResult.Error(
                        AppError.LocationError("HC-05 not paired - please pair the device in Bluetooth settings first")
                    )
                }

                // Disconnect any existing connection
                disconnect()

                // Connect using Serial Port Profile
                val connected = establishSerialConnection()

                if (connected) {
                    _isConnected.value = true
                    _connectionStatus.value = "Connected"
                    updateLastUpdateTime()

                    Log.i(TAG, "✅ Serial connection established to HC-05: ${connectedDevice!!.name}")

                    // Test serial communication
                    delay(1000) // Let HC-05 settle
                    testSerialCommunication()

                    return@withContext AuthResult.Success(Unit)
                } else {
                    disconnect()
                    return@withContext AuthResult.Error(
                        AppError.LocationError("Failed to establish serial connection to HC-05")
                    )
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission denied: ${e.message}")
                return@withContext AuthResult.Error(AppError.PermissionError("Bluetooth permission required"))
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
                disconnect()
                return@withContext AuthResult.Error(AppError.LocationError("Connection failed: ${e.message}"))
            }
        }
    }

    /**
     * Establish serial connection to HC-05
     */
    @SuppressLint("MissingPermission")
    private suspend fun establishSerialConnection(): Boolean {
        return try {
            Log.d(TAG, "Creating RFCOMM socket for serial communication...")

            // Create RFCOMM socket using Serial Port Profile UUID
            bluetoothSocket = connectedDevice!!.createRfcommSocketToServiceRecord(uuid)

            // Cancel discovery to improve connection reliability
            bluetoothAdapter?.cancelDiscovery()

            // Connect with timeout
            val connectionResult = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                bluetoothSocket!!.connect()
                true
            }

            if (connectionResult == true && bluetoothSocket!!.isConnected) {
                // Set up serial streams
                outputStream = bluetoothSocket!!.outputStream
                inputStream = bluetoothSocket!!.inputStream

                // Clear any existing data in buffer
                clearInputBuffer()

                Log.i(TAG, "Serial streams established successfully")
                true
            } else {
                Log.e(TAG, "Socket connection failed or timed out")
                false
            }

        } catch (e: IOException) {
            Log.e(TAG, "RFCOMM connection failed, trying fallback method: ${e.message}")

            try {
                // Fallback method using reflection (for problematic devices)
                val method = connectedDevice!!.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                bluetoothSocket = method.invoke(connectedDevice, 1) as BluetoothSocket

                val fallbackResult = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                    bluetoothSocket!!.connect()
                    true
                }

                if (fallbackResult == true && bluetoothSocket!!.isConnected) {
                    outputStream = bluetoothSocket!!.outputStream
                    inputStream = bluetoothSocket!!.inputStream
                    clearInputBuffer()
                    Log.i(TAG, "Fallback serial connection successful")
                    true
                } else {
                    false
                }

            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback connection also failed: ${fallbackException.message}")
                false
            }
        }
    }

    /**
     * Clear input buffer of any existing data
     */
    private fun clearInputBuffer() {
        try {
            val available = inputStream?.available() ?: 0
            if (available > 0) {
                val buffer = ByteArray(available)
                inputStream?.read(buffer)
                Log.d(TAG, "Cleared $available bytes from input buffer")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear input buffer: ${e.message}")
        }
    }

    /**
     * Test serial communication with Arduino
     */
    private suspend fun testSerialCommunication() {
        try {
            Log.d(TAG, "Testing serial communication...")

            // Send a simple test command
            val testResult = sendSerialCommand("PING")
            if (testResult is AuthResult.Success) {
                Log.i(TAG, "✅ Serial test successful: ${testResult.data}")
                _arduinoStatus.value = "Arduino Ready"
            } else {
                Log.w(TAG, "Serial test failed, but connection established")
                _arduinoStatus.value = "Connected (No Response)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Serial test error: ${e.message}")
            _arduinoStatus.value = "Connected (Test Failed)"
        }
    }

    /**
     * Send command through serial connection (like typing in terminal)
     */
    suspend fun sendSerialCommand(command: String, addNewline: Boolean = true): AuthResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value || outputStream == null) {
                    return@withContext AuthResult.Error(AppError.LocationError("Not connected to HC-05"))
                }

                // Format command like serial terminal
                val serialCommand = if (addNewline) {
                    command + CRLF  // Most Arduino sketches expect CRLF
                } else {
                    command
                }

                // Send data
                val bytes = serialCommand.toByteArray(Charsets.UTF_8)
                outputStream!!.write(bytes)
                outputStream!!.flush()

                _sentData.value = command
                updateLastUpdateTime()
                Log.i(TAG, "📤 Sent: '$command'")

                // Wait for response like serial terminal
                delay(100) // Give Arduino time to process
                val response = readSerialResponse()

                return@withContext response

            } catch (e: IOException) {
                Log.e(TAG, "Serial send failed: ${e.message}")
                handleConnectionLost()
                return@withContext AuthResult.Error(AppError.LocationError("Send failed - connection lost"))
            } catch (e: Exception) {
                Log.e(TAG, "Serial command error: ${e.message}")
                return@withContext AuthResult.Error(AppError.LocationError("Command failed: ${e.message}"))
            }
        }
    }

    /**
     * Read response from serial connection
     */
    private suspend fun readSerialResponse(timeoutMs: Long = SERIAL_READ_TIMEOUT_MS): AuthResult<String> {
        return try {
            if (!_isConnected.value || inputStream == null) {
                return AuthResult.Error(AppError.LocationError("Not connected"))
            }

            val result = withTimeoutOrNull(timeoutMs) {
                val buffer = StringBuilder()
                val byteBuffer = ByteArray(MAX_BUFFER_SIZE)
                val startTime = System.currentTimeMillis()

                // Keep reading until we get data or timeout
                while ((System.currentTimeMillis() - startTime) < timeoutMs) {
                    if (inputStream!!.available() > 0) {
                        val bytesRead = inputStream!!.read(byteBuffer, 0,
                            minOf(byteBuffer.size, inputStream!!.available()))

                        if (bytesRead > 0) {
                            val chunk = String(byteBuffer, 0, bytesRead, Charsets.UTF_8)
                            buffer.append(chunk)

                            // Check if we have a complete line
                            val response = buffer.toString()
                            if (response.contains('\n') || response.contains('\r') ||
                                (System.currentTimeMillis() - startTime) > 500) {
                                break
                            }
                        }
                    } else {
                        delay(50) // Small delay to avoid busy waiting
                    }
                }

                buffer.toString().trim()
            }

            if (!result.isNullOrBlank()) {
                _receivedData.value = result
                updateLastUpdateTime()
                Log.i(TAG, "📥 Received: '$result'")
                AuthResult.Success(result)
            } else {
                AuthResult.Error(AppError.LocationError("No response received"))
            }

        } catch (e: IOException) {
            Log.e(TAG, "Serial read failed: ${e.message}")
            handleConnectionLost()
            AuthResult.Error(AppError.LocationError("Read failed - connection lost"))
        } catch (e: Exception) {
            Log.e(TAG, "Serial read error: ${e.message}")
            AuthResult.Error(AppError.LocationError("Read error: ${e.message}"))
        }
    }

    /**
     * Continuously read from serial port (like terminal)
     */
    suspend fun readData(): AuthResult<String> {
        return readSerialResponse(SERIAL_READ_TIMEOUT_MS)
    }

    /**
     * Arduino-specific commands
     */
    suspend fun sendSOSTrigger(): AuthResult<String> {
        return sendSerialCommand("SOS").also { result ->
            if (result is AuthResult.Success) {
                _arduinoStatus.value = "SOS Triggered"
            }
        }
    }

    suspend fun sendLocation(latitude: Double, longitude: Double): AuthResult<String> {
        val locationCmd = "LOC:$latitude,$longitude"
        return sendSerialCommand(locationCmd).also { result ->
            if (result is AuthResult.Success) {
                _arduinoStatus.value = "Location Sent"
            }
        }
    }

    suspend fun updateEmergencyContacts(contacts: List<String>): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                contacts.take(3).forEachIndexed { index, contact ->
                    if (contact.isNotBlank()) {
                        val cmd = "CONTACT$index:$contact"
                        when (val result = sendSerialCommand(cmd)) {
                            is AuthResult.Success -> {
                                Log.d(TAG, "Contact $index updated: $contact")
                                _arduinoStatus.value = "Contact ${index + 1} Updated"
                                delay(1000) // Wait between contacts
                            }
                            is AuthResult.Error -> {
                                Log.e(TAG, "Failed to update contact $index: ${result.error.message}")
                                return@withContext result
                            }
                        }
                    }
                }
                AuthResult.Success(Unit)
            } catch (e: Exception) {
                AuthResult.Error(AppError.LocationError("Failed to update contacts: ${e.message}"))
            }
        }
    }

    suspend fun testConnection(): AuthResult<String> {
        return sendSerialCommand("PING").also { result ->
            when (result) {
                is AuthResult.Success -> _arduinoStatus.value = "Connection OK"
                is AuthResult.Error -> _arduinoStatus.value = "Connection Failed"
            }
        }
    }

    suspend fun cancelSOS(): AuthResult<String> {
        return sendSerialCommand("CANCEL").also { result ->
            if (result is AuthResult.Success) {
                _arduinoStatus.value = "SOS Cancelled"
            }
        }
    }

    /**
     * Connection management
     */
    private fun handleConnectionLost() {
        _isConnected.value = false
        _connectionStatus.value = "Connection Lost"
        _arduinoStatus.value = "Disconnected"
    }

    fun disconnect() {
        try {
            Log.i(TAG, "Disconnecting from HC-05...")

            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}")
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            connectedDevice = null

            _isConnected.value = false
            _connectionStatus.value = "Disconnected"
            _arduinoStatus.value = null
            _receivedData.value = null
            _sentData.value = null

            Log.i(TAG, "Disconnected from HC-05")
        }
    }

    /**
     * Utility methods
     */
    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun isHC05Paired(macAddress: String = HC05_MAC_ADDRESS): Boolean {
        return try {
            if (!isBluetoothEnabled()) return false
            val pairedDevices = bluetoothAdapter?.bondedDevices
            pairedDevices?.any { it.address == macAddress } == true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission error checking pairing: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getHC05Info(macAddress: String = HC05_MAC_ADDRESS): Pair<String?, String?> {
        return try {
            if (!isBluetoothEnabled()) return Pair(null, null)
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            Pair(device?.name, device?.address)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info: ${e.message}")
            Pair(null, null)
        }
    }

    fun getConnectionInfo(): Triple<Boolean, String, String?> {
        return Triple(
            _isConnected.value,
            _connectionStatus.value,
            _lastUpdateTime.value
        )
    }

    @SuppressLint("MissingPermission")
    fun findHC05DeviceByMac(macAddress: String = HC05_MAC_ADDRESS): BluetoothDevice? {
        return try {
            if (!isBluetoothEnabled()) return null
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)

            // Verify it's paired
            if (device != null && isHC05Paired(macAddress)) {
                Log.d(TAG, "Found paired HC-05: ${device.name} at $macAddress")
                device
            } else {
                Log.w(TAG, "HC-05 at $macAddress not found or not paired")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding HC-05: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        return try {
            bluetoothAdapter?.bondedDevices
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired devices: ${e.message}")
            null
        }
    }

    private fun updateLastUpdateTime() {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _lastUpdateTime.value = currentTime
    }

    // Legacy methods for compatibility
    suspend fun connectByMac(macAddress: String = HC05_MAC_ADDRESS): AuthResult<Unit> {
        return connectToHC05(macAddress)
    }

    suspend fun connect(deviceName: String = "HC-05"): AuthResult<Unit> {
        return connectToHC05() // Use default MAC address
    }
}