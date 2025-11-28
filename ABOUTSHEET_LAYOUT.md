# AboutSheet Layout Flow (v1.7.9)

```
┌─────────────────────────────────────────┐
│  [Close]                                │ ← Top Bar
├─────────────────────────────────────────┤
│                                         │
│  Zii Chat  v1.7.9                       │ ← Header
│                                         │
├─────────────────────────────────────────┤
│  FEATURES                               │
│                                         │
│  🔵 Offline Mesh Chat                   │ ← Feature 1
│                                         │
│  🌐 Online Geohash Channels             │ ← Feature 2
│                                         │
│  🔒 End-to-End Encryption               │ ← Feature 3
│                                         │
│  📰 Feed                                 │ ← Feature 4 (NEW)
│     Coming soon                         │
│                                         │
│  💬 Chatrooms                            │ ← Feature 5 (NEW)
│     Coming soon                         │
│                                         │
├─────────────────────────────────────────┤
│  LIMITS                                 │
│  Image/File Size: 1MB max               │
│  Voice Notes: 10 seconds max            │
│                                         │
├─────────────────────────────────────────┤
│  APPEARANCE                             │
│  [light] [dark]                         │
│                                         │
├─────────────────────────────────────────┤
│  [Advanced Settings ▼]                  │ ← Collapsible
│                                         │
│  (When expanded:)                       │
│  ┌───────────────────────────────────┐ │
│  │ Anti-Spam                         │ │
│  │ [pow off] [pow on]                │ │
│  │ (difficulty slider if enabled)    │ │
│  │                                   │ │
│  │ Privacy Chat                      │ │
│  │ [tor off] [tor on]                │ │
│  │ (status info if enabled)          │ │
│  └───────────────────────────────────┘ │
│                                         │
├─────────────────────────────────────────┤
│  Footer text                            │
│                                         │
└─────────────────────────────────────────┘
```

## Icon Legend:
- 🔵 Bluetooth icon (blue)
- 🌐 Public/Globe icon (blue)
- 🔒 Lock icon (blue)
- 📰 Newspaper icon (blue) - NEW
- 💬 Forum/Chat icon (blue) - NEW

## Color Scheme:
- All feature icons: `MaterialTheme.colorScheme.primary` (blue)
- "Coming soon" text: `MaterialTheme.colorScheme.primary` (blue)
- Feature titles: `onBackground` (white/black depending on theme)
- Section headers: `onBackground.copy(alpha = 0.7f)` (dimmed)

## Changes in v1.7.9:
1. Feed & Chatrooms now styled as features (same as other 3)
2. Both have icons and "Coming soon" status
3. Advanced Settings collapsed by default
4. Emergency/Debug sections removed
