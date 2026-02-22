# Location Accuracy Testing Plan - CrimsonCode2026 Emergency Response App

## Overview

This document provides a comprehensive testing plan for location accuracy features of the CrimsonCode2026 Emergency Response App.

**Key Areas:**
1. GPS Accuracy Verification
2. Indoor Positioning Fallback
3. Location Timeout Handling
4. Manual Location Entry

---

## 1. GPS Accuracy Verification

### High-Precision Mode (Active Emergency)

**Target:** Under 10 meters accuracy
**Update Interval:** 5 seconds

**Test Scenarios:**

| Scenario | Expected Accuracy | Test Method |
|----------|------------------|--------------|
| Open sky, outdoor | < 10 meters (HIGH) | Stand in open area with clear sky view |
| Urban canyon | < 20 meters (GOOD) | Test between tall buildings in downtown |
| Park with trees | < 15 meters (HIGH/GOOD) | Test under moderate tree cover |
| Near water body | < 10 meters (HIGH) | Test near lake/ocean with open sky |
| Elevated location | < 10 meters (HIGH) | Test from high floor or hilltop |

**Verification Steps:**
1. Set location mode to High-Precision
2. Wait for 10-15 seconds for GPS lock
3. Record accuracy reading from accuracy indicator
4. Verify color is GREEN (< 10m) or YELLOW (< 50m) depending on conditions
5. Confirm location updates every ~5 seconds

**Success Criteria:**
- Open sky: Accuracy < 10 meters (GREEN indicator)
- Urban: Accuracy < 50 meters (YELLOW indicator)
- Updates arrive within 5-8 seconds

### Balanced Mode (Standard Operation)

**Target:** Under 50 meters accuracy
**Update Interval:** 30 seconds

**Test Scenarios:**

| Scenario | Expected Accuracy | Test Method |
|----------|------------------|--------------|
| Suburban area | < 30 meters (GOOD) | Test in residential area |
| Shopping mall interior | < 50 meters (FAIR) | Test indoor in large building |
| Office building | < 100 meters (FAIR) | Test in multi-story office |
| Mixed indoor/outdoor | < 50 meters (GOOD) | Walk between indoor and outdoor |

**Verification Steps:**
1. Set location mode to Balanced
2. Wait for first location fix
3. Record accuracy reading
4. Confirm accuracy level is appropriate for environment
5. Verify updates arrive every ~30 seconds

**Success Criteria:**
- Outdoor: Accuracy < 30-50 meters (YELLOW/GREEN)
- Indoor: Accuracy < 100 meters (ORANGE)
- Updates arrive within 25-35 seconds

### Low-Power Mode

**Target:** Under 1 kilometer accuracy
**Update Interval:** 2-5 minutes

**Test Scenarios:**

| Scenario | Expected Accuracy | Test Method |
|----------|------------------|--------------|
| City-wide movement | < 500 meters | Test while driving through city |
| Regional area | < 1000 meters | Test while traveling between suburbs |
| Background use | < 1000 meters | Test with app in background |

**Verification Steps:**
1. Set location mode to Low-Power
2. Wait for initial location
3. Record accuracy
4. Confirm updates arrive every 2-5 minutes
5. Verify battery usage is minimal

**Success Criteria:**
- Accuracy < 1 kilometer
- Updates arrive within 2-5 minute window
- Battery impact is minimal

---

## 2. Indoor Positioning Fallback

### Fallback Chain Test

**Expected Chain:** GPS → WiFi → Cellular → IP → Manual

**Test Method:**

| Step | Action | Expected Result |
|-------|--------|----------------|
| 1 | Start outdoors with GPS | HIGH/GOOD accuracy, GPS source |
| 2 | Enter building with WiFi | GOOD/FAIR accuracy, WiFi source |
| 3 | Go to basement (no WiFi) | FAIR/LOW accuracy, Cellular source |
| 4 | Enable airplane mode (no cellular) | UNKNOWN accuracy, IP fallback or Manual prompt |
| 5 | Set manual location on map | MANUAL source, accuracy specified |

**Verification Steps:**

**WiFi Positioning Test:**
1. Start in building with known WiFi access points
2. Verify location updates within 50-100m accuracy
3. Confirm source shows as WiFi (accuracy between 20-100m)

**Cellular Triangulation Test:**
1. Disable WiFi on device
2. Move to area with poor GPS signal
3. Verify location updates within 100-500m accuracy
4. Confirm source inferred as Cellular (accuracy 100-500m)

