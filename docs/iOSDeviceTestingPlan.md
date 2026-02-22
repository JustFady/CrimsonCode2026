# iOS Device Testing Plan - CrimsonCode2026 Emergency Response App

## Overview

This document provides a comprehensive testing plan for the CrimsonCode2026 Emergency Response App across multiple iOS versions, iPhone models, and screen sizes.

---

## Target Devices and iOS Versions

### Primary Test Matrix

| iOS Version | Min SDK Status | Priority Devices |
|-------------|----------------|------------------|
| iOS 18 | Current | iPhone 16 Pro, iPhone 16, iPhone 15 Pro |
| iOS 17 | Supported | iPhone 15, iPhone 15 Plus, iPhone 14 Pro |
| iOS 16 | Supported | iPhone 14, iPhone 13 Pro, iPhone 13 |
| iOS 15 | Minimum Required | iPhone 12, iPhone 11, iPhone SE 2nd Gen |
| iOS 14 | Supported (older) | iPhone XR, iPhone 11, iPhone SE 1st Gen |

### Device Categories

**Current Generation (iPhone 16 Series):**
- iPhone 16 Pro Max
- iPhone 16 Pro
- iPhone 16 Plus
- iPhone 16

**Previous Generation (iPhone 15 Series):**
- iPhone 15 Pro Max
- iPhone 15 Pro
- iPhone 15 Plus
- iPhone 15

**iPhone 14 Series:**
- iPhone 14 Pro Max
- iPhone 14 Pro
- iPhone 14 Plus
- iPhone 14

**iPhone 13 Series:**
- iPhone 13 Pro Max
- iPhone 13 Pro
- iPhone 13
- iPhone 13 mini

**iPhone 12 Series:**
- iPhone 12 Pro Max
- iPhone 12 Pro
- iPhone 12
- iPhone 12 mini

**iPhone SE Series:**
- iPhone SE 3rd Gen (2022)
- iPhone SE 2nd Gen (2020)

### Screen Sizes

| Device | Resolution | Scale Factor | Physical Size |
|--------|------------|--------------|---------------|
| iPhone 16 Pro Max | 1320x2868 | @3x | 6.9" |
| iPhone 16 Pro | 1206x2622 | @3x | 6.3" |
| iPhone 16/15/14 | 1179x2556 | @3x | 6.1" |
| iPhone 16 Plus/15 Plus/14 Plus | 1290x2796 | @3x | 6.7" |
| iPhone 13 mini/12 mini | 1080x2340 | @3x | 5.4" |
| iPhone SE (all) | 1170x2532 | @3x | 4.7" |

---

## Testing Scenarios

### 1. Installation and First Launch

#### Pre-installation Checklist
- [ ] Device meets iOS 15+ requirement
- [ ] Sufficient storage space (100MB+)
- [ ] Network connectivity available
- [ ] Location services enabled

#### First Launch Flow
- [ ] App splash screen displays correctly
- [ ] App requests necessary permissions on first use:
  - [ ] Location (Always/When In Use) - prompted on first location request
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
- [ ] Permission rationale displays correctly

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
- [ ] Refresh token stored securely in Keychain
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
- [ ] Map tiles load correctly via MapLibre
- [ ] User location marker displays
- [ ] Accuracy circle displays with correct size
- [ ] Accuracy color coding works (green/yellow/orange/red)
- [ ] Map zoom controls work
- [ ] Pan gesture works
- [ ] Rotate gesture works (if supported)
- [ ] Tilt gesture works (if supported)

#### Location Services (CoreLocation)
- [ ] High-precision mode: GPS primary, accuracy <10m
- [ ] Balanced mode: GPS+WiFi+Cellular, accuracy <50m
- [ ] Low-power mode: accuracy <1km
- [ ] Location updates at correct intervals (5s/30s/2-5min)
- [ ] Location permission dialog appears
- [ ] Location denied shows manual entry option
- [ ] Indoor positioning fallback works

#### Location Fallback Chain
- [ ] GPS with WAAS augmentation
- [ ] Wi-Fi positioning (CLLocationManager)
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

#### Notification Setup (KMPNotifier + Firebase FCM)
- [ ] FCM token registered
- [ ] UNUserNotificationCenter configured
- [ ] Notification categories created
- [ ] Request authorization works

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
- [ ] Connection type displays (WIFI/CELLULAR)

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
- [ ] Refresh token encrypted in Keychain
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

