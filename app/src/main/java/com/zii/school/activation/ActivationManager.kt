package com.zii.school.activation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Manages activation code validation and subscription status
 * Implements offline entry + online validation flow
 */
class ActivationManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ActivationManager"
        private const val API_URL = "https://zii-website-theta.vercel.app/api/activate"
        private const val HOUR_IN_MILLIS = 60 * 60 * 1000L
        private const val GRACE_PERIOD = 12 * HOUR_IN_MILLIS
        
        @Volatile
        private var INSTANCE: ActivationManager? = null
        
        fun getInstance(context: Context): ActivationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActivationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    val storage = ActivationStorage(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _activationState = MutableStateFlow<ActivationStatus>(ActivationStatus.NotActivated)
    val activationState: StateFlow<ActivationStatus> = _activationState.asStateFlow()
    
    init {
        // Check current status on init
        updateActivationStatus()
    }
    
    /**
     * Enter activation code (offline or online)
     */
    fun enterCode(code: String): Result {
        Log.d(TAG, "Entering code: ${code.take(4)}...")
        
        // Validate format
        if (!validateCodeFormat(code)) {
            return Result.Error("Invalid code format. Use: XXXX-XXXX-XXXX-XXXX")
        }
        
        // Store pending activation
        val entryTimestamp = System.currentTimeMillis()
        val validationDeadline = entryTimestamp + HOUR_IN_MILLIS
        
        storage.saveActivation(
            code = code,
            state = ActivationState.PENDING,
            entryTimestamp = entryTimestamp,
            validationDeadline = validationDeadline
        )
        
        // Try immediate validation if online
        if (isOnline()) {
            scope.launch {
                validateOnline(code, entryTimestamp)
            }
        } else {
            _activationState.value = ActivationStatus.PendingValidation(
                timeRemaining = HOUR_IN_MILLIS
            )
        }
        
        return Result.Pending("Connect to WiFi within 1 hour to complete activation")
    }
    
    /**
     * Validate code with server
     */
    suspend fun validateOnline(code: String, entryTimestamp: Long): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Validating code online...")
        
        try {
            val deviceId = getDeviceId()
            val response = callActivationAPI(code, entryTimestamp, deviceId)
            
            return@withContext when {
                response.success -> {
                    // Store validated activation
                    storage.saveActivation(
                        code = code,
                        state = ActivationState.VALIDATED,
                        permanentToken = response.token,
                        expiryDate = response.expiryDate ?: 0L,
                        durationDays = response.durationDays ?: 0
                    )
                    
                    // Schedule expiry notifications
                    scheduleExpiryNotifications(response.expiryDate ?: 0L)
                    
                    withContext(Dispatchers.Main) {
                        updateActivationStatus()
                    }
                    
                    Result.Success("Activation successful! Valid for ${response.durationDays} days")
                }
                response.error == "Code already activated" -> {
                    storage.saveActivation(state = ActivationState.REVOKED)
                    withContext(Dispatchers.Main) {
                        updateActivationStatus()
                    }
                    Result.Error("Code already used on another device")
                }
                else -> {
                    Result.Error(response.error ?: "Validation failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Validation error", e)
            Result.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Check current activation status
     */
    fun updateActivationStatus() {
        val data = storage.loadActivation()
        val now = System.currentTimeMillis()
        
        val status = when (data.state) {
            ActivationState.NONE -> ActivationStatus.NotActivated
            
            ActivationState.PENDING -> {
                if (now > data.validationDeadline) {
                    ActivationStatus.ValidationExpired
                } else {
                    ActivationStatus.PendingValidation(
                        timeRemaining = data.validationDeadline - now
                    )
                }
            }
            
            ActivationState.VALIDATED -> {
                val expiryDate = data.expiryDate ?: 0L
                when {
                    now < expiryDate -> {
                        ActivationStatus.Active(
                            expiryDate = expiryDate,
                            daysRemaining = ((expiryDate - now) / (24 * 60 * 60 * 1000)).toInt()
                        )
                    }
                    now < expiryDate + GRACE_PERIOD -> {
                        ActivationStatus.GracePeriod(
                            timeRemaining = expiryDate + GRACE_PERIOD - now
                        )
                    }
                    else -> {
                        storage.saveActivation(state = ActivationState.EXPIRED)
                        ActivationStatus.Expired
                    }
                }
            }
            
            ActivationState.REVOKED -> ActivationStatus.Revoked
            ActivationState.EXPIRED -> ActivationStatus.Expired
        }
        
        _activationState.value = status
        Log.d(TAG, "Activation status: $status")
    }
    
    /**
     * Validate code format
     */
    private fun validateCodeFormat(code: String): Boolean {
        val regex = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return regex.matches(code.uppercase())
    }
    
    /**
     * Call activation API
     */
    private fun callActivationAPI(code: String, entryTimestamp: Long, deviceId: String): APIResponse {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            // Send request
            val jsonBody = JSONObject().apply {
                put("code", code)
                put("entryTimestamp", entryTimestamp)
                put("deviceId", deviceId)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            // Read response
            val responseCode = connection.responseCode
            val responseBody = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            
            val json = JSONObject(responseBody)
            
            return if (responseCode == 200 && json.optBoolean("success", false)) {
                APIResponse(
                    success = true,
                    token = json.optString("token"),
                    expiryDate = json.optLong("expiryDate"),
                    durationDays = json.optInt("durationDays")
                )
            } else {
                APIResponse(
                    success = false,
                    error = json.optString("error", "Unknown error")
                )
            }
            
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Check if device is online
     */
    private fun isOnline(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
            )
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get device ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    
    /**
     * Schedule expiry notifications
     * Notifies user at: 7 days, 3 days, 1 day, and on expiry
     */
    private fun scheduleExpiryNotifications(expiryDate: Long) {
        Log.d(TAG, "Scheduling expiry notifications for: $expiryDate")
        
        val now = System.currentTimeMillis()
        val daysUntilExpiry = ((expiryDate - now) / (24 * 60 * 60 * 1000)).toInt()
        
        // Use WorkManager to schedule notifications
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        // Cancel any existing notification work
        workManager.cancelAllWorkByTag("activation_expiry")
        
        // Schedule notifications at different intervals
        val notificationTimes = listOf(
            7 to "Your Zii Chat subscription expires in 7 days",
            3 to "Your Zii Chat subscription expires in 3 days",
            1 to "Your Zii Chat subscription expires tomorrow",
            0 to "Your Zii Chat subscription has expired"
        )
        
        notificationTimes.forEach { (daysBeforeExpiry, message) ->
            val notificationTime = expiryDate - (daysBeforeExpiry * 24 * 60 * 60 * 1000)
            
            // Only schedule if notification time is in the future
            if (notificationTime > now) {
                val delay = notificationTime - now
                
                val notificationWork = androidx.work.OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(
                        androidx.work.workDataOf(
                            "message" to message,
                            "daysRemaining" to daysBeforeExpiry
                        )
                    )
                    .addTag("activation_expiry")
                    .build()
                
                workManager.enqueue(notificationWork)
                Log.d(TAG, "Scheduled notification: $message in ${delay / 1000 / 60 / 60} hours")
            }
        }
    }
    
    /**
     * Clear activation (for testing)
     */
    fun clearActivation() {
        storage.clear()
        _activationState.value = ActivationStatus.NotActivated
    }
}

// Data classes
data class APIResponse(
    val success: Boolean,
    val token: String? = null,
    val expiryDate: Long? = null,
    val durationDays: Int? = null,
    val error: String? = null
)

sealed class Result {
    data class Success(val message: String) : Result()
    data class Pending(val message: String) : Result()
    data class Error(val message: String) : Result()
}

sealed class ActivationStatus {
    object NotActivated : ActivationStatus()
    data class PendingValidation(val timeRemaining: Long) : ActivationStatus()
    data class Active(val expiryDate: Long, val daysRemaining: Int) : ActivationStatus()
    data class GracePeriod(val timeRemaining: Long) : ActivationStatus()
    object ValidationExpired : ActivationStatus()
    object Expired : ActivationStatus()
    object Revoked : ActivationStatus()
}

enum class ActivationState {
    NONE,
    PENDING,
    VALIDATED,
    EXPIRED,
    REVOKED
}
