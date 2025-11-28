# Zii Chat - Complete Product Overview

## Vision
Decentralized mesh messaging app with offline Bluetooth chat and online geohash channels. Privacy-first, censorship-resistant communication.

## Current Version
**v1.8.0** (November 2024)

## Core Features

### 1. Offline Mesh Chat (Bluetooth)
- Direct device-to-device communication via Bluetooth LE
- No internet or servers required
- Messages relay through nearby devices to extend range
- End-to-end encryption using Noise Protocol
- Works in areas with no connectivity

### 2. Online Geohash Channels
- Location-based chat channels using geohash precision
- Connect with people in your area via Nostr relays
- Multiple precision levels: Block, Neighborhood, City, Province, Region
- Automatic channel switching based on location
- Public channels (anyone can join)

### 3. End-to-End Encryption
- Noise Protocol for mesh chat (ephemeral keys)
- NIP-17 for geohash DMs (private messages)
- Channel messages are public (no encryption)
- Private messages are always encrypted

### 4. Location Notes
- Post text notes to your current location
- Notes visible to others in same geohash (8-char precision)
- Stored on Nostr relays
- 1-week retention (startup period)
- Weather, news, sports updates (coming soon)

### 5. Privacy Features
- Tor integration for anonymous relay connections
- No phone number or email required
- Pseudonymous identities (nickname + hash)
- No message history stored on device (ephemeral)
- Panic clear (triple-tap to wipe all data)


## App Startup Flow

### 1. Permissions Check
- Bluetooth permission
- Location permission (required for Bluetooth scanning on Android)
- Notification permission (optional)

### 2. Bluetooth Check
- Verify Bluetooth is enabled
- Prompt user to enable if disabled
- Show loading screen during check

### 3. Location Services Check
- Verify location services enabled
- Prompt user to enable if disabled
- Required for geohash features

### 4. Welcome Screen
- Shows on every app start
- First time: "Welcome to Zii Chat"
- Returning: "Welcome back"
- User can set/edit nickname
- Nickname appears as @username in main screen

### 5. Main Chat Screen
- Ready to use immediately
- Mesh service starts automatically
- Location channels available if location enabled

## User Interface

### Main Screen
- **Header**: "Zii" (green, tappable) + @nickname
- **Peer counter**: Shows connected Bluetooth devices
- **Channel counter**: Shows joined geohash channels
- **Location button**: Access geohash channels
- **Notes button**: Post location notes
- **Menu button**: Opens sidebar

### Sidebar
- **Channels**: List of joined geohash channels
- **People**: List of connected peers (Bluetooth + Geohash)
- **Favorites**: Starred contacts
- **Settings**: App preferences

### About/Settings Sheet
- **Features**: Offline Mesh, Online Geohash, E2E Encryption
- **Feed**: Weather/news/sports (coming soon) - Green
- **Chatrooms**: Public chat rooms (coming soon) - Purple
- **Limits**: Image 1MB, Voice 10s
- **Appearance**: Light/Dark theme
- **Advanced Settings** (collapsible):
  - Anti-Spam: Proof-of-work for geohash messages
  - Privacy Chat: Tor routing for relay connections


## Technical Architecture

### Mesh Layer (Bluetooth)
- **Protocol**: Bluetooth Low Energy (BLE)
- **Service UUID**: Custom GATT service
- **Encryption**: Noise Protocol (XX pattern)
- **Message Format**: Protobuf
- **Max Message Size**: 1MB (images), 10s (voice)
- **Relay**: Messages hop through nearby devices

### Geohash Layer (Nostr)
- **Protocol**: Nostr (Notes and Other Stuff Transmitted by Relays)
- **Event Types**: 
  - Kind 1: Text notes (location notes)
  - Kind 20000: Ephemeral events (geohash chat)
  - Kind 1059: Gift wraps (NIP-17 DMs)
- **Relays**: Geo-distributed Nostr relays
- **Geohash Precision**: 8 characters (building-level)

### Identity System
- **Mesh**: Ephemeral Noise keys (per-session)
- **Geohash**: Derived from geohash (location-specific identity)
- **Favorites**: Persistent identity via fingerprint
- **Nickname**: User-chosen display name

### Storage
- **Messages**: In-memory only (ephemeral)
- **Favorites**: Encrypted SharedPreferences
- **Settings**: Encrypted SharedPreferences
- **Location Notes**: Nostr relays (7 days) + Local cache (last 50 notes)

