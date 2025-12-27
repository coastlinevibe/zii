package com.zii.school.school

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zii.school.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (LoginResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { SchoolConfigManager.getInstance(context) }
    val schoolConfig = remember { configManager.loadConfig() }
    
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var phoneOrStudentId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // Role options
    val roleOptions = listOf(
        UserRole.STUDENT to "Student",
        UserRole.PARENT to "Parent",
        UserRole.TEACHER to "Teacher",
        UserRole.ASSISTANT to "Assistant Teacher"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // School logo/icon
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "School Logo",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // School name
        Text(
            text = schoolConfig.school.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // School code
        Text(
            text = "School Code: ${schoolConfig.school.code}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Role selector dropdown
        ExposedDropdownMenuBox(
            expanded = isRoleDropdownExpanded,
            onExpandedChange = { isRoleDropdownExpanded = !isRoleDropdownExpanded }
        ) {
            OutlinedTextField(
                value = roleOptions.find { it.first == selectedRole }?.second ?: "Student",
                onValueChange = {},
                readOnly = true,
                label = { Text("I am a") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = isRoleDropdownExpanded,
                onDismissRequest = { isRoleDropdownExpanded = false }
            ) {
                roleOptions.forEach { (role, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedRole = role
                            isRoleDropdownExpanded = false
                            errorMessage = null
                            phoneOrStudentId = "" // Clear input when role changes
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login instructions based on role
        Text(
            text = when (selectedRole) {
                UserRole.STUDENT -> "Enter your student ID"
                UserRole.PARENT -> "Enter your registered phone number"
                UserRole.TEACHER, UserRole.ASSISTANT -> "Enter your registered phone number"
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Input field
        OutlinedTextField(
            value = phoneOrStudentId,
            onValueChange = { 
                phoneOrStudentId = it
                errorMessage = null // Clear error on input
            },
            label = { 
                Text(
                    if (selectedRole == UserRole.STUDENT) "Student ID" 
                    else "Phone Number"
                )
            },
            placeholder = { 
                Text(
                    if (selectedRole == UserRole.STUDENT) "e.g., ABC00001" 
                    else "e.g., 0821234567"
                )
            },
            singleLine = true,
            enabled = !isLoading,
            isError = errorMessage != null,
            supportingText = if (errorMessage != null) {
                { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (selectedRole == UserRole.STUDENT) KeyboardType.Text else KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (phoneOrStudentId.isNotBlank()) {
                        isLoading = true
                        val result = configManager.validateLogin(phoneOrStudentId.trim())
                        isLoading = false
                        
                        if (result != null && result.role == selectedRole) {
                            onLoginSuccess(result)
                        } else if (result != null) {
                            errorMessage = "This credential belongs to a ${result.role.name.lowercase()}, not a ${selectedRole.name.lowercase()}"
                        } else {
                            errorMessage = when (selectedRole) {
                                UserRole.STUDENT -> "Invalid student ID"
                                else -> "Invalid phone number"
                            }
                        }
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Login button
        Button(
            onClick = {
                keyboardController?.hide()
                if (phoneOrStudentId.isNotBlank()) {
                    isLoading = true
                    val result = configManager.validateLogin(phoneOrStudentId.trim())
                    isLoading = false
                    
                    if (result != null && result.role == selectedRole) {
                        onLoginSuccess(result)
                    } else if (result != null) {
                        errorMessage = "This credential belongs to a ${result.role.name.lowercase()}, not a ${selectedRole.name.lowercase()}"
                    } else {
                        errorMessage = when (selectedRole) {
                            UserRole.STUDENT -> "Invalid student ID"
                            else -> "Invalid phone number"
                        }
                    }
                }
            },
            enabled = !isLoading && phoneOrStudentId.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Help text based on selected role
        Text(
            text = when (selectedRole) {
                UserRole.STUDENT -> "Students use their student ID card number\nExample: ABC00001"
                UserRole.PARENT -> "Parents use the phone number registered with the school"
                UserRole.TEACHER, UserRole.ASSISTANT -> "Teachers use the phone number registered with the school"
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}
