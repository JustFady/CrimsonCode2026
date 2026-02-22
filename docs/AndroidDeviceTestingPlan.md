# Android Device Testing Plan - CrimsonCode2026 Emergency Response App

## Overview

This document provides a comprehensive testing plan for the CrimsonCode2026 Emergency Response App across multiple Android devices, API levels, OEMs, and screen sizes.

---

## Target Devices and API Levels

### Primary Test Matrix

| API Level | Android Version | Min SDK Status | Priority Devices |
|------------|-----------------|----------------|------------------|
| API 34 | Android 14 | Current | Pixel 8 Pro, Pixel 8, Galaxy S24 |
| API 33 | Android 13 | Current | Pixel 7 Pro, Pixel 7, Galaxy S23 |
| API 32 | Android 12L | Supported | Pixel 6 Pro, Foldable devices |
| API 31 | Android 12 | Supported | Pixel 6, Galaxy S22 |
| API 30 | Android 11 | Supported | Pixel 5, Galaxy S21, OnePlus 9 |
| API 29 | Android 10 | Supported | Pixel 4, Galaxy S20, OnePlus 8 |
| API 28 | Android 9 | Minimum Required | Pixel 3, Galaxy S10, Essential Phone |

### Device Categories

**Google Pixel:**
- Pixel 8 Pro, Pixel 8, Pixel 7 Pro, Pixel 7, Pixel 6 Pro, Pixel 6, Pixel 5, Pixel 4, Pixel 3
- Pure Android experience - baseline for compatibility

**Samsung Galaxy:**
- S24 Ultra, S24+, S24
- S23 Ultra, S23+, S23
- S22 Ultra, S22+, S22
- S21 Ultra, S21+, S21
- S20 FE, S20 Ultra, S20+
- One UI customizations - test for Samsung-specific behaviors

**Other OEMs:**
- OnePlus: 12, 11, 10, 9, 8, 8T, 7T, 7
- Xiaomi: 13, 13 Pro, Redmi Note 12, Redmi Note 11
- Motorola: Edge 40, Edge 30, Edge Plus, G Power
- HMD/Nokia: G42 5G, G22, X6 5G
- Sony: Xperia 1 V, Xperia 10 V

### Screen Sizes

| Category | Resolution | Density | Physical Size |
|----------|-------------|----------|---------------|
| Small | 320x426 | mdpi | 4.0" - 4.7" |
| Normal | 360x640 | mdpi | 4.7" - 5.5" |
| Large | 480x800 | mdpi | 5.5" - 6.5" |
| XLarge | 720x1280 | mdpi | 6.5"+ |
| Foldable | 1768x2208 | xhdpi | 7.6" unfolded |

---

## Testing Scenarios

### 1. Installation and First Launch

#### Pre-installation Checklist
- [ ] Device meets API 28+ requirement
- [ ] Sufficient storage space (100MB+)
- [ ] Network connectivity available
- [ ] Location services enabled

#### First Launch Flow
- [ ] App splash screen displays correctly
- [ ] App requests necessary permissions on first use:
  - [ ] Location (foreground) - prompted on first location request
  - [ ] Contacts - prompted on contact selection
  - [ ] Push notifications - prompted during setup
- [ ] Phone entry screen displays with correct E.164 formatting
- [ ] Keyboard type is numeric for phone entry
- [ ] OTP verification screen displays correctly
- [ ] Display name entry validates correctly (2-100 chars)
- [ ] Map initializes with user location
- [ ] FCM token registration completes

#### Permission Handling
- [ ] Permissions are requested at appropriate times
- [ ] App handles permission denial gracefully
- [ ] App can continue with limited permissions where possible
- [ ] Settings deep link works for permission changes
- [ ] Permission rationale displays correctly on API 23+

### 2. Authentication Flow

#### Phone Entry
- [ ] Phone number formats correctly with USA format (+1 (XXX) XXX-XXXX)
- [ ] Invalid phone numbers are rejected
- [ ] Send OTP button enables only with valid phone
- [ ] OTP countdown timer works (30 seconds)
- [ ] Resend OTP button enables after countdown
- [ ] Phone number persists during OTP flow

