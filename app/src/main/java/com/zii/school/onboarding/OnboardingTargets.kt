package com.zii.school.onboarding

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.Modifier

/**
 * Tracks UI element positions for onboarding highlighting
 */
class OnboardingTargets {
    private val _targets = mutableStateMapOf<OnboardingManager.TutorialStep, Rect>()
    val targets: Map<OnboardingManager.TutorialStep, Rect> = _targets
    
    fun setTarget(step: OnboardingManager.TutorialStep, rect: Rect) {
        _targets[step] = rect
    }
    
    fun clearTarget(step: OnboardingManager.TutorialStep) {
        _targets.remove(step)
    }
    
    fun clearAll() {
        _targets.clear()
    }
    
    /**
     * Add a target and return a modifier for the UI element
     */
    @Composable
    fun addTarget(key: String): Modifier {
        // Map string keys to tutorial steps
        val step = when (key) {
            "welcome_screen" -> OnboardingManager.TutorialStep.WELCOME
            "app_title" -> OnboardingManager.TutorialStep.APP_SETTINGS
            "nickname_editor" -> OnboardingManager.TutorialStep.CHANGE_NAME
            "location_channels" -> OnboardingManager.TutorialStep.MESH_STATUS
            "location_notes" -> OnboardingManager.TutorialStep.LOCATION_NOTES
            "peer_counter" -> OnboardingManager.TutorialStep.PEOPLE_LIST
            "send_photos" -> OnboardingManager.TutorialStep.SEND_PHOTOS
            "voice_notes" -> OnboardingManager.TutorialStep.VOICE_NOTES
            else -> return Modifier // Return empty modifier for unknown keys
        }
        
        return Modifier.onboardingTarget(step, this)
    }
}

/**
 * Modifier to track an element's position for onboarding
 */
@Composable
fun Modifier.onboardingTarget(
    step: OnboardingManager.TutorialStep,
    targets: OnboardingTargets,
    enabled: Boolean = true
): Modifier {
    return if (enabled) {
        this.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()
            
            val rect = Rect(
                offset = position,
                size = size
            )
            
            targets.setTarget(step, rect)
        }
    } else {
        this
    }
}

/**
 * Create onboarding targets state
 */
@Composable
fun rememberOnboardingTargets(): OnboardingTargets {
    return remember { OnboardingTargets() }
}