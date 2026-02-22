# Network Condition Testing Plan - CrimsonCode2026 Emergency Response App

## Overview

This document provides a comprehensive testing plan for the CrimsonCode2026 Emergency Response App under various network conditions.

**Key Areas:**
1. Intermittent/Slow Network Conditions
2. Connection Recovery
3. Push Notification Delivery During Network Changes

---

## 1. Network Status Monitoring

### Network Status Display

**Expected Behavior:**
- Network status indicator displays: ONLINE/OFFLINE/UNKNOWN
- Connection type displays: WIFI/CELLULAR/ETHERNET/VPN
- Status updates in real-time

**Test Scenarios:**

| Scenario | Expected Status | Test Method |
|----------|----------------|-------------|
| WiFi connected | ONLINE + WIFI | Connect to WiFi network |
| Cellular data | ONLINE + CELLULAR | Disable WiFi, use cellular |
| Ethernet (tablet) | ONLINE + ETHERNET | Connect via Ethernet adapter |
| VPN enabled | ONLINE + VPN | Enable VPN on device |
| Airplane mode | OFFLINE | Enable airplane mode |
| No networks available | OFFLINE | Go to area with no coverage |

**Verification Steps:**
1. Observe network status indicator
2. Change network type
3. Verify status updates immediately
4. Confirm connection type is correct

**Success Criteria:**
- Status indicator reflects actual network state
- Connection type correctly identified
- Updates happen in < 1 second

---

## 2. Slow Network Conditions

### Testing Approaches

**Option A: Network Throttling Tools**
- Use Chrome DevTools (for web testing)
- Use Charles Proxy for mobile throttling
- Use Network Link Conditioner (macOS)

**Option B: Actual Slow Networks**
- Edge/2G cellular (if available)
- Crowded public WiFi
- International roaming

### Test Scenarios

#### 2.1 Event Creation on Slow Network

| Network Speed | Expected Behavior |
|---------------|------------------|
| Very Slow (< 50 Kbps) | Loading indicator, completes in < 30s |
| Slow (50-100 Kbps) | Loading indicator, completes in < 15s |
| Normal (1-10 Mbps) | Completes in < 2s |

**Test Steps:**
1. Enable network throttling to target speed
2. Create an event
3. Verify loading indicator shows
4. Measure time to completion
5. Confirm event is created successfully

**Success Criteria:**
- Loading indicator displays before completion
- No app crashes or freezes
- Completion time is acceptable for network speed
- User can cancel if taking too long

#### 2.2 Map Loading on Slow Network

| Network Speed | Expected Behavior |
|---------------|------------------|
| Very Slow | Tiles load progressively, map usable |
| Slow | Tiles load within 5-10 seconds |
| Normal | Tiles load within 1-2 seconds |

**Test Steps:**
1. Enable network throttling
2. Open map view
3. Pan/zoom to different areas
4. Verify tiles load
5. Check for frozen UI

**Success Criteria:**
- Tiles load progressively
- No UI freezes
- Map remains interactive
- Failed tiles handled gracefully

#### 2.3 Contact Import on Slow Network

| Network Speed | Expected Behavior |
|---------------|------------------|
| Very Slow | Progress bar, completes < 30s |
| Slow | Progress bar, completes < 15s |
| Normal | Completes < 2s |

**Test Steps:**
1. Enable network throttling
2. Grant contacts permission
3. Import contacts
4. Verify progress indicator
5. Confirm all contacts loaded

**Success Criteria:**
- Progress indicator shows
- No timeout errors
- All contacts eventually load
- User can cancel if desired

#### 2.4 Authentication on Slow Network

| Network Speed | Expected Behavior |
|---------------|------------------|
| Very Slow | OTP countdown works, resend available |
| Slow | OTP arrives, verify works |
| Normal | Quick OTP verification |

**Test Steps:**
1. Enable network throttling
2. Enter phone number
3. Request OTP
4. Verify countdown timer
5. Enter OTP code
6. Confirm login works

**Success Criteria:**
- Countdown timer starts and works
- Resend button enables after countdown
- OTP verification works
- No timeout errors

---

## 3. Intermittent Network Conditions

### Testing Approaches