#### OTP Verification
- [ ] OTP input accepts exactly 6 digits
- [ ] Auto-focus on OTP field
- [ ] OTP submission works
- [ ] Invalid OTP shows error message
- [ ] Resend functionality works
- [ ] Countdown timer displays correctly

#### Display Name Entry
- [ ] Display name validates (2-100 characters)
- [ ] Character count displays
- [ ] Leading/trailing spaces are trimmed
- [ ] Error messages display for invalid input

#### Session Persistence
- [ ] Refresh token stored securely
- [ ] Session persists across app restart
- [ ] Session lasts 30 days
- [ ] Re-authentication required after token expiration
- [ ] Logout clears tokens and navigates to login

### 3. Contact Management

#### Contact Permission
- [ ] Permission requested on first contact access
- [ ] Granting permission loads device contacts
- [ ] Denying permission shows manual entry option

#### Contact List
- [ ] All device contacts load correctly
- [ ] Contact search/filter works
- [ ] Phone numbers normalized to E.164
- [ ] App user badges display correctly
- [ ] Multi-selection works
- [ ] Scroll performance is smooth

#### Manual Entry
- [ ] Manual entry form displays
- [ ] Phone validation works
- [ ] Display name validation works
- [ ] Manual contacts save correctly

### 4. Maps and Location

#### Map Display
- [ ] Map tiles load correctly
- [ ] User location marker displays
- [ ] Accuracy circle displays with correct size
- [ ] Accuracy color coding works (green/yellow/orange/red)
- [ ] Map zoom controls work
- [ ] Pan gesture works
- [ ] Rotate gesture works
- [ ] Tilt gesture works

#### Location Services
- [ ] High-precision mode: GPS primary, accuracy <10m
- [ ] Balanced mode: GPS+WiFi+Cellular, accuracy <50m
- [ ] Low-power mode: accuracy <1km
- [ ] Location updates at correct intervals (5s/30s/2-5min)
- [ ] Location permission dialog appears
- [ ] Location denied shows manual entry option
- [ ] Indoor positioning fallback works

#### Location Fallback Chain
- [ ] GPS with WAAS/EGNOS augmentation
- [ ] Wi-Fi positioning
- [ ] Cell tower triangulation
- [ ] IP-based geolocation
- [ ] Manual location entry on map

### 5. Event Creation

#### Category Selection
- [ ] All 8 categories display with correct icons
- [ ] Category selection highlights
- [ ] Category description displays on selection
- [ ] Categories: Medical, Fire, Weather, Crime, Natural Disaster, Infrastructure, Search & Rescue, Traffic, Other

#### Severity Selection
- [ ] Alert (Warning) button works
- [ ] Crisis button works
- [ ] Visual distinction: Orange vs Red
- [ ] Default to Warning (Alert)

#### Broadcast Scope
- [ ] Public toggle selected by default
- [ ] Private toggle works
- [ ] Public shows "All nearby users within 50 miles"
- [ ] Private shows "Your selected emergency contacts"

#### Location Confirmation
- [ ] Map preview displays
- [ ] User location marker with accuracy
- [ ] "Edit location" allows manual pin
- [ ] "Use my location" resets to GPS

#### Description Input
- [ ] Text field accepts input
- [ ] Character count displays (max 500)
- [ ] Max length enforced

#### Event Submission
- [ ] Review summary displays correctly
- [ ] "Create Alert" button works
- [ ] Event saves to database
- [ ] Public events are anonymous
- [ ] Private events include creator info
- [ ] 48-hour expiration set
- [ ] Map displays new event marker

### 6. Event Display and Interaction

#### Map Markers
- [ ] Public events display within bounds
- [ ] Private events display only if recipient
- [ ] Crisis markers: Red with pulsing animation
- [ ] Alert markers: Orange static
- [ ] Category icons display
- [ ] Marker clustering works
- [ ] Markers update on map move

#### Event Details Panel
- [ ] Bottom sheet slides up on marker tap
- [ ] Severity, category, description display
- [ ] Location displays
- [ ] Time displays
- [ ] Public events: anonymous
- [ ] Private events: creator's display name
- [ ] "Clear from list" works (local hide)

