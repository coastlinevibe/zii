# Feature Additions Design - Weather, Sports, News, Spaces

**Version:** 1.7.5+  
**Date:** November 28, 2024

## Current Layout Analysis

### Main Screen Structure
- **Header:** App title (@username), Location button, People counter
- **Content:** Message timeline (mesh/geohash/private chats)
- **Sidebar:** Channels list, People list (mesh or geohash)
- **Bottom:** Message input field

### Available Space for New Features

## Recommended Placement Options

### Option 1: Bottom Navigation Bar (Recommended)
**Best for:** Quick access to main features while keeping current layout

```
┌─────────────────────────────┐
│  Header (zii @username)     │
├─────────────────────────────┤
│                             │
│   Message Timeline          │
│                             │
│                             │
├─────────────────────────────┤
│  Message Input              │
├─────────────────────────────┤
│ [Chat] [Spaces] [News] [+]  │ ← New Bottom Nav
└─────────────────────────────┘
```

**Icons:**
- 💬 Chat (current messages - default)
- 🌐 Spaces (chat rooms/channels)
- 📰 News (news, weather, sports combined)
- ➕ More (additional features)

**Pros:**
- Doesn't clutter existing UI
- Easy thumb access on mobile
- Standard mobile pattern
- Can expand with more features

**Cons:**
- Takes vertical space
- Requires rethinking message input placement

---

### Option 2: Expandable Header Tabs
**Best for:** Keeping everything in one view

```
┌─────────────────────────────┐
│  zii @username  [≡]         │
│  [Chat] [Spaces] [News]     │ ← New Tab Bar
├─────────────────────────────┤
│                             │
│   Content Area              │
│   (changes based on tab)    │
│                             │
└─────────────────────────────┘
```

**Pros:**
- Clean, organized
- No extra navigation needed
- Familiar tab pattern

**Cons:**
- Adds height to header
- Less space for messages

---

### Option 3: Sidebar Enhancement (Minimal Change)
**Best for:** Keeping current design intact

Add new sections to existing sidebar:

```
Sidebar:
├─ Channels
├─ People
├─ ─────────────
├─ 🌐 Spaces        ← New
├─ 📰 News Feed     ← New
├─ 🌤️ Weather       ← New
└─ ⚽ Sports        ← New
```

**Pros:**
- Minimal UI changes
- Uses existing navigation pattern
- Keeps main screen clean

**Cons:**
- Sidebar gets longer
- Requires opening sidebar to access

---

### Option 4: Swipeable Screens
**Best for:** Full-screen experiences

```
← [News] | [Chat] | [Spaces] →
```

Swipe left/right to switch between main sections

**Pros:**
- Modern gesture-based
- Full screen for each feature
- Clean separation

**Cons:**
- Might confuse users
- Harder to see notifications from other sections

---

## Feature Naming Recommendations

### 1. Chat Rooms/Channels/Spaces
**Recommended Name:** **"Spaces"** 🌐

**Why:**
- Modern, inclusive term
- Used by Discord, Twitter/X
- Implies both public and private areas
- Short and memorable

**Alternative Names:**
- "Rooms" (traditional, clear)
- "Channels" (already used for geohash)
- "Communities" (too long)

### 2. News/Weather/Sports Section
**Recommended Name:** **"Feed"** 📰 or **"Updates"** 📡

**Why:**
- Covers all content types
- Familiar social media term
- Implies fresh, current info

**Sub-sections within Feed:**
- 🌤️ Weather
- ⚽ Sports
- 📰 News
- 📊 Crypto (optional)

---

## Detailed Design Recommendation

### **RECOMMENDED: Option 1 + Option 3 Hybrid**

**Main Screen:** Add bottom navigation for primary features
**Sidebar:** Keep for secondary/settings features

```
┌─────────────────────────────┐
│  zii @username  [≡]         │ ← Header (unchanged)
├─────────────────────────────┤
│                             │
│   Active Content            │
│   (Chat/Spaces/Feed)        │
│                             │
├─────────────────────────────┤
│  Message Input (if Chat)    │
├─────────────────────────────┤
│ [💬] [🌐] [📰] [⚙️]         │ ← Bottom Nav
└─────────────────────────────┘

Sidebar (when opened):
├─ Channels (geohash)
├─ People (mesh/geohash)
├─ ─────────────
├─ ⚙️ Settings
└─ ℹ️ About
```

**Bottom Navigation Items:**
1. **💬 Chat** - Current messaging (default)
2. **🌐 Spaces** - Public/private chat rooms
3. **📰 Feed** - News, weather, sports
4. **⚙️ Settings** - App settings, about

---

## Implementation Priority

### Phase 1: Foundation (v1.8.0)
1. Add bottom navigation bar
2. Refactor ChatScreen to support multiple views
3. Create navigation state management

### Phase 2: Spaces (v1.8.1)
1. Create Spaces screen
2. Add room creation/joining
3. Implement room list

### Phase 3: Feed (v1.8.2)
1. Create Feed screen with tabs
2. Add weather widget
3. Add news feed
4. Add sports scores

### Phase 4: Polish (v1.8.3)
1. Add animations
2. Optimize performance
3. Add customization options

---

## Technical Considerations

### Navigation State
```kotlin
enum class MainScreen {
    CHAT,      // Current messaging
    SPACES,    // Chat rooms
    FEED,      // News/weather/sports
    SETTINGS   // App settings
}
```

### Data Sources
- **Weather:** OpenWeatherMap API (free tier)
- **News:** RSS feeds or NewsAPI
- **Sports:** TheSportsDB API (free)
- **Spaces:** Nostr communities or custom implementation

### Offline Support
- Cache last fetched data
- Show "offline" indicator
- Sync when connection available

---

## UI Mockup (Bottom Nav)

```
Current Chat View:
┌─────────────────────────────┐
│ zii @alice  📍 👥3          │
├─────────────────────────────┤
│ @bob: Hey there!            │
│ @alice: Hi!                 │
│                             │
├─────────────────────────────┤
│ Type a message...           │
├─────────────────────────────┤
│ [💬] [🌐] [📰] [⚙️]         │
└─────────────────────────────┘

Spaces View:
┌─────────────────────────────┐
│ Spaces                      │
├─────────────────────────────┤
│ 🌍 Global Chat       👥 234 │
│ 🎮 Gaming            👥 45  │
│ 💻 Tech Talk         👥 89  │
│ ➕ Create Space             │
├─────────────────────────────┤
│ [💬] [🌐] [📰] [⚙️]         │
└─────────────────────────────┘

Feed View:
┌─────────────────────────────┐
│ Feed  [Weather][News][Sports]│
├─────────────────────────────┤
│ 🌤️ 72°F Sunny               │
│ ─────────────────────────   │
│ 📰 Breaking: ...            │
│ 📰 Tech: New AI...          │
│ ⚽ Lakers 98 - 95 Warriors  │
├─────────────────────────────┤
│ [💬] [🌐] [📰] [⚙️]         │
└─────────────────────────────┘
```

---

## Next Steps

1. **Review this design** - Confirm approach
2. **Choose navigation pattern** - Bottom nav vs tabs vs sidebar
3. **Finalize feature names** - Spaces? Rooms? Feed? Updates?
4. **Start implementation** - Begin with navigation framework

**Question for you:**
Which option do you prefer? Or would you like a different approach?