## Planned Features

### Feed (Coming Soon)
- Weather updates for current location
- Local news aggregation
- Sports scores
- RSS feed integration
- Customizable sources

### Chatrooms/Spaces (Coming Soon)
- Public chat rooms (not location-based)
- Topic-based channels
- Moderated spaces
- Private rooms with invite codes

### Monetization (Activation System)
- Activation codes for subscription access
- Offline code entry + online validation
- Single-use enforcement
- Multiple tiers (5-365 days)
- See: ACTIVATION_SYSTEM_PLAN.md


## Development Roadmap

### Phase 1: Core Features (COMPLETE ✅)
- [x] Bluetooth mesh networking
- [x] Geohash channels via Nostr
- [x] End-to-end encryption
- [x] Location notes
- [x] Tor integration
- [x] Welcome screen with nickname
- [x] About/Settings UI redesign

### Phase 2: Monetization (IN PROGRESS 🔄)
- [ ] Activation code system
- [ ] Vercel API for validation
- [ ] Code generator tool
- [ ] Expiry notifications
- [ ] Grace period handling

### Phase 3: Content Features (DEFERRED ⏸️)
**Trigger: 1000+ active users in high-volume areas**
- [ ] Feed implementation
  - [ ] Weather API integration
  - [ ] News aggregation
  - [ ] Sports scores
- [ ] Chatrooms/Spaces
  - [ ] Room creation
  - [ ] Moderation tools
  - [ ] Invite system

### Phase 4: Polish & Scale (FUTURE 🚀)
- [ ] iOS version
- [ ] Desktop clients (Windows, Mac, Linux)
- [ ] Voice/Video calls
- [ ] File sharing improvements
- [ ] Group chats
- [ ] Message reactions
- [ ] Read receipts (optional)

## Known Issues & Limitations

### Current Limitations
1. **Location notes not persisting**: Relay storage issue (investigating)
2. **Bluetooth range**: ~30-100m depending on obstacles
3. **Battery usage**: Bluetooth scanning can drain battery
4. **Android only**: iOS version not yet available
5. **No message history**: All messages ephemeral

### Workarounds
1. Location notes: Increased retention to 7 days, added better logging
2. Bluetooth range: Messages relay through nearby devices
3. Battery: Optimized scanning intervals
4. iOS: Planned for Phase 4
5. Message history: By design (privacy feature)


## Questions & Clarifications Needed

### 1. Monetization Details ✅ COMPLETE
- ✅ Pricing tiers defined (R5, R15, R50, R150)
- ✅ Distribution channels defined (website, WhatsApp, shops)
- ✅ Single-use enforcement strategy defined
- ✅ **Payment processing**: FastPay for R15+ (website), EFT/cash for WhatsApp
- ✅ **Shop partnerships**: 20% discount, upfront payment, partner keeps profit
- ✅ **Free tier**: NONE - All access requires activation code

### 2. Feed Feature ⏸️ DEFERRED
- ⏸️ **Status**: Not in v1.0 - Will develop after 1000+ users
- ⏸️ Waiting for high-volume user areas before implementation

### 3. Chatrooms/Spaces ⏸️ DEFERRED
- ⏸️ **Status**: Not in v1.0 - Will develop after 1000+ users
- ⏸️ Waiting for high-volume user areas before implementation

### 4. Location Notes Issue ✅ CLARIFIED
- ✅ Increased retention to 7 days
- ✅ Added better logging
- ✅ **Local persistence**: App keeps last 50 notes locally
- ❓ **Root cause**: Need to test with actual relay responses
- ❓ **Relay selection**: Are we using the right relays for geo-notes?

### 5. Scaling & Infrastructure
- ✅ Vercel setup planned for activation API
- ❓ **Relay hosting**: Self-host Nostr relays or use public ones?
- ❓ **CDN**: Need CDN for media files? (images, voice notes)
- ❓ **Analytics**: What metrics to track? (Mixpanel, PostHog, custom?)
- ❓ **Monitoring**: Error tracking? (Sentry, Bugsnag?)

### 6. User Experience
- ✅ Welcome screen on every start
- ✅ Green "Zii" text to invite tapping
- ❓ **Onboarding**: Need tutorial/hints for first-time users?
- ❓ **Help/Support**: In-app help section? FAQ?
- ❓ **Feedback**: How do users report bugs/suggestions?