**Option A: Network Simulation Tools**
- Use Charles Proxy to simulate packet loss
- Use Network Link Conditioner
- Use ADB commands (Android)

**Option B: Actual Intermittent Networks**
- Move between WiFi zones
- Areas with poor cellular coverage
- Toggling airplane mode

### Test Scenarios

#### 3.1 Intermittent Event Creation

| Pattern | Expected Behavior |
|----------|------------------|
| Online-Offline-Online | Retry on reconnection, success |
| Multiple short drops | Operation continues, may timeout |
| Drop during submission | Queue for retry, show error |

**Test Steps:**
1. Start creating an event
2. Trigger network dropout during submission
3. Observe app behavior
4. Restore network
5. Verify retry behavior

**Success Criteria:**
- App doesn't crash
- Error message displayed
- Retry attempted on reconnection
- Queue shown if needed

#### 3.2 Intermittent Map Usage

| Pattern | Expected Behavior |
|----------|------------------|
| Drop while panning | Tiles queue, resume on reconnection |
| Drop on zoom | Tiles reload when network returns |
| Frequent drops | Map remains interactive |

**Test Steps:**
1. Open map
2. Pan/zoom while triggering network drops
3. Verify app doesn't freeze
4. Confirm tiles reload on reconnection

**Success Criteria:**
- No app crashes
- Map remains interactive
- Tiles load after reconnection
- User informed of network status

#### 3.3 Intermittent Realtime Subscription

| Pattern | Expected Behavior |
|----------|------------------|
| Drop while subscribed | Reconnect automatically |
| Multiple drops | Handle gracefully, reconnect each time |
| Long offline | Resync on return |

**Test Steps:**
1. Connect to realtime channel
2. Trigger network drops
3. Verify reconnection attempts
4. Confirm events received after reconnection

**Success Criteria:**
- Automatic reconnection
- No duplicate events
- Events sync after reconnection
- Connection status visible to user

---

## 4. Network State Transitions

### Transition Scenarios

#### 4.1 WiFi to Cellular

**Test Steps:**
1. Start with WiFi
2. Disable WiFi (cellular takes over)
3. Verify app continues working
4. Confirm status updates to CELLULAR

**Expected Behavior:**
- No app restart needed
- Ongoing operations continue
- Status indicator updates to CELLULAR
- No data loss

#### 4.2 Cellular to WiFi

**Test Steps:**
1. Start with cellular
2. Connect to WiFi
3. Verify app continues working
4. Confirm status updates to WIFI

**Expected Behavior:**
- No app restart needed
- Ongoing operations continue
- Status indicator updates to WIFI
- No data loss

#### 4.3 VPN Enable/Disable

**Test Steps:**
1. Start without VPN
2. Enable VPN
3. Verify status updates to VPN
4. Disable VPN
5. Verify status returns to underlying type

**Expected Behavior:**
- VPN detected
- App continues working
- Status shows VPN when active
- No connection interruption

#### 4.4 Multiple Transitions

**Test Steps:**
1. Cycle through: WiFi -> Cellular -> WiFi -> Offline -> WiFi
2. Verify each transition handled
3. Confirm app remains stable

**Expected Behavior:**
- All transitions handled
- No app crashes
- Status always reflects current state
- Smooth UX throughout

---

## 5. Connection Recovery

### Recovery Scenarios

#### 5.1 Offline to Online Recovery

**Test Steps:**
1. Go offline (airplane mode)
2. Attempt operation (should fail or queue)
3. Go back online
4. Verify queued operations complete

**Expected Behavior:**
- Offline state clearly shown
- Operations queued or gracefully failed
- Automatic retry on reconnection
- User notified of retry status

#### 5.2 Extended Offline Period

| Offline Duration | Expected Behavior |
|-----------------|------------------|
| 1 minute | Operations queued, retry on return |
| 10 minutes | Token remains valid, retry works |
| 1 hour | Token valid if within session, retry works |
| 24+ hours | May require re-authentication |

**Test Steps:**
1. Go offline for varying durations
2. Attempt operations while offline
3. Return online
4. Verify recovery behavior

**Expected Behavior:**
- Session maintained if within token expiration
- Automatic retry of queued operations
- Re-auth prompt only if token expired
- No data loss

