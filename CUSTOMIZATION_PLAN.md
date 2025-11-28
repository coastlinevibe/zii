# Zii Mobile - BitChat Customization Plan

## Overview
Converting BitChat Android to Zii Mobile while keeping the proven Bluetooth mesh networking core.

## Phase 1: Branding & Basic Setup ✅
- [x] Clone BitChat Android repository
- [ ] Change package name from `com.bitchat.android` to `com.zii.mobile`
- [ ] Update app name, icon, and branding
- [ ] Update manifest and build files

## Phase 2: UI Customization 🔄
- [ ] Replace BitChat's terminal UI with Zii's React-style interface
- [ ] Implement Zii's contact management UI
- [ ] Add Zii's wallet/crypto UI components
- [ ] Integrate Zii's theme system

## Phase 3: Feature Integration 🔄
- [ ] Add Zii's profile/identity system
- [ ] Integrate Zii's delta sync over Bluetooth mesh
- [ ] Add Zii's backup/recovery features
- [ ] Implement Zii's QR code sync

## Phase 4: Testing & Deployment 🚀
- [ ] Test Bluetooth mesh between devices
- [ ] Verify cross-platform compatibility (with Zii web/desktop)
- [ ] Build APK for distribution
- [ ] Test on your mobile devices

## Key Files to Customize

### Keep Unchanged (Bluetooth Core):
- `mesh/BluetoothMeshService.kt` - Core mesh networking
- `mesh/BluetoothConnectionManager.kt` - BLE connections
- `crypto/EncryptionService.kt` - Noise protocol
- `protocol/BitchatPacket.kt` - Binary protocol

### Customize (UI & Features):
- `MainActivity.kt` - Main app entry point
- `ui/ChatScreen.kt` - Main interface
- `BitchatApplication.kt` - App initialization
- `res/` - All UI resources, strings, themes

### Replace (Zii-specific):
- Add Zii's contact system
- Add Zii's wallet features
- Add Zii's sync system
- Add Zii's backup system

## Success Criteria
✅ Bluetooth mesh networking works between devices  
✅ Zii's features work over the mesh  
✅ UI matches Zii's design  
✅ Compatible with existing Zii ecosystem  