**IP Geolocation Test:**
1. Enable airplane mode, disable WiFi
2. Enable WiFi only (no location services)
3. Verify IP geolocation is attempted
4. Confirm location is at city/regional level (no accuracy)

**Manual Location Fallback Test:**
1. In areas with no location signal
2. Verify app prompts for manual location entry
3. Set location by tapping on map
4. Confirm manual location is used for event creation

**Success Criteria:**
- App gracefully transitions between sources
- User is informed of current accuracy level
- Manual entry is available when no sources work
- Accuracy indicator reflects actual accuracy

---

## 3. Location Timeout Handling

### Timeout Scenarios

| Scenario | Expected Behavior |
|----------|------------------|
| GPS signal loss (tunnel) | WiFi/Cellular fallback, no app crash |
| Network timeout for IP | Shows last known location, retries on network |
| Permission denied | Shows manual entry option, prompts settings |
| Background timeout | Resumes tracking on app return |

**Test Methods:**

**GPS Signal Loss Test:**
1. Start with high-precision mode outdoors
2. Drive through tunnel or parking garage
3. Verify app continues with WiFi/Cellular location
4. Confirm accuracy indicator changes appropriately
5. Check app doesn't crash or freeze

**Network Timeout Test:**
1. Disconnect network while testing IP fallback
2. Request location
3. Verify timeout is handled gracefully
4. Confirm error message displayed
5. Check app doesn't hang indefinitely

**Permission Denied Test:**
1. Revoke location permission in system settings
2. Open app
3. Verify manual location option is available
4. Test settings deep link to permission screen

**Background Return Test:**
1. Start location tracking
2. Switch to another app for 5+ minutes
3. Return to app
4. Verify location resumes tracking
5. Confirm location is recent (not stale)

**Success Criteria:**
- No app crashes during signal loss
- Timeout handled with user feedback
- Manual entry available when needed
- Background tracking resumes properly

---

## 4. Manual Location Entry

### Manual Location Flows

**Test Scenarios:**

| Scenario | Steps | Expected Result |
|----------|--------|----------------|
| Map pin tap | Tap location on map → Confirm | Manual location set |
| Use my location reset | Tap "Use my location" | GPS location restored |
| Clear manual location | Tap clear option | Returns to GPS tracking |
| Multiple manual pins | Set pin, change pin | Latest pin used |

**Verification Steps:**

**Map Pin Entry Test:**
1. Open map view
2. Long-press or tap desired location
3. Confirm location selection
4. Verify marker shows at pinned location
5. Create event - confirm uses pinned location

**Reset to GPS Test:**
1. Set manual location
2. Tap "Use my location" button
3. Verify marker moves to GPS location
4. Confirm GPS tracking resumes

**Clear Manual Location Test:**
1. Set manual location
2. Clear manual location
3. Verify GPS location is used
4. Confirm indicator returns to GPS accuracy

**Multiple Pin Test:**
1. Set manual location at Point A
2. Change manual location to Point B
3. Verify Point B is used for event
4. Confirm Point A is discarded

**Success Criteria:**
- Manual location overrides GPS when set
- "Use my location" restores GPS
- Clearing manual location returns to GPS
- Only latest manual location is used

---

## Accuracy Indicator Testing

### Color Coding Verification

| Accuracy Level | Range | Color | Test Value |
|---------------|--------|-------|------------|
| HIGH | < 10 meters | GREEN | 5m location |
| GOOD | 10-50 meters | YELLOW | 30m location |
| FAIR | 50-100 meters | ORANGE | 75m location |
| LOW | > 100 meters | RED | 150m location |
| UNKNOWN | N/A | No color | null accuracy |

**Test Steps:**
1. Use developer tools or mock location to set specific accuracy values
2. Verify accuracy circle color matches expected
3. Confirm circle size scales with accuracy
4. Check visual consistency across all modes

**Success Criteria:**
- Colors match specification exactly
- Circle size reflects accuracy (larger = less accurate)
- Transitions between levels are smooth
- No visual glitches on level changes

---

## Device-Specific Testing

### Android Devices

| Device Type | GPS Quality | Notes |
|-------------|--------------|-------|
| Pixel 8 Pro | Excellent | Native GPS with dual frequency |
| Galaxy S24 | Good | Samsung GPS, may vary |
| Budget phone | Fair | May have weaker GPS chip |
| Tablet | Varies | Often no GPS, WiFi-only |

