package com.zii.school.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Interactive onboarding overlay that highlights UI elements
 * Shows step-by-step tutorial with spotlight effect
 */
@Composable
fun OnboardingOverlay(
    onboardingManager: OnboardingManager,
    targets: OnboardingTargets
) {
    val currentStep by onboardingManager.currentStep.observeAsState()
    val isActive by onboardingManager.isActive.observeAsState(false)
    
    if (!isActive || currentStep == null) return
    
    val step = currentStep!!
    val targetRect = targets.targets[step]
    
    // Get theme colors outside Canvas
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Animation for spotlight effect
    val pulseAnimation by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Animation for tooltip appearance
    val tooltipAlpha by animateFloatAsState(
        targetValue = if (targetRect != null) 1f else 0f,
        animationSpec = tween(300),
        label = "tooltip"
    )
    
    Dialog(
        onDismissRequest = { /* Prevent dismissal by tapping outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dark overlay with spotlight cutout
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw dark overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f), // Reduced opacity
                    size = size
                )
                
                // Create spotlight and highlight if target exists
                targetRect?.let { rect ->
                    val padding = 16.dp.toPx()
                    val expandedRect = Rect(
                        offset = Offset(
                            rect.left - padding,
                            rect.top - padding
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            rect.width + padding * 2,
                            rect.height + padding * 2
                        )
                    )
                    
                    // Cut out spotlight area (clear the dark overlay)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = expandedRect.topLeft,
                        size = expandedRect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                    
                    // Draw bright pulsing border around target
                    val pulseSize = 4.dp.toPx() * pulseAnimation
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.9f),
                        topLeft = Offset(
                            expandedRect.left - pulseSize,
                            expandedRect.top - pulseSize
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            expandedRect.width + pulseSize * 2,
                            expandedRect.height + pulseSize * 2
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    
                    // Draw inner glow effect
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.3f),
                        topLeft = expandedRect.topLeft,
                        size = expandedRect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        // Tooltip and cards drawn OUTSIDE the overlay to avoid brownish filter
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Smart tooltip positioned near highlighted element
            if (targetRect != null && tooltipAlpha > 0f) {
                OnboardingTooltip(
                    step = step,
                    targetRect = targetRect,
                    onNext = { onboardingManager.nextStep() },
                    onSkip = { onboardingManager.skipTutorial() },
                    alpha = tooltipAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Fallback tutorial card when no target is available
            if (targetRect == null) {
                OnboardingCard(
                    step = step,
                    onNext = { onboardingManager.nextStep() },
                    onPrevious = { onboardingManager.previousStep() },
                    onSkip = { onboardingManager.skipTutorial() },
                    progress = onboardingManager.getTutorialProgress(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Tutorial content card with step information and navigation
 */
@Composable
private fun OnboardingCard(
    step: OnboardingManager.TutorialStep,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    progress: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A202C) // Darker background for better contrast
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${progress.first + 1} of ${progress.second}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
                
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Skip tutorial",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step title
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                fontWeight = FontWeight.Bold,
                color = Color.White, // Force white text for visibility
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Step description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                color = Color.White, // Force white text for visibility
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (step.id > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Next/Complete button
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (step == OnboardingManager.TutorialStep.COMPLETE) "Finish" else "Next"
                    )
                    if (step != OnboardingManager.TutorialStep.COMPLETE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
/**
 *
 Smart tooltip that positions itself near the highlighted element
 */
@Composable
private fun OnboardingTooltip(
    step: OnboardingManager.TutorialStep,
    targetRect: Rect,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    
    Box(modifier = modifier) {
        // Calculate tooltip position (below target, or above if not enough space)
        val tooltipOffset = with(density) {
            val screenHeight = configuration.screenHeightDp.dp.toPx()
            val screenWidth = configuration.screenWidthDp.dp.toPx()
            val tooltipWidth = 300.dp.toPx()
            val tooltipHeight = 160.dp.toPx()
            
            // Calculate center X position of target
            val targetCenterX = (targetRect.left + targetRect.right) / 2f
            
            // Calculate X position (keep tooltip on screen)
            val x = (targetCenterX - tooltipWidth / 2f).coerceIn(
                16.dp.toPx(), 
                screenWidth - tooltipWidth - 16.dp.toPx()
            )
            
            // Calculate Y position - Position close to the highlighted element
            val spaceBelow = screenHeight - targetRect.bottom
            val y = if (spaceBelow > tooltipHeight + 40.dp.toPx()) {
                // Position just below target with small gap
                targetRect.bottom + 8.dp.toPx()
            } else {
                // Position just above target with small gap  
                targetRect.top - tooltipHeight - 8.dp.toPx()
            }
            
            Offset(x, y)
        }
        
        // Tooltip card
        Card(
            modifier = Modifier
                .offset { 
                    IntOffset(
                        tooltipOffset.x.toInt(),
                        tooltipOffset.y.toInt()
                    )
                }
                .width(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A202C) // Darker background for better contrast
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step title
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    fontWeight = FontWeight.Bold,
                    color = Color.White, // Force white text for visibility
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Step description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    color = Color.White, // Force white text for visibility
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip", fontSize = 14.sp)
                    }
                    
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (step == OnboardingManager.TutorialStep.COMPLETE) "Finish" else "Next",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}