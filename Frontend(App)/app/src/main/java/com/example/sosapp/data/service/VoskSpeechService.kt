package com.example.sosapp.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskSpeechService @Inject constructor() : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastDetectedText = MutableStateFlow("")
    val lastDetectedText: StateFlow<String> = _lastDetectedText.asStateFlow()

    // Emergency keywords that will trigger panic button
    private val emergencyKeywords = setOf(
        // Primary emergency words
        "help", "emergency", "police", "fire", "ambulance",

        // Danger/threat words
        "stop", "danger", "attack", "hurt", "scared", "afraid",

        // Distress signals
        "sos", "mayday", "rescue", "save", "panic",

        // Medical emergencies
        "heart", "stroke", "bleeding", "unconscious",

        // Violence/crime
        "robbery", "assault", "kidnap", "stalker",

        // Natural disasters
        "earthquake", "flood", "tornado", "tsunami"
    )

    private var onPanicDetected: (() -> Unit)? = null
    private var onKeywordDetected: ((String) -> Unit)? = null
    private var onTextDetected: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "VoskSpeechService"
        private const val MODEL_NAME = "vosk-model-small-en-us"
        private const val SAMPLE_RATE = 16000.0f
    }

    suspend fun initialize(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // If we already have a model, we are good
                model?.let { return@withContext true }

                val targetDir = File(context.filesDir, MODEL_NAME)

                // Clean up any existing corrupted model
                if (targetDir.exists()) {
                    Log.d(TAG, "Cleaning up existing model directory...")
                    targetDir.deleteRecursively()
                }

                Log.d(TAG, "Extracting Vosk model ZIP from assets...")

                // Create target directory
                if (!targetDir.exists()) targetDir.mkdirs()

                // Extract ZIP with better error handling
                try {
                    extractModelFromAssets(context, targetDir)
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting Vosk model from assets", e)
                    return@withContext false
                }

                // Find the correct model path
                val modelPath = findValidModelPath(targetDir)
                if (modelPath == null) {
                    Log.e(TAG, "No valid model found after extraction")
                    logDirectoryContents(targetDir, "Final directory structure")
                    return@withContext false
                }

                Log.d(TAG, "Found valid model at: ${modelPath.absolutePath}")

                // Verify all required files exist and are not empty
                if (!verifyModelFiles(modelPath)) {
                    Log.e(TAG, "Model files verification failed")
                    return@withContext false
                }

                // Initialize the model
                model = Model(modelPath.absolutePath)
                Log.d(TAG, "Vosk model initialized successfully at: ${modelPath.absolutePath}")
                true

            } catch (e: IOException) {
                Log.e(TAG, "IOException initializing Vosk model", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error initializing model", e)
                false
            }
        }
    }

    private fun extractModelFromAssets(context: Context, targetDir: File) {
        context.assets.open("models/${MODEL_NAME}.zip").use { input ->
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val name = entry.name

                    // Skip macOS metadata and hidden files
                    val shouldSkip = name.startsWith("__MACOSX/") ||
                            name.startsWith("._") ||
                            name.contains("/.") ||
                            name.contains("..") ||
                            name.endsWith(".DS_Store")

                    if (!shouldSkip) {
                        val outFile = File(targetDir, name)

                        // Ensure we're not going outside target directory (path traversal protection)
                        if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                            Log.w(TAG, "Skipping potentially dangerous path: $name")
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                            Log.d(TAG, "Created directory: ${outFile.relativeTo(targetDir)}")
                        } else {
                            outFile.parentFile?.mkdirs()

                            java.io.FileOutputStream(outFile).use { fos ->
                                var totalBytes = 0L
                                var count = zis.read(buffer)
                                while (count != -1) {
                                    fos.write(buffer, 0, count)
                                    totalBytes += count
                                    count = zis.read(buffer)
                                }
                                fos.flush()

                                Log.d(TAG, "Extracted file: ${outFile.relativeTo(targetDir)} (${totalBytes} bytes)")
                            }
                        }
                    } else {
                        Log.d(TAG, "Skipped: $name")
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        logDirectoryContents(targetDir, "After extraction")
    }

    private fun findValidModelPath(baseDir: File): File? {
        Log.d(TAG, "Searching for valid model in: ${baseDir.absolutePath}")

        // Check if baseDir itself is valid
        if (isValidModelDir(baseDir)) {
            Log.d(TAG, "Base directory is valid model directory")
            return baseDir
        }

        // Search all subdirectories
        val queue = ArrayDeque<File>()
        baseDir.listFiles()?.filter { it.isDirectory }?.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            Log.d(TAG, "Checking directory: ${dir.relativeTo(baseDir)}")

            if (isValidModelDir(dir)) {
                Log.d(TAG, "Found valid model directory: ${dir.relativeTo(baseDir)}")
                return dir
            }

            // Add subdirectories to queue
            dir.listFiles()?.filter {
                it.isDirectory &&
                        !it.name.startsWith(".") &&
                        !it.name.startsWith("__MACOSX")
            }?.forEach { queue.add(it) }
        }

        return null
    }

    private fun isValidModelDir(dir: File): Boolean {
        Log.d(TAG, "Validating model directory: ${dir.absolutePath}")

        // Check for required files - updated for different model formats
        val coreRequiredFiles = listOf(
            "conf/model.conf",
            "am/final.mdl"
        )

        // Check for graph files - accepting different formats
        val graphFiles = listOf(
            "graph/HCLG.fst",     // Standard format
            "graph/HCLr.fst",     // Alternative format 1
            "graph/Gr.fst"        // Alternative format 2
        )

        val wordFiles = listOf(
            "graph/words.txt",    // Standard format
            "graph/words.int"     // Alternative format
        )

        val phoneFiles = listOf(
            "graph/phones.txt",   // Standard format
            "graph/phones.int",   // Alternative format
            "graph/phones/word_boundary.int" // Found in your logs
        )

        var hasRequiredFiles = 0
        var hasGraphFile = false
        var hasWordFile = false
        var hasPhoneFile = false

        // Check core required files
        for (filePath in coreRequiredFiles) {
            val file = File(dir, filePath)
            if (file.exists() && file.length() > 0) {
                hasRequiredFiles++
                Log.d(TAG, "Found core required file: $filePath (${file.length()} bytes)")
            } else {
                Log.d(TAG, "Missing core required file: $filePath")
            }
        }

        // Check for at least one graph file
        for (filePath in graphFiles) {
            val file = File(dir, filePath)
            if (file.exists() && file.length() > 1000) { // Must be reasonably large
                hasGraphFile = true
                Log.d(TAG, "Found graph file: $filePath (${file.length()} bytes)")
                break
            }
        }

        // Check for word files (optional for some models)
        for (filePath in wordFiles) {
            val file = File(dir, filePath)
            if (file.exists() && file.length() > 0) {
                hasWordFile = true
                Log.d(TAG, "Found word file: $filePath (${file.length()} bytes)")
                break
            }
        }

        // Check for phone files (optional for some models)
        for (filePath in phoneFiles) {
            val file = File(dir, filePath)
            if (file.exists() && file.length() > 0) {
                hasPhoneFile = true
                Log.d(TAG, "Found phone file: $filePath (${file.length()} bytes)")
                break
            }
        }

        // A valid model needs:
        // 1. Both core files (model.conf + final.mdl)
        // 2. At least one graph file
        val isValid = hasRequiredFiles >= 2 && hasGraphFile

        Log.d(TAG, "Directory validation result: $isValid")
        Log.d(TAG, "  - Core files: ${hasRequiredFiles}/2")
        Log.d(TAG, "  - Graph file: $hasGraphFile")
        Log.d(TAG, "  - Word file: $hasWordFile (optional)")
        Log.d(TAG, "  - Phone file: $hasPhoneFile (optional)")

        return isValid
    }

    // Also update the verifyModelFiles function to be more flexible
    private fun verifyModelFiles(modelDir: File): Boolean {
        val criticalFiles = listOf(
            "am/final.mdl",
            "conf/model.conf"
        )

        // Check for at least one graph file
        val graphFiles = listOf(
            "graph/HCLG.fst",
            "graph/HCLr.fst",
            "graph/Gr.fst"
        )

        // Verify critical files exist
        for (filePath in criticalFiles) {
            val file = File(modelDir, filePath)
            if (!file.exists()) {
                Log.e(TAG, "Critical model file missing: $filePath")
                return false
            }

            if (file.length() == 0L) {
                Log.e(TAG, "Critical model file is empty: $filePath")
                return false
            }
        }

        // Verify at least one graph file exists and is large enough
        var hasValidGraphFile = false
        for (filePath in graphFiles) {
            val file = File(modelDir, filePath)
            if (file.exists() && file.length() > 1000) {
                hasValidGraphFile = true
                Log.d(TAG, "Found valid graph file: $filePath (${file.length()} bytes)")
                break
            }
        }

        if (!hasValidGraphFile) {
            Log.e(TAG, "No valid graph file found")
            return false
        }

        Log.d(TAG, "Model files verification passed")
        return true
    }

    private fun logDirectoryContents(dir: File, label: String) {
        Log.d(TAG, "=== $label ===")
        logDirectoryRecursive(dir, dir, 0)
        Log.d(TAG, "=== End $label ===")
    }

    private fun logDirectoryRecursive(baseDir: File, currentDir: File, level: Int) {
        val indent = "  ".repeat(level)
        val files = currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        files?.forEach { file ->
            val relativePath = file.relativeTo(baseDir)
            if (file.isDirectory) {
                Log.d(TAG, "$indent📁 $relativePath/")
                if (level < 3) { // Limit recursion depth
                    logDirectoryRecursive(baseDir, file, level + 1)
                }
            } else {
                Log.d(TAG, "$indent📄 $relativePath (${file.length()} bytes)")
            }
        }
    }

    suspend fun startListening(
        onPanicKeywordDetected: () -> Unit,
        onKeywordDetected: (String) -> Unit = {},
        onTextDetected: (String) -> Unit = {}
    ): Boolean {
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return true
        }

        val currentModel = model
        if (currentModel == null) {
            Log.e(TAG, "Model not initialized")
            return false
        }

        this.onPanicDetected = onPanicKeywordDetected
        this.onKeywordDetected = onKeywordDetected
        this.onTextDetected = onTextDetected

        return withContext(Dispatchers.IO) {
            try {
                val recognizer = Recognizer(currentModel, SAMPLE_RATE)
                speechService = SpeechService(recognizer, SAMPLE_RATE)
                speechService?.startListening(this@VoskSpeechService)

                _isListening.value = true
                Log.d(TAG, "Started listening with Vosk")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                false
            }
        }
    }

    fun stopListening() {
        if (!_isListening.value) return

        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech service", e)
        }

        speechService = null
        _isListening.value = false
        _lastDetectedText.value = ""
        Log.d(TAG, "Stopped listening")
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // RecognitionListener implementation
    override fun onResult(hypothesis: String?) {
        hypothesis?.let { result ->
            try {
                val jsonResult = JSONObject(result)
                val text = jsonResult.optString("text", "").lowercase().trim()

                if (text.isNotEmpty()) {
                    Log.d(TAG, "Final recognition result: '$text'")
                    _lastDetectedText.value = text
                    onTextDetected?.invoke(text)

                    // Check for emergency keywords
                    val words = text.split(Regex("\\s+"))
                    var panicTriggered = false

                    for (word in words) {
                        val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
                        if (emergencyKeywords.contains(cleanWord)) {
                            Log.w(TAG, "🚨 EMERGENCY KEYWORD DETECTED: '$cleanWord' in text '$text'")
                            onKeywordDetected?.invoke(cleanWord)

                            if (!panicTriggered) {
                                onPanicDetected?.invoke()
                                panicTriggered = true
                            }
                        }
                    }

                    // Also check for phrase-based detection
                    for (keyword in emergencyKeywords) {
                        if (text.contains(keyword)) {
                            Log.w(TAG, "🚨 EMERGENCY PHRASE DETECTED: '$keyword' in text '$text'")
                            onKeywordDetected?.invoke(keyword)

                            if (!panicTriggered) {
                                onPanicDetected?.invoke()
                                panicTriggered = true
                            }
                        }
                    }
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recognition result", e)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // This method is called when speech recognition is completely finished
        onResult(hypothesis)
        Log.d(TAG, "Final result received: $hypothesis")
    }

    override fun onPartialResult(hypothesis: String?) {
        hypothesis?.let { result ->
            try {
                val jsonResult = JSONObject(result)
                val partial = jsonResult.optString("partial", "").lowercase().trim()

                if (partial.isNotEmpty() && partial.length > 2) {
                    Log.d(TAG, "Partial result: '$partial'")

                    // Check partial results for immediate emergency response
                    for (keyword in emergencyKeywords) {
                        if (partial.contains(keyword)) {
                            Log.w(TAG, "🚨 EMERGENCY DETECTED IN PARTIAL: '$keyword'")
                            onKeywordDetected?.invoke(keyword)
                            onPanicDetected?.invoke()
                            break
                        }
                    }
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Error processing partial result", e)
            }
        }
    }

    override fun onError(exception: Exception?) {
        Log.e(TAG, "Speech recognition error", exception)

        // Try to restart listening on recoverable errors
        val shouldRestart = _isListening.value && (exception?.message?.contains("timeout") != true)
        if (shouldRestart) {
            Log.d(TAG, "Attempting to restart speech recognition...")

            // Stop and restart
            try {
                speechService?.stop()
                Thread.sleep(100)

                val currentModel = model
                if (currentModel != null) {
                    val recognizer = Recognizer(currentModel, SAMPLE_RATE)
                    speechService = SpeechService(recognizer, SAMPLE_RATE)
                    speechService?.startListening(this)
                    Log.d(TAG, "Speech recognition restarted successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart speech recognition", e)
                _isListening.value = false
            }
        }
    }

    override fun onTimeout() {
        Log.d(TAG, "Speech recognition timeout - this is normal")
        // Timeout is normal behavior, don't treat as error
    }

    // Get list of emergency keywords for UI display
    fun getEmergencyKeywords(): List<String> {
        return emergencyKeywords.sorted()
    }
}