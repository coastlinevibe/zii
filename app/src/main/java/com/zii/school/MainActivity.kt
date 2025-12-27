package com.zii.school

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.zii.school.mesh.BluetoothMeshService
import com.zii.school.onboarding.BluetoothCheckScreen
import com.zii.school.onboarding.BluetoothStatus
import com.zii.school.onboarding.BluetoothStatusManager
import com.zii.school.onboarding.BatteryOptimizationManager
import com.zii.school.onboarding.BatteryOptimizationPreferenceManager
import com.zii.school.onboarding.BatteryOptimizationScreen
import com.zii.school.onboarding.BatteryOptimizationStatus
import com.zii.school.onboarding.InitializationErrorScreen
import com.zii.school.onboarding.InitializingScreen
import com.zii.school.onboarding.LocationCheckScreen
import com.zii.school.onboarding.LocationStatus
import com.zii.school.onboarding.LocationStatusManager
import com.zii.school.onboarding.OnboardingCoordinator
import com.zii.school.onboarding.OnboardingState
import com.zii.school.onboarding.PermissionExplanationScreen
import com.zii.school.onboarding.PermissionManager
import com.zii.school.ui.ChatScreen
import com.zii.school.ui.ChatViewModel
import com.zii.school.ui.OrientationAwareActivity
import com.zii.school.ui.theme.ZiiTheme
import com.zii.school.school.LoginScreen
import com.zii.school.school.SchoolHomeScreen
import com.zii.school.school.SchoolSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : OrientationAwareActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var onboardingCoordinator: OnboardingCoordinator
    private lateinit var bluetoothStatusManager: BluetoothStatusManager
    private lateinit var locationStatusManager: LocationStatusManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    
    // Core mesh service - BT mesh only (v2.5.0)
    private lateinit var meshService: BluetoothMeshService
    
    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels { 
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application, meshService) as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android look
        enableEdgeToEdge()

        // Initialize permission management
        permissionManager = PermissionManager(this)
        
        // Initialize BT mesh service
        meshService = BluetoothMeshService(this)
        
        Log.d("MainActivity", "=== Zii Chat initialized (BT Mesh only) ===")
        
        bluetoothStatusManager = BluetoothStatusManager(
            activity = this,
            context = this,
            onBluetoothEnabled = ::handleBluetoothEnabled,
            onBluetoothDisabled = ::handleBluetoothDisabled
        )
        locationStatusManager = LocationStatusManager(
            activity = this,
            context = this,
            onLocationEnabled = ::handleLocationEnabled,
            onLocationDisabled = ::handleLocationDisabled
        )
        batteryOptimizationManager = BatteryOptimizationManager(
            activity = this,
            context = this,
            onBatteryOptimizationDisabled = ::handleBatteryOptimizationDisabled,
            onBatteryOptimizationFailed = ::handleBatteryOptimizationFailed
        )
        onboardingCoordinator = OnboardingCoordinator(
            activity = this,
            permissionManager = permissionManager,
            onOnboardingComplete = ::handleOnboardingComplete,
            onOnboardingFailed = ::handleOnboardingFailed
        )
        
        setContent {
            ZiiTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    OnboardingFlowScreen(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    )
                }
            }
        }
        
        // Collect state changes in a lifecycle-aware manner
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.onboardingState.collect { state ->
                    handleOnboardingStateChange(state)
                }
            }
        }
        
        // Only start onboarding process if we're in the initial CHECKING state
        // This prevents restarting onboarding on configuration changes
        if (mainViewModel.onboardingState.value == OnboardingState.CHECKING) {
            checkOnboardingStatus()
        }
    }

    
    @Composable
    private fun OnboardingFlowScreen(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val schoolSession = remember { SchoolSession.getInstance(context) }
        var isLoggedIn by remember { mutableStateOf(schoolSession.isLoggedIn()) }
        var loginResult by remember { mutableStateOf(schoolSession.getLoginResult()) }
        
        // If not logged in, show login screen first
        if (!isLoggedIn || loginResult == null) {
            LoginScreen(
                onLoginSuccess = { result ->
                    schoolSession.saveLogin(result)
                    loginResult = result
                    isLoggedIn = true
                },
                modifier = modifier
            )
            return
        }
        
        // User is logged in, show school home screen
        SchoolHomeScreen(
            loginResult = loginResult!!,
            onLogout = {
                schoolSession.logout()
                isLoggedIn = false
                loginResult = null
            },
            modifier = modifier
        )
        
        /* OLD ONBOARDING FLOW - COMMENTED OUT FOR NOW
        val onboardingState by mainViewModel.onboardingState.collectAsState()
        val bluetoothStatus by mainViewModel.bluetoothStatus.collectAsState()
        val locationStatus by mainViewModel.locationStatus.collectAsState()
        val batteryOptimizationStatus by mainViewModel.batteryOptimizationStatus.collectAsState()
        val errorMessage by mainViewModel.errorMessage.collectAsState()
        val isBluetoothLoading by mainViewModel.isBluetoothLoading.collectAsState()
        val isLocationLoading by mainViewModel.isLocationLoading.collectAsState()
        val isBatteryOptimizationLoading by mainViewModel.isBatteryOptimizationLoading.collectAsState()

        when (onboardingState) {
            OnboardingState.BLUETOOTH_CHECK -> {
                BluetoothCheckScreen(
                    modifier = modifier,
                    status = bluetoothStatus,
                    onEnableBluetooth = {
                        mainViewModel.updateBluetoothLoading(true)
                        bluetoothStatusManager.requestEnableBluetooth()
                    },
                    onRetry = {
                        checkBluetoothAndProceed()
                    },
                    isLoading = isBluetoothLoading
                )
            }
            
            OnboardingState.LOCATION_CHECK -> {
                LocationCheckScreen(
                    modifier = modifier,
                    status = locationStatus,
                    onEnableLocation = {
                        mainViewModel.updateLocationLoading(true)
                        locationStatusManager.requestEnableLocation()
                    },
                    onRetry = {
                        checkLocationAndProceed()
                    },
                    isLoading = isLocationLoading
                )
            }
            
            OnboardingState.CHECKING, OnboardingState.INITIALIZING -> {
                // Show initialization screen while checking bluetooth/location
                InitializingScreen(modifier)
            }
            
            OnboardingState.COMPLETE -> {
                // Set up back navigation handling for the chat screen
                val backCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Let ChatViewModel handle navigation state
                        val handled = chatViewModel.handleBackPressed()
                        if (!handled) {
                            // If ChatViewModel doesn't handle it, disable this callback
                            // and let the system handle it (which will exit the app)
                            this.isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            this.isEnabled = true
                        }
                    }
                }

                // Add the callback - this will be automatically removed when the activity is destroyed
                onBackPressedDispatcher.addCallback(this, backCallback)
                
                // ACTIVATION DISABLED FOR TESTING
                // TODO: Re-enable activation before production release
                /*
                val activationManager = remember { com.zii.school.activation.ActivationManager.getInstance(context) }
                val activationStatus by activationManager.activationState.collectAsState()
                var activationComplete by remember { mutableStateOf(false) }
                
                // Show activation screen if not active
                if (!activationComplete && activationStatus !is com.zii.school.activation.ActivationStatus.Active) {
                    com.zii.school.activation.ActivationScreen(
                        onActivationComplete = {
                            activationComplete = true
                        }
                    )
                } else {
                */
                
                // Always show welcome screens on every app start
                    val onboardingPrefs = com.zii.school.onboarding.OnboardingPrefs.getInstance(this@MainActivity)
                    val isFirstTime = !onboardingPrefs.isWelcomeCompleted
                    var showWelcome by remember { mutableStateOf(true) }
                    val currentNickname by chatViewModel.nickname.observeAsState("")
                    
                    if (showWelcome) {
                        // Show welcome screens with name input
                        com.zii.school.onboarding.SimpleWelcomeScreens(
                            currentNickname = currentNickname,
                            isFirstTime = isFirstTime,
                            onComplete = { newNickname ->
                                // Save nickname if changed
                                if (newNickname != currentNickname) {
                                    chatViewModel.setNickname(newNickname)
                                }
                                // Mark welcome as completed for first-time users
                                if (isFirstTime) {
                                    onboardingPrefs.isWelcomeCompleted = true
                                }
                                showWelcome = false
                            }
                        )
                    } else {
                        // Show main chat screen
                        ChatScreen(viewModel = chatViewModel)
                    }
                // } // End of activation check (commented out for testing)
            }
            
            else -> {
                // Show initialization screen for other states
                InitializingScreen(modifier)
            }
        }
        */
    }
    
    private fun handleOnboardingStateChange(state: OnboardingState) {
        when (state) {
            OnboardingState.COMPLETE -> {
                // App is fully initialized, mesh service is running
                android.util.Log.d("MainActivity", "Onboarding completed - app ready")
            }
            OnboardingState.ERROR -> {
                android.util.Log.e("MainActivity", "Onboarding error state reached")
            }
            else -> {}
        }
    }
    
    private fun checkOnboardingStatus() {
        Log.d("MainActivity", "Checking onboarding status")
        
        lifecycleScope.launch {
            // Small delay to show the checking state
            delay(500)
            
            // Check if permissions are granted
            if (!permissionManager.areAllPermissionsGranted()) {
                Log.d("MainActivity", "Permissions not granted, requesting permissions")
                onboardingCoordinator.requestPermissions()
            } else {
                // Permissions already granted, check Bluetooth
                checkBluetoothAndProceed()
            }
        }
    }
    
    private fun checkBluetoothAndProceed() {
        Log.d("MainActivity", "Checking Bluetooth status")
        
        bluetoothStatusManager.logBluetoothStatus()
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
        
        when (mainViewModel.bluetoothStatus.value) {
            BluetoothStatus.ENABLED -> {
                // Bluetooth is enabled, check location services next
                checkLocationAndProceed()
            }
            BluetoothStatus.DISABLED -> {
                // Show Bluetooth enable screen
                Log.d("MainActivity", "Bluetooth disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
            BluetoothStatus.NOT_SUPPORTED -> {
                // Device doesn't support Bluetooth
                Log.e("MainActivity", "Bluetooth not supported")
                mainViewModel.updateOnboardingState(OnboardingState.BLUETOOTH_CHECK)
                mainViewModel.updateBluetoothLoading(false)
            }
        }
    }
    
    private fun checkLocationAndProceed() {
        Log.d("MainActivity", "Checking location services status")
        
        locationStatusManager.logLocationStatus()
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())
        
        when (mainViewModel.locationStatus.value) {
            LocationStatus.ENABLED -> {
                // Location services enabled, initialize app
                initializeApp()
            }
            LocationStatus.DISABLED -> {
                // Show location enable screen
                Log.d("MainActivity", "Location services disabled, showing enable screen")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
            LocationStatus.NOT_AVAILABLE -> {
                Log.e("MainActivity", "Location services not available")
                mainViewModel.updateOnboardingState(OnboardingState.LOCATION_CHECK)
                mainViewModel.updateLocationLoading(false)
            }
        }
    }
    
    private fun handleBluetoothEnabled() {
        Log.d("MainActivity", "Bluetooth enabled by user")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(BluetoothStatus.ENABLED)
        // Proceed to check location
        checkLocationAndProceed()
    }

    private fun handleLocationEnabled() {
        Log.d("MainActivity", "Location services enabled by user")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(LocationStatus.ENABLED)
        // Proceed to initialize app
        initializeApp()
    }

    private fun handleLocationDisabled(message: String) {
        Log.w("MainActivity", "Location services disabled or failed: $message")
        mainViewModel.updateLocationLoading(false)
        mainViewModel.updateLocationStatus(locationStatusManager.checkLocationStatus())
    }
    
    private fun handleBluetoothDisabled(message: String) {
        Log.w("MainActivity", "Bluetooth disabled or failed: $message")
        mainViewModel.updateBluetoothLoading(false)
        mainViewModel.updateBluetoothStatus(bluetoothStatusManager.checkBluetoothStatus())
    }
    
    private fun handleOnboardingComplete() {
        Log.d("MainActivity", "Onboarding completed - permissions granted")
        // Now that we have permissions, check Bluetooth
        checkBluetoothAndProceed()
    }
    
    private fun handleOnboardingFailed(message: String) {
        Log.e("MainActivity", "Onboarding failed: $message")
        mainViewModel.updateErrorMessage(message)
        mainViewModel.updateOnboardingState(OnboardingState.ERROR)
    }
    
    private fun handleBatteryOptimizationDisabled() {
        android.util.Log.d("MainActivity", "Battery optimization disabled by user")
        mainViewModel.updateBatteryOptimizationLoading(false)
        mainViewModel.updateBatteryOptimizationStatus(BatteryOptimizationStatus.DISABLED)
    }
    
    private fun handleBatteryOptimizationFailed(message: String) {
        android.util.Log.w("MainActivity", "Battery optimization disable failed: $message")
        mainViewModel.updateBatteryOptimizationLoading(false)
        val currentStatus = when {
            !batteryOptimizationManager.isBatteryOptimizationSupported() -> BatteryOptimizationStatus.NOT_SUPPORTED
            batteryOptimizationManager.isBatteryOptimizationDisabled() -> BatteryOptimizationStatus.DISABLED
            else -> BatteryOptimizationStatus.ENABLED
        }
        mainViewModel.updateBatteryOptimizationStatus(currentStatus)
    }
    
    private fun initializeApp() {
        Log.d("MainActivity", "Starting app initialization")
        
        lifecycleScope.launch {
            try {
                delay(1000)
                
                Log.d("MainActivity", "Initializing chat system")

                // Set up mesh service delegate and start services
                meshService.delegate = chatViewModel
                meshService.startServices()
                
                Log.d("MainActivity", "Mesh service started successfully")
                
                // Handle any notification intent
                handleNotificationIntent(intent)
                
                // Small delay to ensure mesh service is fully initialized
                delay(500)
                Log.d("MainActivity", "App initialization complete")
                mainViewModel.updateOnboardingState(OnboardingState.COMPLETE)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to initialize app", e)
                handleOnboardingFailed("Failed to initialize the app: ${e.message}")
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification intents when app is already running
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            handleNotificationIntent(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check Bluetooth and Location status on resume and handle accordingly
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            // Set app foreground state
            meshService.connectionManager.setAppBackgroundState(false)
            chatViewModel.setAppBackgroundState(false)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Only set background state if app is fully initialized
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            // Set app background state
            meshService.connectionManager.setAppBackgroundState(true)
            chatViewModel.setAppBackgroundState(true)
        }
    }
    
    /**
     * Handle intents from notification clicks - open specific private chat or geohash chat
     */
    private fun handleNotificationIntent(intent: Intent) {
        val shouldOpenPrivateChat = intent.getBooleanExtra(
            com.zii.school.ui.NotificationManager.EXTRA_OPEN_PRIVATE_CHAT, 
            false
        )
        
        val shouldOpenGeohashChat = intent.getBooleanExtra(
            com.zii.school.ui.NotificationManager.EXTRA_OPEN_GEOHASH_CHAT,
            false
        )
        
        when {
            shouldOpenPrivateChat -> {
                val peerID = intent.getStringExtra(com.zii.school.ui.NotificationManager.EXTRA_PEER_ID)
                val senderNickname = intent.getStringExtra(com.zii.school.ui.NotificationManager.EXTRA_SENDER_NICKNAME)
                
                if (peerID != null) {
                    Log.d("MainActivity", "Opening private chat with $senderNickname (peerID: $peerID) from notification")
                    
                    // Open the private chat with this peer
                    chatViewModel.startPrivateChat(peerID)
                    
                    // Clear notifications for this sender since user is now viewing the chat
                    chatViewModel.clearNotificationsForSender(peerID)
                }
            }
            
            shouldOpenGeohashChat -> {
                // Geohash removed - BT mesh only
            }
        }
    }

    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup location status manager
        try {
            locationStatusManager.cleanup()
            Log.d("MainActivity", "Location status manager cleaned up successfully")
        } catch (e: Exception) {
            Log.w("MainActivity", "Error cleaning up location status manager: ${e.message}")
        }
        
        // Stop mesh services if app was fully initialized
        if (mainViewModel.onboardingState.value == OnboardingState.COMPLETE) {
            try {
                meshService.stopServices()
                Log.d("MainActivity", "Mesh services stopped successfully")
            } catch (e: Exception) {
                Log.w("MainActivity", "Error stopping mesh services in onDestroy: ${e.message}")
            }
            
            // Briar removed - BT mesh only
        }
    }
}
