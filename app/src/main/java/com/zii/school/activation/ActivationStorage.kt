package com.zii.school.activation

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for activation data using EncryptedSharedPreferences
 */
class ActivationStorage(context: Context) {
    
    private val prefs: SharedPreferences
    
    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = EncryptedSharedPreferences.create(
            context,
            "activation_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveActivation(
        code: String? = null,
        state: ActivationState? = null,
        entryTimestamp: Long? = null,
        validationDeadline: Long? = null,
        permanentToken: String? = null,
        expiryDate: Long? = null,
        durationDays: Int? = null
    ) {
        prefs.edit().apply {
            code?.let { putString("code", it) }
            state?.let { putString("state", it.name) }
            entryTimestamp?.let { putLong("entry_timestamp", it) }
            validationDeadline?.let { putLong("validation_deadline", it) }
            permanentToken?.let { putString("permanent_token", it) }
            if (expiryDate != null) { putLong("expiry_date", expiryDate) }
            durationDays?.let { putInt("duration_days", it) }
            apply()
        }
    }
    
    fun loadActivation(): ActivationData {
        return ActivationData(
            code = prefs.getString("code", "") ?: "",
            state = try {
                ActivationState.valueOf(prefs.getString("state", "NONE") ?: "NONE")
            } catch (e: Exception) {
                ActivationState.NONE
            },
            entryTimestamp = prefs.getLong("entry_timestamp", 0L),
            validationDeadline = prefs.getLong("validation_deadline", 0L),
            permanentToken = prefs.getString("permanent_token", null),
            expiryDate = prefs.getLong("expiry_date", 0L).takeIf { it > 0 },
            durationDays = prefs.getInt("duration_days", 0)
        )
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}

data class ActivationData(
    val code: String,
    val state: ActivationState,
    val entryTimestamp: Long,
    val validationDeadline: Long,
    val permanentToken: String?,
    val expiryDate: Long?,
    val durationDays: Int
)