#### Event List
- [ ] Top-right button shows event count
- [ ] List displays all private and nearby public events
- [ ] Tapping event zooms to location
- [ ] "Clear all" works
- [ ] Scroll performance smooth

### 7. Push Notifications

#### Notification Setup
- [ ] FCM token registered
- [ ] Notification channels created (API 26+)
- [ ] Emergency channel: High importance
- [ ] Alerts channel: Default importance

#### Crisis Alert
- [ ] Vibration: Aggressive pattern
- [ ] Priority: High
- [ ] Action: "View on Map"
- [ ] Title: "CRISIS - [Category]"
- [ ] Body: Description

#### Alert (Warning)
- [ ] Vibration: Standard pattern
- [ ] Priority: Normal
- [ ] Action: "View on Map"
- [ ] Title: "ALERT - [Category]"
- [ ] Body: Description

#### Notification Tapping
- [ ] Deep link works: `crimsoncode://event/{eventId}`
- [ ] Opens app to event on map
- [ ] App navigates correctly from background
- [ ] App navigates correctly from foreground

### 8. Settings and Preferences

#### Notification Settings
- [ ] Master toggle: Enable/disable all
- [ ] Crisis alerts toggle
- [ ] Warning alerts toggle
- [ ] Public alerts toggle (opt-out)
- [ ] Private alerts toggle
- [ ] Vibration toggle

#### Location Settings
- [ ] High precision mode toggle
- [ ] Default: Balanced mode

#### Contact Settings
- [ ] Emergency contact list displays
- [ ] Add/remove contacts works
- [ ] Import from device works
- [ ] Manual entry works

#### Account Settings
- [ ] Display name editable
- [ ] Display name saves to database
- [ ] Private events update with new name
- [ ] Logout button works
- [ ] Re-authentication required on next open

### 9. Network Resilience

#### Network Status
- [ ] Network status indicator displays
- [ ] ONLINE/OFFLINE/UNKNOWN states work
- [ ] Connection type displays (WIFI/CELLULAR/ETHERNET/VPN)

#### Network Changes
- [ ] ONLINE -> OFFLINE handled gracefully
- [ ] OFFLINE -> ONLINE recovery works
- [ ] WIFI <-> CELLULAR transitions work
- [ ] Intermittent connection handled

#### Offline Behavior
- [ ] Queue operations for later
- [ ] Show offline UI state
- [ ] Retry on reconnection

#### Push During Network Changes
- [ ] Notifications deliver after reconnection
- [ ] Failed delivery retried
- [ ] Delivery status updated

### 10. Performance

#### Map Rendering
- [ ] Markers render within visible bounds only
- [ ] Clustering works efficiently
- [ ] Map tiles load smoothly
- [ ] No lag on pan/zoom

#### Event List
- [ ] Virtual scrolling works
- [ ] Smooth scrolling with 100+ events
- [ ] Search/filter instant

#### Event Creation
- [ ] Form submission <2 seconds
- [ ] Location confirmation instant
- [ ] No lag on typing

#### Memory Usage
- [ ] No memory leaks on map navigation
- [ ] No memory leaks on event list
- [ ] No memory leaks on location updates

### 11. Security

#### Token Storage
- [ ] Refresh token encrypted in KSafe
- [ ] Access token not persisted
- [ ] Tokens cleared on logout

#### Data Transmission
- [ ] HTTPS only
- [ ] TLS 1.2+

#### Data Privacy
- [ ] Public events anonymous
- [ ] Private events show display name only
- [ ] Location data expires after 48 hours

---

## API Level Specific Tests

### API 34 (Android 14)
- [ ] Foreground service types compatible
- [ ] Photo picker integration (if used)
- [ ] Partial media permissions
- [ ] Predictive back gesture works
- [ ] Per-app language settings respected

### API 33 (Android 13)
- [ ] Notification runtime permission works
- [ ] Themed icon support (if implemented)
- [ ] Per-app language settings
- [ ] Exact alarm permission handling

### API 32 (Android 12L)
- [ ] Foldable layouts work
- [ ] Multi-window mode works
- [ ] Taskbar interaction (if relevant)

