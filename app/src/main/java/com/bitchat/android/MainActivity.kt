package com.bitchat.android

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
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.onboarding.BluetoothCheckScreen
import com.bitchat.android.onboarding.BluetoothStatus
import com.bitchat.android.onboarding.BluetoothStatusManager
import com.bitchat.android.onboarding.BatteryOptimizationManager
import com.bitchat.android.onboarding.BatteryOptimizationPreferenceManager
import com.bitchat.android.onboarding.BatteryOptimizationScreen
import com.bitchat.android.onboarding.BatteryOptimizationStatus
import com.bitchat.android.onboarding.InitializationErrorScreen
import com.bitchat.android.onboarding.InitializingScreen
import com.bitchat.android.onboarding.LocationCheckScreen
import com.bitchat.android.onboarding.LocationStatus
import com.bitchat.android.onboarding.LocationStatusManager
import com.bitchat.android.onboarding.OnboardingCoordinator
import com.bitchat.android.onboarding.OnboardingState
import com.bitchat.android.onboarding.PermissionExplanationScreen
import com.bitchat.android.onboarding.PermissionManager
import com.bitchat.android.ui.ChatScreen
import com.bitchat.android.ui.ChatViewModel
import com.bitchat.android.ui.OrientationAwareActivity
import com.bitchat.android.ui.theme.ZiiTheme
import com.bitchat.android.nostr.PoWPreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : OrientationAwareActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var onboardingCoordinator: OnboardingCoordinator
    private lateinit var bluetoothStatusManager: BluetoothStatusManager
    private lateinit var locationStatusManager: LocationStatusManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    
    // Core mesh service - managed at app level
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
        // Initialize core mesh service first
        meshService = BluetoothMeshService(this)
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
                
                // Always show welcome screens on every app start
                val onboardingPrefs = com.bitchat.android.onboarding.OnboardingPrefs.getInstance(this@MainActivity)
                val isFirstTime = !onboardingPrefs.isWelcomeCompleted
                var showWelcome by remember { mutableStateOf(true) }
                val currentNickname by chatViewModel.nickname.observeAsState("")
                
                if (showWelcome) {
                    // Show welcome screens with name input
                    com.bitchat.android.onboarding.SimpleWelcomeScreens(
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
            }
            
            else -> {
                // Show initialization screen for other states
                InitializingScreen(modifier)
            }
        }
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
                
                // Initialize PoW preferences early in the initialization process
                PoWPreferenceManager.init(this@MainActivity)
                Log.d("MainActivity", "PoW preferences initialized")
                
                // Initialize Location Notes Manager (extracted to separate file)
                com.bitchat.android.nostr.LocationNotesInitializer.initialize(this@MainActivity)

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
            com.bitchat.android.ui.NotificationManager.EXTRA_OPEN_PRIVATE_CHAT, 
            false
        )
        
        val shouldOpenGeohashChat = intent.getBooleanExtra(
            com.bitchat.android.ui.NotificationManager.EXTRA_OPEN_GEOHASH_CHAT,
            false
        )
        
        when {
            shouldOpenPrivateChat -> {
                val peerID = intent.getStringExtra(com.bitchat.android.ui.NotificationManager.EXTRA_PEER_ID)
                val senderNickname = intent.getStringExtra(com.bitchat.android.ui.NotificationManager.EXTRA_SENDER_NICKNAME)
                
                if (peerID != null) {
                    Log.d("MainActivity", "Opening private chat with $senderNickname (peerID: $peerID) from notification")
                    
                    // Open the private chat with this peer
                    chatViewModel.startPrivateChat(peerID)
                    
                    // Clear notifications for this sender since user is now viewing the chat
                    chatViewModel.clearNotificationsForSender(peerID)
                }
            }
            
            shouldOpenGeohashChat -> {
                val geohash = intent.getStringExtra(com.bitchat.android.ui.NotificationManager.EXTRA_GEOHASH)
                
                if (geohash != null) {
                    Log.d("MainActivity", "Opening geohash chat #$geohash from notification")
                    
                    // Switch to the geohash channel - create appropriate geohash channel level
                    val level = when (geohash.length) {
                        7 -> com.bitchat.android.geohash.GeohashChannelLevel.BLOCK
                        6 -> com.bitchat.android.geohash.GeohashChannelLevel.NEIGHBORHOOD
                        5 -> com.bitchat.android.geohash.GeohashChannelLevel.CITY
                        4 -> com.bitchat.android.geohash.GeohashChannelLevel.PROVINCE
                        2 -> com.bitchat.android.geohash.GeohashChannelLevel.REGION
                        else -> com.bitchat.android.geohash.GeohashChannelLevel.CITY // Default fallback
                    }
                    val geohashChannel = com.bitchat.android.geohash.GeohashChannel(level, geohash)
                    val channelId = com.bitchat.android.geohash.ChannelID.Location(geohashChannel)
                    chatViewModel.selectLocationChannel(channelId)
                    
                    // Update current geohash state for notifications
                    chatViewModel.setCurrentGeohash(geohash)
                    
                    // Clear notifications for this geohash since user is now viewing it
                    chatViewModel.clearNotificationsForGeohash(geohash)
                }
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
        }
    }
}
