package com.bitchat.android.onboarding

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple preferences for onboarding state
 */
class OnboardingPrefs private constructor(context: Context) {
    
    companion object {
        private const val PREF_NAME = "zii_onboarding"
        private const val KEY_WELCOME_COMPLETED = "welcome_completed"
        private const val KEY_HINTS_ENABLED = "hints_enabled"
        
        @Volatile
        private var INSTANCE: OnboardingPrefs? = null
        
        fun getInstance(context: Context): OnboardingPrefs {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingPrefs(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    var isWelcomeCompleted: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_WELCOME_COMPLETED, value).apply()
    
    var areHintsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HINTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HINTS_ENABLED, value).apply()
    
    fun markHintShown(hintId: String) {
        prefs.edit().putBoolean("hint_$hintId", true).apply()
    }
    
    fun isHintShown(hintId: String): Boolean {
        return prefs.getBoolean("hint_$hintId", false)
    }
    
    fun resetOnboarding() {
        prefs.edit().clear().apply()
    }
}