### API 31 (Android 12)
- [ ] Splash screen API works
- [ ] Approximate location permission
- [ ] Exact alarm permission
- [ ] Haptic feedback
- [ ] Material You theming (if implemented)

### API 30 (Android 11)
- [ ] Scoped storage compatibility
- [ ] Autofill compatibility
- [ ] Bubbles support (if used)
- [ ] Screen recording detection

### API 29 (Android 10)
- [ ] Scoped storage
- [ ] Dark theming
- [ ] Gesture navigation
- [ ] 5G detection (if relevant)

### API 28 (Android 9) - Minimum
- [ ] All core functionality works
- [ ] Notification channels work
- [ ] Biometric prompt (if used)
- [ ] Adaptive icons
- [ ] Display cutout support

---

## OEM-Specific Tests

### Samsung One UI
- [ ] No Samsung-specific bugs
- [ ] Samsung keyboard compatibility
- [ ] Samsung DeX mode (if applicable)
- [ ] Edge screen support (if applicable)
- [ ] Samsung Internet browser deep links

### OnePlus OxygenOS
- [ ] OnePlus specific battery optimizations handled
- [ ] Dark mode compatibility
- [ ] Gesture navigation support

### Xiaomi MIUI
- [ ] MIUI permission dialogs handled
- [ ] MIUI notification behavior
- [ ] MIUI-specific battery optimizations

### Motorola
- [ ] Moto Actions compatibility
- [ ] Display cutout handling

---

## Screen Size Tests

### Small Screens (<5")
- [ ] UI fits without scroll
- [ ] Buttons large enough for touch
- [ ] Text readable
- [ ] Map controls accessible

### Normal Screens (5"-6.5")
- [ ] Optimal layout
- [ ] Touch targets 48dp minimum

### Large Screens (6.5"+)
- [ ] No excessive white space
- [ ] Content appropriately sized

### Foldable Devices
- [ ] Layout adapts to fold state
- [ ] Continuity across fold/unfold
- [ ] No crashes on fold/unfold

---

## Known Issues and Workarounds

### Common Android Issues
1. **Battery Optimization**: Some OEMs kill background services
   - Workaround: Guide users to disable battery optimization

2. **Location Accuracy**: Some devices have poor GPS
   - Workaround: Prompt to enable high-accuracy mode

3. **Notification Delivery**: Some devices delay notifications
   - Workaround: Guide users to check notification settings

4. **Samsung Deep Linking**: May require additional configuration
   - Workaround: Test thoroughly on Samsung devices

---

## Test Execution Template

### Device Test Record

| Field | Value |
|-------|-------|
| Device Model | |
| Android Version | |
| API Level | |
| Screen Size | |
| OEM | |
| Build Number | |
| Test Date | |
| Tester | |

### Test Results Summary

| Category | Passed | Failed | Blocked | Notes |
|----------|--------|--------|---------|-------|
| Installation & First Launch | | | | |
| Authentication | | | | |
| Contact Management | | | | |
| Maps & Location | | | | |
| Event Creation | | | | |
| Event Display | | | | |
| Push Notifications | | | | |
| Settings | | | | |
| Network Resilience | | | | |
| Performance | | | | |
| **TOTAL** | | | | |

---

## Automation Recommendations

### Espresso Instrumentation Tests
1. Auth flow: Phone entry -> OTP -> Display name
2. Contact list: Load, search, select
3. Map navigation: Pan, zoom, marker tap
4. Event creation: Full flow
5. Settings navigation

### Firebase Test Lab
1. Run on multiple API levels (28-34)
2. Test on different screen sizes
3. Automated screenshot capture
4. Video recording of test runs

---

## Success Criteria

- [ ] All critical tests pass on Pixel 8 Pro (API 34)
- [ ] All critical tests pass on Galaxy S24 (API 34)
- [ ] All critical tests pass on OnePlus 11 (API 33)
- [ ] No crashes on minimum API (API 28)
- [ ] UI renders correctly on all screen sizes
- [ ] Performance meets targets (<2s event creation)
- [ ] Push notifications deliver on all devices
- [ ] No OEM-specific blockers identified