### iOS Devices

| Device Type | GPS Quality | Notes |
|-------------|--------------|-------|
| iPhone 16 Pro | Excellent | Latest GPS chip |
| iPhone 15 | Good | Standard iOS GPS |
| iPhone SE | Fair | Older GPS hardware |
| iPad WiFi-only | None | WiFi positioning only |

---

## Environment Testing Matrix

### Outdoor Environments

| Environment | Expected Accuracy | Test Focus |
|------------|------------------|-------------|
| Open field | < 5m | Best-case GPS |
| Dense forest | 10-30m | Tree cover impact |
| Urban canyon | 20-50m | Building reflection |
| Coastal | < 5m | Water reflection effect |
| Mountain | 5-15m | Altitude impact |

### Indoor Environments

| Environment | Expected Accuracy | Test Focus |
|------------|------------------|-------------|
| Home | 20-50m | WiFi positioning |
| Office building | 30-100m | Multiple APs |
| Shopping mall | 50-100m | Large indoor space |
| Hospital | 50-100m | Dense building structure |
| Basement | 100-500m | Cellular fallback |

### Edge Cases

| Scenario | Expected Behavior |
|----------|------------------|
| Underground parking | Cellular or manual fallback |
| Airplane mode | Manual location only |
| Dead zone (no signal) | Manual location prompt |
| International travel | GPS should work (if enabled) |
| Rapid movement | Smooth location updates |

---

## Performance Testing

### Location Update Latency

| Mode | Target | Acceptable |
|------|---------|------------|
| High-Precision | 5s | 5-10s |
| Balanced | 30s | 25-35s |
| Low-Power | 3 min | 2-5 min |

**Test Method:**
1. Record timestamp of each location update
2. Calculate interval between updates
3. Verify interval matches mode specification
4. Check for jitter or missed updates

### Battery Impact

| Mode | Expected Battery Impact |
|------|------------------------|
| High-Precision | High (drain faster) |
| Balanced | Moderate |
| Low-Power | Minimal |

**Test Method:**
1. Start with 100% battery
2. Run in each mode for 30 minutes
3. Record battery percentage
4. Verify High-Precision > Balanced > Low-Power drain

---

## Known Limitations and Workarounds

### GPS Limitations

| Limitation | Impact | Workaround |
|------------|---------|------------|
| Doesn't work indoors | Rely on WiFi/Cellular | User education on accuracy |
| Poor in urban canyons | Reduced accuracy | Use manual pinning if critical |
| Cold start delay | 10-30 seconds for first fix | Warn user, show last known |

### Fallback Limitations

| Limitation | Impact | Workaround |
|------------|---------|------------|
| IP geolocation is coarse | City-level accuracy only | Manual entry for precise location |
| WiFi positioning not everywhere | May fallback to cellular | Accept lower accuracy or manual |
| Cellular triangulation varies | 100m-5km accuracy | Manual entry for emergencies |

---

## Test Execution Template

### Location Accuracy Test Record

| Field | Value |
|-------|-------|
| Device Model | |
| Platform (Android/iOS) | |
| Location Mode | |
| Environment | |
| Date/Time | |
| Tester | |

### Accuracy Measurements

| Reading # | Mode | Environment | Accuracy (m) | Level | Source | Notes |
|-----------|------|-------------|---------------|-------|--------|-------|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |
| 5 | | | | | | |

### Test Results

| Test Category | Passed | Failed | Notes |
|--------------|--------|--------|-------|
| High-Precision Mode | | | |
| Balanced Mode | | | |
| Low-Power Mode | | | |
| WiFi Fallback | | | |
| Cellular Fallback | | | |
| IP Fallback | | | |
| Manual Entry | | | |
| Timeout Handling | | | |
| Accuracy Indicator | | | |
| **TOTAL** | | | |

---

## Success Criteria

- [ ] All accuracy levels display correct colors
- [ ] Fallback chain works GPS → WiFi → Cellular → IP → Manual
- [ ] Timeout handling doesn't cause app crashes
- [ ] Manual location entry overrides GPS
- [ ] Reset to GPS works correctly
- [ ] Update intervals match mode specifications
- [ ] Battery impact is reasonable for each mode
- [ ] User experience is smooth during mode transitions
- [ ] Error messages are clear and actionable