#### 5.3 Rapid Recovery Cycles

**Test Steps:**
1. Cycle online/offline rapidly (5 times)
2. Verify app stability
3. Confirm no connection leaks

**Expected Behavior:**
- No connection leaks
- App remains responsive
- Each recovery works correctly
- No memory buildup

---

## 6. Push Notification Delivery During Network Changes

### Test Scenarios

#### 6.1 Notification While Online

**Test Steps:**
1. Ensure device is online
2. Have another device create an event
3. Verify notification arrives
4. Tap notification
5. Confirm deep link works

**Expected Behavior:**
- Notification arrives within 5 seconds
- Tapping opens app to event
- Map navigates to event location

#### 6.2 Notification Sent While Offline

**Test Steps:**
1. Device A is offline
2. Device B creates event targeting Device A
3. Wait 1 minute
4. Device A comes online
5. Verify notification arrives

**Expected Behavior:**
- Notification arrives when Device A comes online
- Notification shows correct event info
- Deep link works correctly

#### 6.3 Notification During Network Transition

**Test Steps:**
1. Device A is on WiFi
2. Event is created
3. Device A switches to cellular during notification delivery
4. Verify notification arrives

**Expected Behavior:**
- Notification delivered despite transition
- No duplicate notifications
- Deep link works correctly

#### 6.4 Multiple Notifications While Offline

**Test Steps:**
1. Device A is offline
2. Device B creates 3 events targeting Device A
3. Device A comes online
4. Verify all notifications arrive

**Expected Behavior:**
- All 3 notifications arrive
- Order preserved (or timestamp sorted)
- No notifications lost
- No duplicates

#### 6.5 Notification Timeout Handling

**Test Steps:**
1. Device A is offline
2. Event created targeting Device A
3. Keep Device A offline for > 24 hours
4. Check notification delivery status

**Expected Behavior:**
- Delivery marked as FAILED
- No error crash in app
- User can view event in app list (if applicable)

---

## 7. Offline Behavior

### Offline UI State

**Expected Behavior:**
- Clear offline indicator/banner
- Network status shows OFFLINE
- Features requiring network show error or are disabled

**Test Steps:**
1. Go offline
2. Verify offline UI appears
3. Try network-dependent features
4. Check error messages

**Success Criteria:**
- Offline state clearly visible
- App doesn't crash
- Appropriate error messages
- Clear indication of what's unavailable

### Offline Operations

| Feature | Offline Behavior |
|----------|----------------|
| View map | Show cached tiles, show offline |
| Create event | Queue for retry or show error |
| View existing events | Load from cache |
| Settings | Work (local-only settings) |
| Profile | Load from cache |

**Test Steps:**
1. Go offline
2. Test each feature
3. Verify appropriate behavior

**Success Criteria:**
- Each feature handles offline gracefully
- No app crashes
- User informed of offline limitations

### Queue and Retry

**Test Steps:**
1. Start online
2. Go offline
3. Create event (should queue)
4. Go back online
5. Verify queued event sent

**Expected Behavior:**
- Operations queued when offline
- Automatic retry when online
- Clear indication of queued items
- User can cancel queued items

---

## 8. Network Error Handling

### Timeout Handling

**Test Scenarios:**

| Operation | Timeout | Expected Behavior |
|------------|-----------|------------------|
| Event creation | 30s | Show timeout error, offer retry |
| OTP request | 30s | Show error, allow resend |
| Map tiles | 10s per tile | Show placeholder, retry on pan |
| Realtime connect | 10s | Show offline, retry in background |

**Test Steps:**
1. Enable network throttling to near-zero speed
2. Attempt operation
3. Wait for timeout
4. Verify error message
5. Test retry option

**Success Criteria:**
- Timeout error displayed
- Retry option available
- No app freeze
- User can cancel

### Server Error Handling

**Test Scenarios:**

| Error Code | Expected Behavior |
|------------|------------------|
| 500 Server Error | Show error message, offer retry |
| 503 Service Unavailable | Show maintenance message |
| 429 Rate Limited | Show rate limit message, suggest retry later |
| 401 Unauthorized | Show error, redirect to login |