## iOS Version Specific Tests

### iOS 18 (Current)
- [ ] Dynamic Island interactions work (if implemented)
- [ ] Contact sheet integration
- [ ] Live Activities support (if implemented)
- [ ] Control Center widgets (if implemented)
- [ ] Lock screen widgets (if implemented)
- [ ] StandBy mode compatibility (if relevant)
- [ ] Spatial Audio/Head Tracking (if relevant)

### iOS 17
- [ ] Interactive widgets work (if implemented)
- [ ] NameDrop compatibility (if contacts sharing is added)
- [ ] Safari Web Push (if relevant)
- [ ] AirDrop Receiving improvements (if relevant)

### iOS 16
- [ ] Lock screen widgets (if implemented)
- [ ] Focus filters (if implemented)
- [ ] iCloud Shared Photo Library (if relevant)
- [ ] Passkeys support (if implemented)

### iOS 15
- [ ] All core functionality works
- [ ] Focus mode compatibility
- [ ] Live Text in camera (if used)
- [ ] iCloud+ Private Relay (if relevant)

### iOS 14 (Older Support)
- [ ] All core functionality works
- [ ] Compact UI compatibility
- [ ] App Clips (if relevant)
- [ ] Picture in Picture (if relevant)

---

## Device-Specific Tests

### iPhone 16 Pro Max / 16 Pro (Large Screens)
- [ ] UI scales correctly to 6.9" / 6.3"
- [ ] No UI overflow
- [ ] Touch targets accessible
- [ ] Maps render correctly

### iPhone mini (Small Screens)
- [ ] UI fits without excessive scrolling
- [ ] Buttons large enough for touch
- [ ] Text readable
- [ ] Map controls accessible

### iPhone SE (Legacy Screen Size)
- [ ] Legacy 4.7" compatibility
- [ ] No horizontal overflow
- [ ] All features accessible
- [ ] Performance acceptable

---

## Screen Orientation Tests

### Portrait Mode
- [ ] All screens render correctly
- [ ] No layout issues
- [ ] Touch targets accessible

### Landscape Mode (if supported)
- [ ] All screens render correctly
- [ ] Map rotation works
- [ ] No layout issues

---

## Accessibility Tests

### VoiceOver
- [ ] All elements properly labeled
- [ ] Navigation is logical
- [ ] Important actions announced

### Dynamic Type
- [ ] UI scales correctly at all text sizes
- [ ] No text truncation
- [ ] Layout remains usable

### Reduced Motion
- [ ] Animations respect setting
- [ ] App remains functional

### Contrast
- [ ] Text meets contrast ratios
- [ ] Interactive elements distinguishable

---

## Known Issues and Workarounds

### Common iOS Issues
1. **Background Location**: iOS has strict background restrictions
   - Workaround: Use significant location changes or educate users about foreground requirement

2. **Notification Delivery**: iOS may batch notifications
   - Workaround: Use critical alerts for crisis (requires special entitlement)

3. **Contact Access**: iOS requires explicit permission
   - Workaround: Guide users to Settings app if denied

4. **Keychain Access**: Some apps have issues with keychain sharing
   - Workaround: Use proper keychain access groups

---

## Test Execution Template

### Device Test Record

| Field | Value |
|-------|-------|
| Device Model | |
| iOS Version | |
| Screen Size | |
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

### XCTest UI Tests
1. Auth flow: Phone entry -> OTP -> Display name
2. Contact list: Load, search, select
3. Map navigation: Pan, zoom, marker tap
4. Event creation: Full flow
5. Settings navigation

### Xcode Cloud / CI
1. Run on multiple iOS simulators (15-18)
2. Test on different device simulators
3. Automated screenshot capture
4. Video recording of test runs

---

## Success Criteria

- [ ] All critical tests pass on iPhone 16 Pro (iOS 18)
- [ ] All critical tests pass on iPhone 15 (iOS 17)
- [ ] All critical tests pass on iPhone 14 (iOS 16)
- [ ] No crashes on minimum iOS (iOS 15)
- [ ] UI renders correctly on all screen sizes
- [ ] Performance meets targets (<2s event creation)
- [ ] Push notifications deliver on all devices
- [ ] Keychain storage works correctly
- [ ] CoreLocation permissions work correctly
