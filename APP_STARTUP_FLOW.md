# Zii Chat - App Startup Flow

**Version:** 1.7.5-welcome-name  
**Last Updated:** November 28, 2024

## Complete Startup Sequence

### 1. App Launch
- `MainActivity.onCreate()` initializes
- Sets up `BluetoothMeshService`
- Initializes managers:
  - `BluetoothStatusManager` - Handles Bluetooth state
  - `LocationStatusManager` - Handles Location services state
  - `BatteryOptimizationManager` - Handles battery optimization
  - `PermissionManager` - Handles runtime permissions
  - `OnboardingCoordinator` - Coordinates permission requests

### 2. Initial State Check
- App starts in `OnboardingState.CHECKING`
- Shows initializing screen briefly (500ms delay)
- Checks if all permissions are granted

### 3. Permission Request (If Needed)
- **If permissions NOT granted:**
  - Requests all required permissions:
    - `BLUETOOTH_CONNECT`
    - `BLUETOOTH_SCAN`
    - `BLUETOOTH_ADVERTISE`
    - `ACCESS_FINE_LOCATION`
    - `ACCESS_COARSE_LOCATION`
    - `POST_NOTIFICATIONS`
    - `RECORD_AUDIO`
    - `READ_MEDIA_IMAGES`
  - User must grant permissions to continue
  - After permissions granted → proceeds to step 4

- **If permissions already granted:**
  - Proceeds directly to step 4

### 4. Bluetooth Check
- Checks if Bluetooth is enabled
- **If Bluetooth OFF:**
  - Shows Bluetooth enable screen
  - User clicks "Enable Bluetooth"
  - System Bluetooth enable dialog appears
  - After Bluetooth enabled → proceeds to step 5
- **If Bluetooth ON:**
  - Proceeds directly to step 5
- **If Bluetooth NOT SUPPORTED:**
  - Shows error screen (device incompatible)

### 5. Location Check
- Checks if Location services are enabled
- **If Location OFF:**
  - Shows Location enable screen
  - User clicks "Enable Location"
  - System Location settings dialog appears
  - After Location enabled → proceeds to step 6
- **If Location ON:**
  - Proceeds directly to step 6
- **If Location NOT AVAILABLE:**
  - Shows error screen (device incompatible)

### 6. App Initialization
- Initializes PoW (Proof of Work) preferences
- Initializes Location Notes Manager
- Sets up mesh service delegate
- Starts `BluetoothMeshService`
- Handles any notification intents
- Sets onboarding state to `COMPLETE`

### 7. Welcome Screen (Always Shown)
- **First Time Users:**
  - Shows "Welcome to Zii Chat"
  - 3-screen swipeable tutorial:
    - Screen 1: Welcome + Name input field (empty or @anonXXXX)
    - Screen 2: "No Internet Required" info
    - Screen 3: "Ready to Start" info
  - User enters their name on Screen 1
  - User can skip or swipe through screens
  - Clicking "Get Started" saves name and proceeds

- **Returning Users:**
  - Shows "Welcome back"
  - 3-screen swipeable tutorial:
    - Screen 1: Welcome back + Name input field (shows saved name, editable)
    - Screen 2: "No Internet Required" info
    - Screen 3: "Ready to Start" info
  - User can edit their name if desired
  - User can skip or swipe through screens
  - Clicking "Get Started" saves any name changes and proceeds

### 8. Main Chat Screen
- Shows `ChatScreen` (main app interface)
- User's name appears in header as "@username"
- User can tap their name to edit it anytime
- App is fully functional

---

## State Diagram

```
App Launch
    ↓
Checking State (500ms)
    ↓
Permissions Granted? ──No──→ Request Permissions ──→ Granted ──┐
    ↓ Yes                                                       │
    ←───────────────────────────────────────────────────────────┘
    ↓
Bluetooth Enabled? ──No──→ Show BT Prompt ──→ User Enables ──┐
    ↓ Yes                                                      │
    ←──────────────────────────────────────────────────────────┘
    ↓
Location Enabled? ──No──→ Show Location Prompt ──→ User Enables ──┐
    ↓ Yes                                                           │
    ←───────────────────────────────────────────────────────────────┘
    ↓
Initialize App
    ↓
Welcome Screen (ALWAYS)
    ├─ First Time: "Welcome to Zii Chat" + Name Input
    └─ Returning: "Welcome back" + Name Input (pre-filled)
    ↓
Main Chat Screen
```

---

## Key Features

### Always-On Welcome Screen
- Shows on **every app start** (not just first time)
- Allows users to verify/update their name each time
- Provides consistent entry experience

### Name Management
- **Initial Setup:** User enters name on welcome screen
- **Persistence:** Name is saved and persists across app restarts
- **Display:** Name shows as "@username" in main screen header
- **Editing:** 
  - Can edit on welcome screen every time
  - Can tap name in main screen header to edit anytime

### Error Handling
- If Bluetooth not supported → Shows error screen
- If Location not available → Shows error screen
- If permissions denied → Cannot proceed (shows permission explanation)

---

## Technical Details

### State Management
- Uses `MainViewModel.onboardingState` to track progress
- States: `CHECKING`, `BLUETOOTH_CHECK`, `LOCATION_CHECK`, `INITIALIZING`, `COMPLETE`, `ERROR`

### Persistence
- Welcome completion status: `OnboardingPrefs.isWelcomeCompleted`
- User nickname: Stored in `ChatViewModel.nickname` (LiveData)
- Preferences saved using SharedPreferences

### Background/Foreground Handling
- `onResume()`: Sets app to foreground state
- `onPause()`: Sets app to background state
- Mesh service adjusts behavior based on app state

---

## Version History

- **1.7.5-welcome-name:** Added always-on welcome screen with name input
- **1.7.4-bt-permission-fix:** Fixed Bluetooth enable by requesting permissions first
- **1.7.3-theme-fix:** Fixed theme buttons and removed duplicate tutorial button
- **1.7.2-bt-loc-fix2:** Restored Bluetooth/Location check screens