**Test Steps:**
1. Use proxy to intercept and modify responses
2. Trigger each error code
3. Verify app behavior

**Success Criteria:**
- Appropriate error message
- No app crash
- Suggested action (retry, login, etc.)
- User can continue

---

## 9. Platform-Specific Network Testing

### Android Network Testing

| Feature | Test Focus |
|---------|-------------|
| NetworkRequest API | Verify network requests work |
| ConnectivityManager | Connection detection |
| NetworkCallback | Real-time status updates |
| Data Saver mode | Respect user setting |
| Metered detection | Correctly identify metered |

**Test Steps:**
1. Test on Android 9-14
2. Verify NetworkRequest API works
3. Check Data Saver mode handling
4. Test metered connection detection

### iOS Network Testing

| Feature | Test Focus |
|---------|-------------|
| NWPathMonitor | Real-time path updates |
| Network framework | Connection detection |
| Reachability | Network availability |
| Background networking | Verify background updates |

**Test Steps:**
1. Test on iOS 15-18
2. Verify NWPathMonitor works
3. Test background network handling
4. Check airplane mode detection

---

## 10. Performance Under Poor Network

### Response Time Metrics

| Operation | Slow Network Target | Acceptable |
|------------|-------------------|------------|
| Event creation | < 30s at 50Kbps | < 60s |
| OTP send | < 20s at 50Kbps | < 40s |
| Map tile | < 10s at 50Kbps | < 15s |
| Contact import | < 30s at 50Kbps | < 60s |

**Test Steps:**
1. Enable specific network throttling
2. Time each operation
3. Record results
4. Compare against targets

### Battery Impact Under Poor Network

| Scenario | Expected Impact |
|----------|----------------|
| Constant retry | Higher than normal |
| Long timeout | Moderate impact |
| Frequent reconnection | Higher impact |

**Test Steps:**
1. Start with 100% battery
2. Simulate poor network for 30 minutes
3. Record battery usage
4. Compare to normal network usage

---

## 11. Edge Cases

### Unusual Network Scenarios

| Scenario | Expected Behavior |
|----------|------------------|
| Roaming | App works, may warn about data |
| Network behind proxy | App works, detects correctly |
| Captive portal | Handle authentication flow |
| Dual SIM switching | Maintain connection |
| WiFi calling only | Show offline for data |

---

## Test Execution Template

### Network Condition Test Record

| Field | Value |
|-------|-------|
| Device Model | |
| Platform (Android/iOS) | |
| Network Type (WIFI/CELLULAR) | |
| Network Speed (Mbps/Kbps) | |
| Test Date/Time | |
| Tester | |

### Test Results

| Test Category | Passed | Failed | Notes |
|--------------|--------|--------|-------|
| Network Status Display | | | |
| Slow Network - Event Creation | | | |
| Slow Network - Map Loading | | | |
| Slow Network - Contacts Import | | | |
| Slow Network - Authentication | | | |
| Intermittent - Event Creation | | | |
| Intermittent - Map Usage | | | |
| Intermittent - Realtime | | | |
| WiFi to Cellular Transition | | | |
| Cellular to WiFi Transition | | | |
| VPN Enable/Disable | | | |
| Offline to Online Recovery | | | |
| Extended Offline (1h+) | | | |
| Rapid Recovery Cycles | | | |
| Notification While Online | | | |
| Notification While Offline | | | |
| Notification During Transition | | | |
| Multiple Offline Notifications | | | |
| Offline UI State | | | |
| Queue and Retry | | | |
| Timeout Handling | | | |
| Server Error Handling | | | |
| Performance Targets | | | |
| **TOTAL** | | | |

---

## Success Criteria

- [ ] Network status displays accurately
- [ ] All connection types detected correctly
- [ ] Slow network operations complete within acceptable time
- [ ] Intermittent connections don't cause crashes
- [ ] Network transitions handled gracefully
- [ ] Offline state clearly communicated
- [ ] Queued operations retry automatically
- [ ] Push notifications delivered correctly during transitions
- [ ] Timeouts handled with user feedback
- [ ] Server errors show appropriate messages
- [ ] App remains responsive under poor network
- [ ] Battery impact is reasonable
- [ ] No connection leaks
- [ ] Data is preserved across network changes
