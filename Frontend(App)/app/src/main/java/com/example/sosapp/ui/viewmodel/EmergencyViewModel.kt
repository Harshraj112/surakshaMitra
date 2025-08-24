package com.example.sosapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.ContactValidationState
import com.example.sosapp.data.model.DeviceStatus
import com.example.sosapp.data.model.EmergencyContact
import com.example.sosapp.data.model.EmergencyUiState
import com.example.sosapp.data.model.UserData
import com.example.sosapp.data.service.BluetoothService
import com.example.sosapp.data.service.EmergencyContactService
import com.example.sosapp.data.service.LocationService
import com.example.sosapp.data.service.NotificationService
import com.example.sosapp.data.service.VoskSpeechService
import com.example.sosapp.domain.repository.AuthRepository
import com.example.sosapp.domain.usecase.BluetoothUseCase
import com.example.sosapp.domain.usecase.LocationUseCase
import com.example.sosapp.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val emergencyContactService: EmergencyContactService,
    private val locationService: LocationService,
    private val locationUseCase: LocationUseCase,
    private val notificationService: NotificationService,
    private val voskSpeechService: VoskSpeechService,
    private val bluetoothService: BluetoothService,
    private val bluetoothUseCase: BluetoothUseCase
) : ViewModel() {

    // UI State Management
    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    // Contact Management
    private val _contact1 = MutableStateFlow("")
    val contact1: StateFlow<String> = _contact1.asStateFlow()

    private val _contact2 = MutableStateFlow("")
    val contact2: StateFlow<String> = _contact2.asStateFlow()

    private val _contact3 = MutableStateFlow("")
    val contact3: StateFlow<String> = _contact3.asStateFlow()

    // Contact Validation
    private val _contact1Validation = MutableStateFlow(ContactValidationState(true))
    val contact1Validation: StateFlow<ContactValidationState> = _contact1Validation.asStateFlow()

    private val _contact2Validation = MutableStateFlow(ContactValidationState(true))
    val contact2Validation: StateFlow<ContactValidationState> = _contact2Validation.asStateFlow()

    private val _contact3Validation = MutableStateFlow(ContactValidationState(true))
    val contact3Validation: StateFlow<ContactValidationState> = _contact3Validation.asStateFlow()

    // Initialization State
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Emergency Directory
    private val _emergencyDirectory = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyDirectory: StateFlow<List<EmergencyContact>> = _emergencyDirectory.asStateFlow()

    // User Data
    private val _currentUser = MutableStateFlow<UserData?>(null)

    // Call Permission Handling
    private val _pendingCall = MutableStateFlow<String?>(null)
    val pendingCall: StateFlow<String?> = _pendingCall.asStateFlow()

    private val _showCallPermissionDialog = MutableStateFlow(false)
    val showCallPermissionDialog: StateFlow<Boolean> = _showCallPermissionDialog.asStateFlow()

    // Voice Detection States
    private val _isVoiceDetectionInitialized = MutableStateFlow(false)
    val isVoiceDetectionInitialized: StateFlow<Boolean> = _isVoiceDetectionInitialized.asStateFlow()

    val isVoiceDetectionActive: StateFlow<Boolean> = voskSpeechService.isListening
    val lastDetectedText: StateFlow<String> = voskSpeechService.lastDetectedText

    private val _detectedKeywords = MutableStateFlow<List<String>>(emptyList())
    val detectedKeywords: StateFlow<List<String>> = _detectedKeywords.asStateFlow()

    // Arduino/Bluetooth States
    private val _isArduinoConnected = MutableStateFlow(false)
    val isArduinoConnected: StateFlow<Boolean> = _isArduinoConnected.asStateFlow()

    private val _arduinoConnectionStatus = MutableStateFlow("Disconnected")
    val arduinoConnectionStatus: StateFlow<String> = _arduinoConnectionStatus.asStateFlow()

    private val _arduinoLastUpdate = MutableStateFlow<String?>(null)
    val arduinoLastUpdate: StateFlow<String?> = _arduinoLastUpdate.asStateFlow()

    private val _arduinoResponse = MutableStateFlow<String?>(null)
    val arduinoResponse: StateFlow<String?> = _arduinoResponse.asStateFlow()

    // Auto-reconnection management
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    // Location tracking
    private var lastKnownLatitude = 0.0
    private var lastKnownLongitude = 0.0

    init {
        // Initialize all systems
        loadCurrentUser()
        initializeVoiceDetection()
        initializeArduino()
        updateDeviceStatus()

        // Start periodic updates
        startPeriodicStatusUpdates()

        // Monitor Arduino responses
        monitorArduinoResponses()
    }

    /**
     * INITIALIZATION METHODS
     */
    private fun startPeriodicStatusUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Update every 30 seconds
                updateDeviceStatus()

                // Auto-reconnect Arduino if disconnected
                if (!_isArduinoConnected.value && reconnectAttempts < maxReconnectAttempts) {
                    autoReconnectArduino()
                }

                // Update location periodically
                updateCurrentLocation()
            }
        }
    }

    private fun initializeVoiceDetection() {
        viewModelScope.launch {
            try {
                val initialized = voskSpeechService.initialize(context)
                _isVoiceDetectionInitialized.value = initialized

                if (initialized) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Voice detection ready"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to initialize voice detection"
                    )
                }
            } catch (e: Exception) {
                _isVoiceDetectionInitialized.value = false
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Voice detection initialization failed: ${e.message}"
                )
            }
        }
    }

    private fun initializeArduino() {
        viewModelScope.launch {
            try {
                if (!bluetoothService.isBluetoothSupported()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Bluetooth not supported on this device"
                    )
                    return@launch
                }

                if (!bluetoothService.isBluetoothEnabled()) {
                    _arduinoConnectionStatus.value = "Bluetooth Disabled"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please enable Bluetooth to connect to Arduino"
                    )
                    return@launch
                }

                // Check if HC-05 is paired
                if (bluetoothService.isHC05Paired()) {
                    _arduinoConnectionStatus.value = "HC-05 Found - Connecting..."
                    connectToArduino()
                } else {
                    _arduinoConnectionStatus.value = "HC-05 Not Paired"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "HC-05 not paired. Please pair device (29:50:0E:A7:8A:54) first."
                    )
                }
            } catch (e: Exception) {
                _arduinoConnectionStatus.value = "Initialization Failed"
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Arduino initialization failed: ${e.message}"
                )
            }
        }
    }

    private fun monitorArduinoResponses() {
        viewModelScope.launch {
            // Monitor Arduino status and received data
            bluetoothService.arduinoStatus.collect { status ->
                if (!status.isNullOrBlank()) {
                    _arduinoResponse.value = status
                    Log.d("EmergencyViewModel", "Arduino status: $status")
                }
            }
        }

        viewModelScope.launch {
            // Monitor received data for hardware triggers
            bluetoothService.receivedData.collect { data ->
                if (!data.isNullOrBlank()) {
                    handleArduinoResponse(data)
                }
            }
        }
    }

    private fun handleArduinoResponse(response: String) {
        Log.d("EmergencyViewModel", "Arduino Response: $response")

        when {
            // Hardware SOS trigger
            response.contains("SOS_STARTED") -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isPanicActive = true,
                        successMessage = "🚨 Hardware SOS triggered! Emergency in progress..."
                    )
                    delay(3000)
                    clearMessages()
                }
            }

            // SOS completion
            response.contains("SOS_DONE") -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isNotifyingContacts = false,
                        successMessage = "✅ Arduino completed SOS sequence!"
                    )
                    delay(5000)
                    clearMessages()
                }
            }

            // SOS cancelled
            response.contains("SOS_CANCELLED") -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isPanicActive = false,
                        isNotifyingContacts = false,
                        successMessage = "SOS cancelled by Arduino"
                    )
                    delay(2000)
                    clearMessages()
                }
            }

            // Location request from Arduino
            response.contains("LOC_REQ") -> {
                viewModelScope.launch {
                    Log.d("EmergencyViewModel", "Arduino requesting location")
                    sendCurrentLocationToArduino()
                }
            }

            // Contact update confirmation
            response.contains("OK CONTACT UPDATED") -> {
                Log.d("EmergencyViewModel", "Arduino confirmed contact update")
            }

            // Contact data response
            response.startsWith("CONTACT") -> {
                val parts = response.split(":")
                if (parts.size == 2) {
                    val contactIndex = parts[0].substringAfter("CONTACT").toIntOrNull()
                    val contactNumber = parts[1]
                    Log.d(
                        "EmergencyViewModel",
                        "Received contact $contactIndex: $contactNumber"
                    )
                }
            }

            // Arduino ready signal
            response.contains("READY") -> {
                Log.d("EmergencyViewModel", "Arduino is ready")
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Arduino is ready"
                    )
                    // Auto-sync data when Arduino is ready
                    delay(1000)
                    syncAllDataToArduino()
                }
            }
        }
    }

    fun startVoiceDetection() {
        if (!_isVoiceDetectionInitialized.value) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Voice detection not initialized"
            )
            return
        }

        if (!voskSpeechService.hasAudioPermission(context)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Microphone permission required for voice detection"
            )
            return
        }

        viewModelScope.launch {
            val success = voskSpeechService.startListening(
                onPanicKeywordDetected = {
                    // This will trigger the panic button automatically
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "🚨 EMERGENCY KEYWORD DETECTED! Triggering panic..."
                        )
                        delay(500) // Brief delay to show the message
                        triggerEmergency()
                    }
                },
                onKeywordDetected = { keyword ->
                    // Track detected keywords for UI
                    val currentKeywords = _detectedKeywords.value.toMutableList()
                    if (!currentKeywords.contains(keyword)) {
                        currentKeywords.add(keyword)
                        if (currentKeywords.size > 5) {
                            currentKeywords.removeAt(0) // Keep only last 5
                        }
                        _detectedKeywords.value = currentKeywords
                    }
                },
                onTextDetected = { text ->
                    // You can add any additional text processing here
                    android.util.Log.d("EmergencyViewModel", "Detected text: $text")
                }
            )

            if (success) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Voice detection started - Say 'help' or 'emergency' to trigger panic"
                )
                delay(3000)
                clearMessages()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to start voice detection"
                )
            }
        }
    }

    /**
     * VOICE DETECTION METHODS
     */
    fun stopVoiceDetection() {
        voskSpeechService.stopListening()
        _detectedKeywords.value = emptyList()
        _uiState.value = _uiState.value.copy(
            successMessage = "Voice detection stopped"
        )
        viewModelScope.launch {
            delay(2000)
            clearMessages()
        }
    }

    fun getEmergencyKeywords(): List<String> {
        return voskSpeechService.getEmergencyKeywords()
    }

    /**
     * ARDUINO CONNECTION METHODS
     */
    private fun connectToArduino() {
        viewModelScope.launch {
            try {
                _arduinoConnectionStatus.value = "Connecting..."
                _uiState.value = _uiState.value.copy(
                    successMessage = "Connecting to HC-05..."
                )

                when (val result = bluetoothService.connectToHC05()) {
                    is AuthResult.Success -> {
                        _isArduinoConnected.value = true
                        _arduinoConnectionStatus.value = "Connected"
                        _arduinoLastUpdate.value = getCurrentTimeString()
                        reconnectAttempts = 0

                        _uiState.value = _uiState.value.copy(
                            successMessage = "HC-05 connected successfully!"
                        )

                        // Test communication
                        delay(1000)
                        testArduinoConnection()

                        // Sync data after successful test
                        delay(1000)
                        syncAllDataToArduino()

                        delay(2000)
                        clearMessages()
                    }

                    is AuthResult.Error -> {
                        _isArduinoConnected.value = false
                        _arduinoConnectionStatus.value = "Connection Failed"
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to connect: ${result.error.message}"
                        )
                        reconnectAttempts++
                    }
                }
            } catch (e: Exception) {
                _isArduinoConnected.value = false
                _arduinoConnectionStatus.value = "Error"
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Connection error: ${e.message}"
                )
                reconnectAttempts++
            }
        }
    }

    fun disconnectArduino() {
        bluetoothService.disconnect()
        _isArduinoConnected.value = false
        _arduinoConnectionStatus.value = "Disconnected"
        _uiState.value = _uiState.value.copy(
            successMessage = "Arduino disconnected"
        )
        viewModelScope.launch {
            delay(2000)
            clearMessages()
        }
    }

    fun testArduinoConnection() {
        if (!_isArduinoConnected.value) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Arduino not connected. Please connect first."
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Testing Arduino connection..."
                )

                when (val result = bluetoothService.testConnection()) {
                    is AuthResult.Success -> {
                        _arduinoLastUpdate.value = getCurrentTimeString()
                        val response = result.data
                        _uiState.value = _uiState.value.copy(
                            successMessage = "✅ Arduino test successful: $response"
                        )
                        delay(2000)
                        clearMessages()
                    }

                    is AuthResult.Error -> {
                        _isArduinoConnected.value = false
                        _arduinoConnectionStatus.value = "Connection Lost"
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Arduino test failed: ${result.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Arduino test error: ${e.message}"
                )
            }
        }
    }

    private fun autoReconnectArduino() {
        if (reconnectAttempts < maxReconnectAttempts) {
            viewModelScope.launch {
                delay(5000)
                Log.d(
                    "EmergencyViewModel",
                    "Auto-reconnecting Arduino... (attempt ${reconnectAttempts + 1})"
                )
                connectToArduino()
            }
        }
    }

    /**
     * EMERGENCY METHODS
     */
    fun triggerEmergency() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isPanicActive = true,
                    isNotifyingContacts = true,
                    successMessage = "🚨 Emergency alert activated!"
                )

                // Show emergency notification
                try {
                    notificationService.showEmergencyActiveNotification()
                } catch (e: SecurityException) {
                    // Handle notification permission not granted
                }

                // Get current location first
                val locationResult = locationUseCase.getCurrentLocation()
                var locationText = "Location unavailable"
                var latitude = 0.0
                var longitude = 0.0

                when (locationResult) {
                    is AuthResult.Success -> {
                        latitude = locationResult.data.first
                        longitude = locationResult.data.second
                        lastKnownLatitude = latitude
                        lastKnownLongitude = longitude
                        locationText = "https://maps.google.com/?q=$latitude,$longitude"
                        _uiState.value = _uiState.value.copy(isLocationSharing = true)

                        // Send location to Arduino first
                        sendLocationToArduino(latitude, longitude)
                    }

                    is AuthResult.Error -> {
                        Log.w(
                            "EmergencyViewModel",
                            "Failed to get location: ${locationResult.error.message}"
                        )
                        // Use last known location if available
                        if (lastKnownLatitude != 0.0 && lastKnownLongitude != 0.0) {
                            locationText =
                                "https://maps.google.com/?q=$lastKnownLatitude,$lastKnownLongitude"
                            sendLocationToArduino(lastKnownLatitude, lastKnownLongitude)
                        }
                    }
                }

                // Send SOS signal to Arduino with location
                sendSOSToArduino(locationText)

                // Notify emergency contacts (backup to Arduino)
                notifyEmergencyContacts(locationText, latitude, longitude)

                delay(2000)

                _uiState.value = _uiState.value.copy(
                    isNotifyingContacts = false,
                    successMessage = "✅ Emergency contacts notified! Arduino alerted."
                )

                delay(5000)
                clearMessages()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPanicActive = false,
                    isNotifyingContacts = false,
                    isLocationSharing = false,
                    errorMessage = "Failed to trigger emergency: ${e.message}"
                )
                delay(3000)
                clearMessages()
            }
        }
    }

    fun stopEmergency() {
        viewModelScope.launch {
            try {
                // Send cancel command to Arduino first
                if (_isArduinoConnected.value) {
                    when (val result = bluetoothService.cancelSOS()) {
                        is AuthResult.Success -> {
                            Log.d(
                                "EmergencyViewModel",
                                "Arduino SOS cancelled: ${result.data}"
                            )
                        }

                        is AuthResult.Error -> {
                            Log.w(
                                "EmergencyViewModel",
                                "Failed to cancel Arduino SOS: ${result.error.message}"
                            )
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isPanicActive = false,
                    isNotifyingContacts = false,
                    isLocationSharing = false,
                    successMessage = "Emergency alert stopped"
                )

                // Hide emergency notification
                notificationService.hideEmergencyNotification()

                delay(2000)
                clearMessages()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to stop emergency: ${e.message}"
                )
                delay(3000)
                clearMessages()
            }
        }
    }

    private suspend fun sendSOSToArduino(locationText: String = "") {
        if (_isArduinoConnected.value) {
            try {
                Log.d("EmergencyViewModel", "Sending SOS command to Arduino...")

                when (val result = bluetoothService.sendSOSTrigger()) {
                    is AuthResult.Success -> {
                        _arduinoLastUpdate.value = getCurrentTimeString()
                        Log.i("EmergencyViewModel", "✅ SOS sent to Arduino: ${result.data}")

                        _uiState.value = _uiState.value.copy(
                            successMessage = "✅ Arduino received SOS command"
                        )
                    }

                    is AuthResult.Error -> {
                        Log.w("EmergencyViewModel", "Failed to send SOS: ${result.error.message}")
                        _isArduinoConnected.value = false
                        _arduinoConnectionStatus.value = "Connection Lost"
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error sending SOS: ${e.message}")
            }
        } else {
            Log.w("EmergencyViewModel", "Arduino not connected - SOS handled by app only")
        }
    }

    private suspend fun sendLocationToArduino(latitude: Double, longitude: Double) {
        if (_isArduinoConnected.value) {
            try {
                when (val result = bluetoothService.sendLocation(latitude, longitude)) {
                    is AuthResult.Success -> {
                        _arduinoLastUpdate.value = getCurrentTimeString()
                        Log.i("EmergencyViewModel", "✅ Location sent: $latitude, $longitude - ${result.data}")
                    }

                    is AuthResult.Error -> {
                        Log.w("EmergencyViewModel", "Failed to send location: ${result.error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error sending location: ${e.message}")
            }
        }
    }

    private suspend fun sendCurrentLocationToArduino() {
        try {
            when (val locationResult = locationUseCase.getCurrentLocation()) {
                is AuthResult.Success -> {
                    lastKnownLatitude = locationResult.data.first
                    lastKnownLongitude = locationResult.data.second
                    sendLocationToArduino(lastKnownLatitude, lastKnownLongitude)
                }

                is AuthResult.Error -> {
                    Log.w(
                        "EmergencyViewModel",
                        "Could not get location for Arduino: ${locationResult.error.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(
                "EmergencyViewModel",
                "Error sending current location to Arduino: ${e.message}"
            )
        }
    }

    private suspend fun updateCurrentLocation() {
        try {
            when (val locationResult = locationUseCase.getCurrentLocation()) {
                is AuthResult.Success -> {
                    val newLat = locationResult.data.first
                    val newLng = locationResult.data.second

                    // Only update Arduino if location changed significantly (>50 meters)
                    if (kotlin.math.abs(newLat - lastKnownLatitude) > 0.0005 ||
                        kotlin.math.abs(newLng - lastKnownLongitude) > 0.0005
                    ) {
                        lastKnownLatitude = newLat
                        lastKnownLongitude = newLng
                        sendLocationToArduino(newLat, newLng)
                    }
                }

                is AuthResult.Error -> {
                    // Silently fail for periodic updates
                }
            }
        } catch (e: Exception) {
            // Silently fail for periodic updates
        }
    }

    /**
     * ARDUINO DATA MANAGEMENT
     */
    private suspend fun updateArduinoEmergencyContacts() {
        if (!_isArduinoConnected.value) return

        try {
            val user = _currentUser.value
            if (user?.emergencyContacts?.isNotEmpty() == true) {
                Log.d("EmergencyViewModel", "Updating Arduino emergency contacts...")

                when (val result = bluetoothService.updateEmergencyContacts(user.emergencyContacts)) {
                    is AuthResult.Success -> {
                        Log.i("EmergencyViewModel", "✅ Emergency contacts updated on Arduino")
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Emergency contacts synced to Arduino"
                        )
                    }

                    is AuthResult.Error -> {
                        Log.w("EmergencyViewModel", "Failed to update contacts: ${result.error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to sync contacts to Arduino"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyViewModel", "Error updating Arduino contacts: ${e.message}")
        }
    }

    private suspend fun syncAllDataToArduino() {
        if (_isArduinoConnected.value) {
            try {
                Log.d("EmergencyViewModel", "Syncing all data to Arduino...")

                // Sync emergency contacts
                updateArduinoEmergencyContacts()
                delay(1000)

                // Sync current location
                sendCurrentLocationToArduino()
                delay(1000)

                _uiState.value = _uiState.value.copy(
                    successMessage = "All data synced to Arduino"
                )
                delay(2000)
                clearMessages()

            } catch (e: Exception) {
                Log.e(
                    "EmergencyViewModel",
                    "Error syncing data to Arduino: ${e.message}"
                )
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to sync data to Arduino"
                )
            }
        }
    }

    /**
     * CONTACT MANAGEMENT METHODS
     */
    fun initializeContacts(contacts: List<String>) {
        if (!_isInitialized.value) {
            _contact1.value = contacts.getOrNull(0) ?: ""
            _contact2.value = contacts.getOrNull(1) ?: ""
            _contact3.value = contacts.getOrNull(2) ?: ""
            _isInitialized.value = true
        }
    }

    fun updateContact1(newContact: String) {
        _contact1.value = newContact
        _contact1Validation.value = validatePhoneNumber(newContact, isRequired = true)
    }

    fun updateContact2(newContact: String) {
        _contact2.value = newContact
        _contact2Validation.value = validatePhoneNumber(newContact, isRequired = false)
    }

    fun updateContact3(newContact: String) {
        _contact3.value = newContact
        _contact3Validation.value = validatePhoneNumber(newContact, isRequired = false)
    }

    private fun validatePhoneNumber(
        phone: String,
        isRequired: Boolean = false
    ): ContactValidationState {
        return when {
            phone.isEmpty() && isRequired -> ContactValidationState(false, "This field is required")
            phone.isEmpty() && !isRequired -> ContactValidationState(true)
            phone.length < 10 -> ContactValidationState(
                false,
                "Phone number must be at least 10 digits"
            )

            phone.length > 15 -> ContactValidationState(
                false,
                "Phone number cannot exceed 15 digits"
            )

            !phone.matches(Regex("^[+]?[0-9\\s\\-()]+$")) ->
                ContactValidationState(false, "Invalid phone number format")

            else -> ContactValidationState(true)
        }
    }

    fun validateAllContacts(): Boolean {
        _contact1Validation.value = validatePhoneNumber(_contact1.value, isRequired = true)
        _contact2Validation.value = validatePhoneNumber(_contact2.value, isRequired = false)
        _contact3Validation.value = validatePhoneNumber(_contact3.value, isRequired = false)
        return _contact1Validation.value.isValid &&
                _contact2Validation.value.isValid &&
                _contact3Validation.value.isValid &&
                _contact1.value.isNotEmpty()
    }

    /**
     * EMERGENCY DIRECTORY METHODS
     */
    fun loadEmergencyDirectory(countryCode: String) {
        viewModelScope.launch {
            try {
                delay(500) // Simulate loading delay for network call
                val directory = Constants.getEmergencyNumbers(countryCode)
                _emergencyDirectory.value = directory

                _uiState.value = _uiState.value.copy(
                    successMessage = "Emergency directory loaded"
                )

                delay(2000)
                clearMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load emergency directory: ${e.message}"
                )
            }
        }
    }

    /**
     * CALL MANAGEMENT METHODS
     */
    fun makeEmergencyCall(phoneNumber: String) {
        viewModelScope.launch {
            try {
                if (emergencyContactService.hasCallPermission(context)) {
                    executeEmergencyCall(phoneNumber)
                } else {
                    _pendingCall.value = phoneNumber
                    _showCallPermissionDialog.value = true
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to initiate call: ${e.message}"
                )
                delay(3000)
                clearMessages()
            }
        }
    }

    private suspend fun executeEmergencyCall(phoneNumber: String) {
        try {
            val callMade = emergencyContactService.makeEmergencyCall(context, phoneNumber)

            if (callMade) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Calling $phoneNumber..."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Dialer opened for $phoneNumber"
                )
            }

            delay(3000)
            clearMessages()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to make call: ${e.message}"
            )
            delay(3000)
            clearMessages()
        }
    }

    fun onCallPermissionGranted() {
        viewModelScope.launch {
            _showCallPermissionDialog.value = false
            val phoneNumber = _pendingCall.value
            if (phoneNumber != null) {
                _pendingCall.value = null
                executeEmergencyCall(phoneNumber)
            }
        }
    }

    fun onCallPermissionDenied() {
        viewModelScope.launch {
            _showCallPermissionDialog.value = false
            val phoneNumber = _pendingCall.value
            if (phoneNumber != null) {
                _pendingCall.value = null
                val dialerOpened = emergencyContactService.makeEmergencyCall(context, phoneNumber)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (dialerOpened) "Dialer opened for $phoneNumber"
                    else "Please dial $phoneNumber manually"
                )
                delay(3000)
                clearMessages()
            }
        }
    }

    fun dismissCallPermissionDialog() {
        _showCallPermissionDialog.value = false
        _pendingCall.value = null
    }

    fun callEmergencyContacts() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user?.emergencyContacts.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No emergency contacts to call"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                successMessage = "Calling emergency contacts..."
            )

            // Call first emergency contact
            user?.emergencyContacts?.firstOrNull()?.let { contact ->
                makeEmergencyCall(contact)
            }
        }
    }

    /**
     * NOTIFICATION AND MESSAGING METHODS
     */
    private suspend fun notifyEmergencyContacts(
        locationText: String,
        latitude: Double,
        longitude: Double
    ) {
        val user = _currentUser.value
        if (user?.emergencyContacts.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No emergency contacts configured"
            )
            return
        }

        val emergencyMessage = """
            🚨 EMERGENCY ALERT 🚨
            
            ${user?.fullName} has triggered an emergency alert.
            
            Location: $locationText
            
            Google Maps: https://maps.google.com/?q=$latitude,$longitude
            
            Time: ${
            System.currentTimeMillis().let {
                java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(it))
            }
        }
            
            Please check on them immediately or contact local emergency services.
            
            - SOS App Emergency System
        """.trimIndent()

        // In a real implementation, you would:
        // 1. Send SMS to each emergency contact
        // 2. Make emergency calls if configured
        // 3. Send push notifications to app users
        // 4. Upload emergency event to server
        // 5. Send data to Arduino device

        user?.emergencyContacts?.forEach { contact ->
            delay(200) // Simulate SMS sending delay
            Log.d("EmergencyViewModel", "Sending emergency SMS to: $contact")
            // TODO: Implement actual SMS sending
            // smsService.sendSMS(contact, emergencyMessage)
        }
    }

    /**
     * DATA MANAGEMENT METHODS
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    _currentUser.value = user
                    _uiState.value = _uiState.value.copy(
                        emergencyContacts = user.emergencyContacts
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "User not logged in"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading user data: ${e.message}"
                )
            }
        }
    }

    private fun updateDeviceStatus() {
        viewModelScope.launch {
            try {
                val deviceStatus = DeviceStatus(
                    bandConnected = _isArduinoConnected.value,
                    locationEnabled = true, // You might want to check actual location status
                    lastUpdate = getCurrentTimeString()
                )

                _uiState.value = _uiState.value.copy(
                    deviceStatus = deviceStatus
                )
            } catch (e: Exception) {
                Log.e(
                    "EmergencyViewModel",
                    "Error updating device status: ${e.message}"
                )
            }
        }
    }

    /**
     * UTILITY METHODS
     */
    private fun getCurrentTimeString(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    private fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    /**
     * CLEANUP
     */
    override fun onCleared() {
        super.onCleared()
        // Stop voice detection
        if (voskSpeechService.isListening.value) {
            voskSpeechService.stopListening()
        }
        // Disconnect Arduino
        bluetoothService.disconnect()
    }

    /**
     * Connect to Arduino using the specific HC-05 MAC address
     */
    fun connectToArduinoByMac() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy() }

                when (val result = bluetoothUseCase.connectByMac("29:50:0E:A7:8A:54")) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                deviceStatus = it.deviceStatus.copy(
                                    bandConnected = true,
                                    lastUpdate = getCurrentTime()
                                ),
                                successMessage = "Successfully connected to HC-05"
                            )
                        }
                        Log.i("EmergencyViewModel", "Connected to HC-05 via MAC address")
                    }

                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to connect to HC-05: ${result.error.message}"
                            )
                        }
                        Log.e(
                            "EmergencyViewModel",
                            "Failed to connect to HC-05: ${result.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Connection error: ${e.message}"
                    )
                }
                Log.e("EmergencyViewModel", "Connection error: ${e.message}")
            }
        }
    }

    /**
     * Check if HC-05 is paired and get device info
     */
    private fun checkHC05Status() {
        viewModelScope.launch {
            try {
                val isPaired = bluetoothUseCase.isHC05Paired()
                val (deviceName, deviceAddress) = bluetoothUseCase.getHC05Info()

                Log.d(
                    "EmergencyViewModel",
                    "HC-05 Status - Paired: $isPaired, Name: $deviceName, Address: $deviceAddress"
                )

                if (!isPaired) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "HC-05 device is not paired. Please pair it in Bluetooth settings first."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error checking HC-05 status: ${e.message}")
            }
        }
    }

    /**
     * Auto-reconnect to HC-05 if connection is lost
     */
    fun autoReconnectToArduino() {
        viewModelScope.launch {
            try {
                if (!bluetoothUseCase.isConnectedToArduino()) {
                    Log.d("EmergencyViewModel", "Attempting auto-reconnect to HC-05...")

                    when (val result = bluetoothUseCase.autoReconnectByMac("29:50:0E:A7:8A:54")) {
                        is AuthResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    deviceStatus = it.deviceStatus.copy(
                                        bandConnected = true,
                                        lastUpdate = getCurrentTime()
                                    ),
                                    successMessage = "Reconnected to HC-05"
                                )
                            }
                            Log.i("EmergencyViewModel", "Auto-reconnected to HC-05")
                        }

                        is AuthResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    deviceStatus = it.deviceStatus.copy(
                                        bandConnected = false
                                    ),
                                    errorMessage = "Auto-reconnect failed: ${result.error.message}"
                                )
                            }
                            Log.w(
                                "EmergencyViewModel",
                                "Auto-reconnect failed: ${result.error.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Auto-reconnect error: ${e.message}")
            }
        }
    }

    /**
     * Initialize Arduino connection on app start
     */
    fun initializeArduinoConnection() {
        viewModelScope.launch {
            try {
                // Check if HC-05 is paired first
                if (bluetoothUseCase.isHC05Paired()) {
                    // Attempt to connect
                    connectToArduinoByMac()
                } else {
                    checkHC05Status()
                }
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error initializing Arduino connection: ${e.message}")
            }
        }
    }

    /**
     * Get current time for updates
     */
    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}