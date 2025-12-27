package com.zii.school.school

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main home screen after login - shows role-specific dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolHomeScreen(
    loginResult: LoginResult,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { SchoolConfigManager.getInstance(context) }
    val schoolConfig = remember { configManager.loadConfig() }
    val currentStatus = remember { configManager.getCurrentStatus() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(schoolConfig.school.name) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome message
            Text(
                text = "Welcome, ${loginResult.name}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Role badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = when (loginResult.role) {
                        UserRole.TEACHER -> "Teacher"
                        UserRole.ASSISTANT -> "Assistant Teacher"
                        UserRole.PARENT -> "Parent"
                        UserRole.STUDENT -> "Student"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grade/Class info
            if (loginResult.gradeName != null) {
                Text(
                    text = loginResult.gradeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Role-specific content
            when (loginResult.role) {
                UserRole.TEACHER, UserRole.ASSISTANT -> TeacherDashboard(loginResult, configManager)
                UserRole.PARENT -> ParentDashboard(loginResult, configManager)
                UserRole.STUDENT -> StudentDashboard(loginResult, configManager)
            }
        }
    }
}

@Composable
private fun TeacherDashboard(
    loginResult: LoginResult,
    configManager: SchoolConfigManager
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Teacher Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        DashboardCard(
            icon = Icons.Default.People,
            title = "Class Chat",
            description = "Communicate with parents in your class"
        )
        
        DashboardCard(
            icon = Icons.Default.Notifications,
            title = "Send Notice",
            description = "Send announcements to parents"
        )
        
        DashboardCard(
            icon = Icons.Default.CheckCircle,
            title = "Roll Call",
            description = "Mark student attendance"
        )
        
        if (loginResult.role == UserRole.TEACHER) {
            DashboardCard(
                icon = Icons.Default.Warning,
                title = "Emergency Alert",
                description = "Send urgent notifications"
            )
        }
    }
}

@Composable
private fun ParentDashboard(
    loginResult: LoginResult,
    configManager: SchoolConfigManager
) {
    val children = remember { configManager.getChildrenForParent(loginResult.userId) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Parent Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Children list
        if (children.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your Children",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    children.forEach { child ->
                        val grade = configManager.getGrade(child.gradeId)
                        Text(
                            text = "• ${child.name} (${grade?.name})",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        DashboardCard(
            icon = Icons.Default.Chat,
            title = "Class Chat",
            description = "Chat with teachers"
        )
        
        DashboardCard(
            icon = Icons.Default.DirectionsCar,
            title = "Pickup",
            description = "Notify teacher of pickup"
        )
        
        DashboardCard(
            icon = Icons.Default.Notifications,
            title = "Notices",
            description = "View school announcements"
        )
    }
}

@Composable
private fun StudentDashboard(
    loginResult: LoginResult,
    configManager: SchoolConfigManager
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Student Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        DashboardCard(
            icon = Icons.Default.Chat,
            title = "Chat with Parents",
            description = "Message your parents during breaks"
        )
        
        DashboardCard(
            icon = Icons.Default.Notifications,
            title = "Notices",
            description = "View class announcements"
        )
        
        // Timetable info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Communication Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• You can chat during breaks\n" +
                           "• You can chat after school\n" +
                           "• Chat is disabled during class time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* TODO: Navigate to feature */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