### 7. Legal & Compliance
- ❓ **Terms of Service**: Need legal review?
- ❓ **Privacy Policy**: POPIA compliance? (South African law)
- ❓ **Content moderation**: Policy for illegal content?
- ❓ **Age restriction**: 18+ or allow minors?
- ❓ **Data retention**: What data do we keep? For how long?

### 8. Marketing & Launch ✅ CLARIFIED
- ✅ **Launch strategy**: Soft launch - Share link and let it grow organically
- ✅ **Growth expectation**: Slow start (days/week), may accelerate after initial adoption
- ✅ **No rush**: Gradual user acquisition expected
- ❓ **App stores**: Google Play only or also F-Droid, APK direct?
- ❓ **Marketing channels**: Social media, ads, word-of-mouth?
- ❓ **Press kit**: Screenshots, description, press release?


## Technical Decisions Made

### Architecture
- ✅ **Android-first**: Kotlin + Jetpack Compose
- ✅ **Bluetooth**: BLE for mesh networking
- ✅ **Nostr**: Decentralized relay network for geohash
- ✅ **Encryption**: Noise Protocol (mesh), NIP-17 (DMs)
- ✅ **Storage**: Ephemeral (no message history)

### UI/UX
- ✅ **Theme**: Dark mode default, light mode available
- ✅ **Colors**: Green for new features, primary blue for existing
- ✅ **Navigation**: Bottom sheet for settings, sidebar for channels
- ✅ **Onboarding**: Welcome screen on every start
- ✅ **Simplicity**: Minimal UI, focus on messaging

### Privacy
- ✅ **No registration**: No phone/email required
- ✅ **Pseudonymous**: Nickname + hash identifier
- ✅ **Tor support**: Optional anonymous relay connections
- ✅ **Panic clear**: Triple-tap to wipe all data
- ✅ **No tracking**: No analytics by default

### Monetization
- ✅ **Activation codes**: Offline entry, online validation
- ✅ **Vercel API**: Serverless validation endpoint
- ✅ **Single-use**: Timestamp-based enforcement
- ✅ **Grace periods**: 1 hour (validation), 12 hours (expiry)

## File Structure

```
zii-mobile/
├── app/
│   ├── src/main/java/com/bitchat/android/
│   │   ├── MainActivity.kt              # App entry point
│   │   ├── MainViewModel.kt             # Main state management
│   │   ├── mesh/                        # Bluetooth mesh layer
│   │   │   ├── BluetoothMeshService.kt
│   │   │   └── BluetoothPacketBroadcaster.kt
│   │   ├── geohash/                     # Location-based features
│   │   │   ├── LocationChannelManager.kt
│   │   │   └── GeohashChannel.kt
│   │   ├── nostr/                       # Nostr protocol
│   │   │   ├── NostrProtocol.kt
│   │   │   ├── LocationNotesManager.kt
│   │   │   └── GeohashRepository.kt
│   │   ├── noise/                       # Encryption
│   │   │   └── NoiseChannelEncryption.kt
│   │   ├── ui/                          # UI components
│   │   │   ├── ChatScreen.kt
│   │   │   ├── ChatHeader.kt
│   │   │   ├── AboutSheet.kt
│   │   │   └── LocationNotesSheet.kt
│   │   ├── onboarding/                  # Welcome screens
│   │   │   ├── SimpleOnboarding.kt
│   │   │   ├── BluetoothCheckScreen.kt
│   │   │   └── LocationCheckScreen.kt
│   │   └── net/                         # Tor integration
│   │       └── TorManager.kt
│   └── build.gradle.kts                 # Build configuration
├── ACTIVATION_SYSTEM_PLAN.md            # Monetization plan
├── ZII_CHAT_COMPLETE_OVERVIEW.md        # This file
├── FEATURE_ADDITIONS_DESIGN.md          # Feature specs
└── APP_STARTUP_FLOW.md                  # Startup sequence
```

## Contact & Resources

### Development
- **Repository**: [Your GitHub/GitLab URL]
- **Issue Tracker**: [Your issue tracker URL]
- **Documentation**: [Your docs URL]

### Support
- **Email**: support@ziichat.com
- **WhatsApp**: [Your number]
- **Website**: ziichat.com

### Community
- **Discord**: [Your Discord invite]
- **Telegram**: [Your Telegram group]
- **Twitter**: @ziichat

---

**Document Version:** 1.0  
**Last Updated:** 2024-11-28  
**Status:** Living Document (Updated as project evolves)
