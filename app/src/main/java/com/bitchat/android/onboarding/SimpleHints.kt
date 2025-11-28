package com.bitchat.android.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * Simple hint system - just highlight + text
 */
@Composable
fun SimpleHint(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    // Pulsing animation for highlight
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier
            .zIndex(10f)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        // Hint text card
        Card(
            modifier = Modifier
                .offset(y = (-40).dp)
                .widthIn(max = 200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hint manager to control which hints to show
 */
class SimpleHintManager {
    private val _currentHint = mutableStateOf<String?>(null)
    val currentHint: State<String?> = _currentHint
    
    private val shownHints = mutableSetOf<String>()
    
    fun showHint(hintId: String, text: String) {
        if (!shownHints.contains(hintId)) {
            _currentHint.value = text
        }
    }
    
    fun dismissHint(hintId: String) {
        shownHints.add(hintId)
        _currentHint.value = null
    }
    
    fun isHintShown(hintId: String): Boolean {
        return shownHints.contains(hintId)
    }
}

/**
 * Composable to remember hint manager
 */
@Composable
fun rememberSimpleHintManager(): SimpleHintManager {
    return remember { SimpleHintManager() }
}

/**
 * Modifier to add simple hint highlighting
 */
@Composable
fun Modifier.simpleHint(
    hintId: String,
    hintManager: SimpleHintManager,
    showCondition: Boolean = true
): Modifier {
    return if (showCondition && !hintManager.isHintShown(hintId)) {
        this.border(
            width = 2.dp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        this
    }
}