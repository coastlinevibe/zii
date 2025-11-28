package com.bitchat.android.onboarding

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Manages the interactive onboarding tutorial for new users
 * Shows step-by-step walkthrough of all UI elements
 */
class OnboardingManager private constructor(private val context: Context) {
    
    companion object {
        private const val PREF_NAME = "zii_onboarding"
        private const val KEY_COMPLETED = "tutorial_completed"
        private const val KEY_CURRENT_STEP = "current_step"
        
        @Volatile
        private var INSTANCE: OnboardingManager? = null
        
        fun getInstance(context: Context): OnboardingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    enum class TutorialStep(val id: Int, val title: String, val description: String) {
        WELCOME(0, "👋 Welcome to Zii Chat!", "Let's take a quick tour of the features"),
        APP_SETTINGS(1, "⚙️ App Settings", "Tap 'zii/' to access settings, limits, and app info"),
        CHANGE_NAME(2, "✏️ Your Name", "Tap your name to change it - make it unique!"),
        MESH_STATUS(3, "🌐 Who's Online", "Tap 'mesh' to see nearby Zii Chat users"),
        LOCATION_NOTES(4, "📝 Location Notes", "Tap the note icon to leave messages at this location"),
        PEOPLE_LIST(5, "👥 Friends", "Tap the people icon to select who to chat with"),
        SEND_PHOTOS(6, "📷 Send Images", "Tap camera icon to send photos (1MB limit)"),
        VOICE_NOTES(7, "🎤 Voice Messages", "Hold mic button to record voice notes (10 seconds max)"),
        COMPLETE(8, "You're Ready!", "Enjoy using Zii Chat's mesh network features!")
    }
    
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val _currentStep = MutableLiveData<TutorialStep?>(null)
    val currentStep: LiveData<TutorialStep?> = _currentStep
    
    private val _isActive = MutableLiveData(false)
    val isActive: LiveData<Boolean> = _isActive
    
    /**
     * Check if user has completed onboarding
     */
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }
    
    /**
     * Start the onboarding tutorial
     */
    fun startTutorial() {
        if (hasCompletedOnboarding()) {
            // Allow replay of tutorial
            prefs.edit().putBoolean(KEY_COMPLETED, false).apply()
        }
        
        _isActive.value = true
        _currentStep.value = TutorialStep.WELCOME
        saveCurrentStep(TutorialStep.WELCOME)
    }
    
    /**
     * Move to next step in tutorial
     */
    fun nextStep() {
        val current = _currentStep.value ?: return
        val nextStepId = current.id + 1
        val nextStep = TutorialStep.values().find { it.id == nextStepId }
        
        if (nextStep != null) {
            _currentStep.value = nextStep
            saveCurrentStep(nextStep)
            
            if (nextStep == TutorialStep.COMPLETE) {
                // Auto-complete after showing final step
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    completeTutorial()
                }, 3000) // Show completion message for 3 seconds
            }
        } else {
            completeTutorial()
        }
    }
    
    /**
     * Go to previous step
     */
    fun previousStep() {
        val current = _currentStep.value ?: return
        val prevStepId = current.id - 1
        val prevStep = TutorialStep.values().find { it.id == prevStepId }
        
        if (prevStep != null) {
            _currentStep.value = prevStep
            saveCurrentStep(prevStep)
        }
    }
    
    /**
     * Skip tutorial
     */
    fun skipTutorial() {
        completeTutorial()
    }
    
    /**
     * Complete tutorial and mark as done
     */
    fun completeTutorial() {
        _isActive.value = false
        _currentStep.value = null
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .remove(KEY_CURRENT_STEP)
            .apply()
    }
    
    /**
     * Resume tutorial from saved step (if app was closed during tutorial)
     */
    fun resumeTutorialIfNeeded() {
        if (!hasCompletedOnboarding()) {
            val savedStepId = prefs.getInt(KEY_CURRENT_STEP, -1)
            val savedStep = TutorialStep.values().find { it.id == savedStepId }
            
            if (savedStep != null) {
                _isActive.value = true
                _currentStep.value = savedStep
            } else {
                // No saved step, start from beginning
                startTutorial()
            }
        }
    }
    
    /**
     * Get tutorial progress (for progress indicator)
     */
    fun getTutorialProgress(): Pair<Int, Int> {
        val current = _currentStep.value?.id ?: 0
        val total = TutorialStep.values().size - 1 // Exclude COMPLETE step
        return Pair(current, total)
    }
    
    private fun saveCurrentStep(step: TutorialStep) {
        prefs.edit().putInt(KEY_CURRENT_STEP, step.id).apply()
    }
}