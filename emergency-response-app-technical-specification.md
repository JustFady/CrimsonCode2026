# Emergency Response Application - Technical Specification

---

## Executive Summary

Cross-platform emergency response mobile application for USA users. Single device per account, phone number authentication, 50-mile public alert radius, 48-hour event expiration.

**Target Platforms:** Android, iOS (Tauri v2)
**Target Region:** USA only
**Target Language:** English only
**Account Model:** One device per phone number

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Svelte 5 with Runes |
| Mobile Framework | Tauri v2 (Android + iOS only) |
| Database | Supabase PostgreSQL |
| ORM | Prisma 7 |
| Authentication | Supabase Auth + Twilio Verify |
| Session Management | Tauri Stronghold plugin + Biometrics |
| Real-time | Supabase Realtime |
| Maps | Leaflet + Leaflet.markercluster |
| Map Tiles | OpenStreetMap (free) |
| Push Notifications | Firebase Cloud Messaging (free) |
| Geolocation | Tauri Geolocation plugin |
| Storage | Supabase Storage (for future attachments) |
| Backend | Supabase Edge Functions |

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│          CLIENT LAYER                  │
│                                       │
│  ┌────────────────┐  ┌──────────────┐│
│  │  Tauri Android│  │Tauri iOS    ││
│  │                │  │              ││
│  │  - Svelte 5    │  │- Svelte 5   ││
│  │  - Leaflet     │  │- Leaflet    ││
│  │  - Stronghold  │  │- Stronghold ││
│  │  - FCM Plugin  │  │- FCM Plugin ││
│  │  - Geolocation │  │- Geolocation││
│  └────────┬───────┘  └──────┬───────┘│
└───────────┼──────────────────┼──────────┘
            │                  │
            └────────┬─────────┘
                     │
        ┌────────────┴────────────┐
        │   HTTPS / WebSocket    │
        └────────────┬────────────┘
                     │
        ┌────────────┴────────────┐
        │    SUPABASE PLATFORM   │
        │                        │
        │  ┌────────────────────┐│
        │  │   Postgres DB     ││
        │  └────────┬───────────┘│
        │           │            │
        │  ┌────────┴────────┐ │
        │  │  Row Level Sec  │ │
        │  └─────────────────┘ │
        │                        │
        │  ┌────────────────────┐│
        │  │  Realtime (WAL)  ││
        │  └────────────────────┘│
        │                        │
        │  ┌────────────────────┐│
        │  │ Edge Functions    ││
        │  └────────────────────┘│
        └────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │ EXTERNAL INTEGRATIONS    │
        │                         │
        │  ┌─────────┐ ┌─────────┐│
        │  │ Twilio  │ │ Firebase ││
        │  │ Verify  │ │   FCM   ││
        │  └─────────┘ └─────────┘│
        └───────────────────────────┘
```

---

## Database Schema

### Users Table
- Primary Key: UUID
- Fields:
  - Phone number (unique, indexed)
  - Device ID (unique, indexed)
  - isActive: boolean
  - createdAt: timestamp
  - lastActiveAt: timestamp
- Constraint: One user per phone number + device ID combination

### Contacts Table
- Primary Key: UUID
- Fields:
  - Phone number (unique, indexed)
  - Display name
  - hasApp: boolean (indexed)
  - createdAt: timestamp
- Purpose: Global registry of all contacts across system

### UserContacts Table
- Primary Key: Composite (userId, contactId)
- Fields:
  - User ID (foreign key)
  - Contact ID (foreign key)
  - AddedAt: timestamp
- Constraint: Unique combination of user and contact
- Purpose: The list of private contacts for alert delivery

### Events Table
- Primary Key: UUID
- Fields:
  - User ID (foreign key) - Creator of event
  - Severity: enum (ALERT, CRISIS)
  - Category: enum (MEDICAL, FIRE, WEATHER, CRIME, NATURAL_DISASTER, INFRASTRUCTURE, SEARCH_RESCUE, TRAFFIC, OTHER)
  - Latitude: float
  - Longitude: float
  - LocationOverride: string (optional, for manual location entry)
  - BroadcastType: enum (PUBLIC, PRIVATE)
  - Description: string (max 500 characters)
  - IsAnonymous: boolean (true for public events, false for private)
  - ExpiresAt: timestamp (createdAt + 48 hours)
  - CreatedAt: timestamp
- Indexes:
  - User ID
  - Location (PostGIS for radius queries)
  - Severity
  - BroadcastType
  - ExpiresAt
  - CreatedAt

### EventRecipients Table
- Primary Key: Composite (eventId, userId)
- Fields:
  - Event ID (foreign key)
  - User ID (foreign key)
  - NotifiedAt: timestamp
  - AcknowledgedAt: timestamp (optional)
  - ClearedAt: timestamp (optional, when user clears from their list)
  - IsPublicRecipient: boolean (true if recipient matched via public radius, false if from private contacts)
- Purpose: Track which users received which events and their interaction status

### UserDevices Table
- Primary Key: UUID
- Fields:
  - User ID (foreign key)
  - FCM token (indexed)
  - Platform: enum (ANDROID, IOS)
  - DeviceModel: string
  - LastUsedAt: timestamp
- Purpose: Map users to push notification tokens (one token per user due to single-device model)

### EventCategories Table
- Primary Key: UUID
- Fields:
  - Name: enum (pre-defined categories)
  - Description: string
  - Icon: string (SVG reference)
  - Color: string (hex)
  - DefaultSeverity: enum (ALERT, CRISIS)
- Pre-populated data, user-configurable

---

## Data Model Relationships

```
Users (1) ----< (N) UserContacts ----> (N) Contacts
Users (1) ----< (N) Events
Users (1) ----< (1) UserDevices
Users (1) ----< (N) EventRecipients ----> (N) Events
Events (N) ----> (1) EventCategories
```

---

## Authentication System Design

### Account Model

**One Device Per Account:**
- User account uniquely identified by phone number + device ID combination
- No support for multiple devices per phone number
- Attempting to log in from new device with same phone number requires re-registration (MVP behavior)

### Authentication Flow

**Registration:**
1. User opens app, enters USA phone number
2. Phone number format validated (E.164 format for USA: +1XXXXXXXXXX)
3. Twilio Verify sends 6-digit OTP via SMS
4. User enters OTP code
5. Server validates code
6. Device ID generated from device hardware characteristics
7. Server creates user record with phone number + device ID
8. Access token and refresh token generated
9. Tokens stored in Tauri Stronghold encrypted vault
10. User navigates to contact selection screen

**Subsequent App Opens:**
1. App checks for existing session in Stronghold vault
2. If valid refresh token exists: Request biometric authentication
3. Biometric verified: New access token issued, refresh token remains
4. No OTP required
5. Navigate to main application

**Re-authentication Required When:**
- Refresh token expires (30 days)
- Device is factory reset
- App is reinstalled
- Stronghold vault data is cleared

### Session Management

| Token Type | Storage | Expiration |
|------------|---------|-----------|
| Access Token | Memory only | 15 minutes |
| Refresh Token | Stronghold encrypted vault | 30 days |
| FCM Push Token | Supabase database | Until device uninstalls app |

### Biometric Authentication

- Required to unlock app and access features
- Required to create emergency events
- Falls back to device passcode if biometrics unavailable
- No rate limiting in MVP

### Security Measures (MVP)

- Tokens signed with HS256
- Refresh token does not rotate in MVP
- Device ID bound to hardware characteristics
- Single device enforced by database constraints
- Session persists for 30 days with biometric re-auth

---

## Geolocation System Design

### Location Services

**High-Precision Mode (Active Emergency):**
- GPS primary, Wi-Fi positioning fallback
- Accuracy target: Under 10 meters
- Update interval: 5 seconds
- Battery impact: High

**Balanced Mode (Standard Operation):**
- GPS + Wi-Fi + Cellular triangulation
- Accuracy target: Under 50 meters
- Update interval: 30 seconds
- Battery impact: Moderate

**Low-Power Mode (Background):**
- Cellular tower positioning only
- Accuracy target: Under 1 kilometer
- Update interval: 5 minutes
- Battery impact: Minimal

### Permission Handling

**Foreground Permission (Required):**
- Prompted on first location request
- Required for creating events
- Required for viewing nearby public events

**Background Permission (Optional):**
- Requested during first event creation
- Enables tracking during active emergency

### Location Fallback Chain

1. GPS with WAAS/EGNOS augmentation
2. Wi-Fi positioning
3. Cell tower triangulation
4. IP-based geolocation
5. Manual location entry on map

### Accuracy Display

- Real-time accuracy circle on user location marker
- Color-coded: Green (under 10m), Yellow (10-50m), Orange (50-100m), Red (over 100m)

---

## Real-Time Messaging Design

### Channel Architecture

**Private User Channel:** `user:{userId}`
- Receives events where user is in creator's contact list
- One-way notifications only

**Public Regional Channel:** `public:{regionHash}`
- Receives all public events within 50-mile radius
- Region based on user's last known location

### Message Flow

**Event Creation:**
1. User creates event with location, severity, category, broadcast type
2. Server validates event data
3. Event written to database with ExpiresAt set to 48 hours from creation
4. Server identifies recipients based on broadcast type

**Private Broadcast:**
1. Query creator's UserContacts list
2. Filter contacts where hasApp is true
3. Create EventRecipient records for each contact
4. Push notification sent via FCM to each recipient
5. Real-time message sent to each recipient's private channel

**Public Broadcast:**
1. Query users within 50-mile radius using PostGIS from Events table
2. Exclude users who have opted out of public alerts
3. Create EventRecipient records for each matched user
4. Batch push notifications
5. Real-time message sent to regional channel

**Public Event Anonymity:**
- User ID is stored in Events table for tracking and moderation
- Public broadcasts do not include creator's phone number or any identifying information
- Recipients see anonymous public event (no creator information)

**Private Event Identification:**
- Private events include creator's information to recipients
- Recipients can see which contact created the private alert

---

## Push Notification Design

### Notification Types

**Crisis Alert:**
- Sound: Custom emergency sound
- Vibration: Aggressive pattern
- Priority: High (bypasses silent mode)
- Actions: "View on Map"

**Alert (Warning):**
- Sound: Default notification sound
- Vibration: Standard pattern
- Priority: Normal
- Actions: "View on Map"

### Payload Structure

**Standard Alert Payload:**
- Title: Severity + Category (e.g., "CRISIS - Medical")
- Body: Brief description
- Icon: Severity-appropriate icon
- Data: Event ID, coordinates, severity, deep link URL

**Deep Linking:**
- Custom URL scheme: `emergency://event/{eventId}`
- Opens app, navigates to event on map

### Notification Channels (Android)

**Emergency Alerts:**
- Importance: High
- Sound: Custom emergency sound
- Bypass DND: Enabled

**Alerts:**
- Importance: Default
- Sound: Default notification sound

---

## Maps and Visualization Design

### Map Tile Strategy

**Primary Tile Provider:** OpenStreetMap Standard
- Cost: Free, no API key required
- Coverage: Global
- Attribution required

**Offline Caching:**
- Cache tiles for user's region (zoom levels 12-16)
- Service Worker for offline tile delivery
- Cache expiration: 7 days

### Marker System

**Severity-Based Styling:**
- Crisis: Red circle with pulsing animation
- Alert: Orange circle with static styling

**Category Indicators:**
- Small icon overlay on marker indicating category
- Icons: Fire (flame), Medical (cross), Weather (cloud), etc.

**Clustering:**
- Leaflet.markercluster for dense areas
- Cluster color: Maximum severity within cluster
- Spiderfy behavior at maximum zoom

### User Location Display

**Accuracy Circle:**
- Blue circle with transparency
- Dynamic size based on accuracy reading
- Updates with each location change

---

## Contact Management Design

### Contact Import Flow

**Permission Request:**
1. User navigates to contact selection screen
2. System requests contacts read permission
3. If granted: Load device contacts
4. If denied: Manual entry only mode

**Contact Processing:**
1. Device contacts scanned for phone numbers
2. Contacts compared against global Contacts table
3. New contacts added to registry
4. Existing contacts retrieved with current data

**App Detection:**
1. Phone numbers checked against Users table
2. Contacts with matching phone number marked hasApp = true
3. Visual distinction: Badge or icon for app users

**Selection Interface:**
1. Contacts displayed as list with search/filter
2. Multi-select enabled
3. Show/hide toggle for "App Users Only"
4. Save button persists selections to UserContacts table

**Contact Display:**
- Name
- Phone number (masked: +1 (***) ***-1234)
- App user status (badge)

### Contact Sync

**Initial Import:**
- All device contacts imported on first authorization

**Ongoing Sync:**
- Sync when user opens contact selection screen

**Manual Entry:**
- Available for users who deny contacts permission
- Requires phone number and optional name

---

## Event Categories Design

### Standard Categories

**Medical** - Red, Medical cross icon
**Fire** - Orange, Flame icon
**Weather** - Purple, Cloud with lightning icon
**Crime** - Red, Shield icon
**Natural Disaster** - Dark red, Mountain peak icon
**Infrastructure** - Yellow, Gear icon
**Search & Rescue** - Bright red, Magnifying glass icon
**Traffic** - Green, Car icon
**Other** - Blue, Question mark icon

---

## Event Creation Flow

### User Interface

**Main Screen:**
- Map view centered on user location
- Floating action button: "+" to create new event
- Event list toggle: Top-right button showing count of unacknowledged events
- User location marker with accuracy indicator

### Event Creation Wizard

**Step 1: Category Selection**
- Grid of category icons with labels
- Category name and description displayed on selection

**Step 2: Severity Selection**
- Two buttons: "Warning" (Alert) and "Crisis"
- Visual distinction: Orange vs Red
- Default: Warning

**Step 3: Broadcast Scope**
- Toggle: "Public" vs "Private"
- Default: Public (50-mile radius)
- Public shows "All nearby users within 50 miles"
- Private shows "Your selected emergency contacts"

**Step 4: Location Confirmation**
- Map preview showing user's current location
- Accuracy circle displayed
- "Edit location" option to manually pin different location
- "Use my location" button to reset to GPS location

**Step 5: Description**
- Text field for additional details (max 500 characters)
- Character count displayed

**Step 6: Review & Submit**
- Summary of all selections
- "Create Alert" button (primary action)
- "Cancel" button

---

## Event Display & Interaction Design

### Map Display

**Event Markers:**
- All public events within map bounds displayed
- Private events only displayed if user is recipient
- Marker style reflects severity and category

**Event Information Panel:**
- Bottom sheet panel slides up on marker click
- Contains: severity, category, description, location, time
- If private: shows which contact created the event
- If public: anonymous (no creator information)
- "Clear from list" button to remove from user's event list

**Event List View:**
- Accessed via top-right button on map
- Shows all events user has received
- Tapping event zooms map to location
- "Clear all" button to remove all events from list

### Event Lifecycle

**Active State:**
- Default state for new events
- Marker displayed on map
- Event expires 48 hours after creation (automatic database cleanup)
- User can manually clear event from their list

**Acknowledged State:**
- Set when recipient views event or taps notification
- Event remains on map and in list
- Visual indicator shows event has been acknowledged

**Cleared State:**
- User manually clears event from their list
- ClearedAt timestamp recorded in EventRecipients table
- Event removed from user's event list view
- Event still visible on map for others (only removed from this user's list)
- User can still access event if they navigate directly to it on map

**Expired State:**
- Automatic after 48 hours from creation
- Server removes from Events table and EventRecipients table
- No longer visible to any user

**No Resolution Feature:**
- Events are never resolved or marked as completed
- Events only expire or are cleared by individual users
- No status change from "active" to any other state

---

## User Settings & Preferences

### Notification Settings

**Push Notifications:**
- Master toggle: Enable/disable all notifications
- Critical alerts: Toggle (cannot be fully disabled due to emergency nature)
- Warning alerts: Toggle
- Public alerts: Toggle (user can opt-out of receiving all public alerts)
- Private alerts: Toggle

**Notification Preferences:**
- Sound selection per severity
- Vibration toggle

### Location Settings

**Location Accuracy:**
- High precision mode toggle
- Default: Balanced mode

**Background Location:**
- Enable/disable background tracking
- Default: Enabled during active emergencies only

**Privacy:**
- Share location with contacts toggle
- Participate in public alerts toggle (this is the opt-out for receiving public alerts)
- Default: Both enabled

### Contact Settings

**Emergency Contacts:**
- Add/remove contacts
- Import from device contacts
- Manual entry option
- This is the single list that receives private alerts

### Account Settings

**Authentication:**
- Logout button
- Re-authentication required on next app open

---

## Offline Functionality Design

### Offline Capabilities

**Cached Content:**
- User's emergency contacts list
- Event details of recently viewed events
- Map tiles for user's region
- User preferences

**Offline Event Creation:**
- Events queued locally when offline
- Synced to server when connection restored
- Timestamp reflects creation time, not sync time
- Visual indicator shows queued events

### Sync Strategy

**Priority Queue:**
1. Emergency event creation (highest)
2. Event acknowledgment
3. Contact list updates
4. User preferences

---

## Performance Optimization

### Frontend

**Component Rendering:**
- Virtual scrolling for event lists
- Markers only rendered within visible map bounds
- Marker clustering

**Data Loading:**
- Incremental loading for large datasets
- Debounced search for contact filtering

### Backend

**Database Indexing:**
- Composite indexes for common queries
- PostGIS geospatial indexes for location queries
- Index on ExpiresAt for automatic cleanup job

**Query Optimization:**
- Prepared statements for repeated queries
- Connection pooling via Supabase

---

## Security Architecture

### Data Security

**Encryption:**
- Tokens encrypted in Stronghold vault
- Database connections encrypted with TLS

**Row Level Security:**
- Users can only access their own data
- Events only visible to recipients or within geographic bounds
- Public events anonymous in delivery to recipients

### Privacy Controls

**Data Minimization:**
- Phone number only personally identifiable information collected
- Public events do not expose creator identity
- Location data retained per event (48 hours)

**User Control:**
- Clear events from personal list
- Opt-out of public alerts entirely

---

## Testing Strategy

### Device Testing

**Android:**
- Multiple Android versions (API 28+)
- Various OEMs (Samsung, Google, Pixel)
- Different screen sizes

**iOS:**
- Multiple iOS versions
- Different iPhone models
- Different screen sizes

**Location Testing:**
- GPS accuracy verification
- Indoor positioning fallback
- Background location tracking

**Network Testing:**
- Offline mode functionality
- Slow network conditions
- Connection recovery

---

## Deployment Strategy

### Development Environment

**Local:**
- Supabase local development environment
- Tauri local development server

### Production Environment

**Infrastructure:**
- Supabase Pro plan
- Twilio Verify (USA only)
- Firebase FCM (free)
- OpenStreetMap tiles (free)

**Release Process:**
- Automated builds
- Code signing for iOS and Android
- App store submission
- Staged rollout

---

## Cost Analysis

### Monthly Recurring

| Service | Monthly Cost |
|----------|--------------|
| Supabase Pro | $25 |
| Twilio Verify (USA) | $50-100 |
| Firebase FCM | $0 |
| OpenStreetMap Tiles | $0 |
| **Total** | **$75-125/month** |

### Projected Annual

**Base:** $900-1,500/year

---

## Implementation Stages

### Stage 1: Foundation (Weeks 1-4)

**Deliverables:**
- Tauri v2 project initialized with Svelte 5
- Supabase project created and configured
- Prisma schema defined and initial migration applied
- Authentication flow (Twilio Verify + Supabase Auth)
- Stronghold plugin for secure token storage
- Biometric authentication flow
- Basic UI shell with navigation
- Development environment functional

**Success Criteria:**
- User can register with phone number and OTP
- Session persists securely across app restarts
- Biometric unlock functions on subsequent logins

---

### Stage 2: Contact Management (Weeks 5-6)

**Deliverables:**
- Device contacts permission request flow
- Contacts import and processing
- Contact selection UI with multi-select
- App user status detection
- Contact data persistence to UserContacts table

**Success Criteria:**
- User can import contacts from device
- Contacts with app are visually distinguished
- Selected contacts saved as single list for private alerts

---

### Stage 3: Maps & Location (Weeks 7-8)

**Deliverables:**
- Leaflet map integration with OSM tiles
- Geolocation integration with permission handling
- User location marker with accuracy visualization
- Map interaction patterns
- Location fallback chain
- Accuracy display with color coding
- Map tile caching via Service Worker

**Success Criteria:**
- Map displays with user location
- Location accuracy visualized
- Map responsive on mobile

---

### Stage 4: Event Creation (Weeks 9-10)

**Deliverables:**
- Category selection interface
- Severity selection interface (Alert/Crisis)
- Broadcast scope toggle (public/private, default public)
- Location confirmation with manual override
- Description input (max 500 characters)
- Event creation validation
- Event submission with 48-hour expiration
- Public event anonymity in database

**Success Criteria:**
- User can create events with all required fields
- Events saved with 48-hour expiration
- Public broadcasts include 50-mile radius logic

---

### Stage 5: Real-Time Messaging (Weeks 11-12)

**Deliverables:**
- Supabase Realtime channel subscription
- Private event broadcast to UserContacts list
- Public event broadcast to nearby users (50-mile radius)
- Event recipient tracking
- Message delivery confirmation
- Connection management and reconnection

**Success Criteria:**
- Private contacts receive notifications
- Public broadcasts reach users within 50-mile radius
- Delivery status tracked

---

### Stage 6: Push Notifications (Weeks 13-14)

**Deliverables:**
- FCM plugin integration for Tauri
- Device token registration
- Push notification payload construction
- Severity-based notification styling
- Deep link handling from notifications
- Notification channel configuration (Android)

**Success Criteria:**
- Users receive push notifications for events
- Tapping notification opens app to event on map
- Critical alerts have enhanced behavior

---

### Stage 7: Event Display & Interaction (Weeks 15-16)

**Deliverables:**
- Event marker display on map
- Severity and category visualization
- Marker clustering
- Event details bottom panel
- Event list view with filtering
- Map navigation to event
- Event acknowledgment flow
- Clear from list functionality
- Private events show creator info, public events anonymous

**Success Criteria:**
- Events display with appropriate styling
- Users can view details and navigate to location
- Users can clear events from their list

---

### Stage 8: User Settings & Preferences (Week 17)

**Deliverables:**
- Settings screen navigation
- Notification preference toggles
- Location preference controls
- Contact management interface
- Public alert opt-out toggle
- Account logout

**Success Criteria:**
- Users can configure app behavior
- Preferences saved and restored
- Users can manage emergency contacts (single list)

---

### Stage 9: Offline Functionality (Week 18)

**Deliverables:**
- Service Worker for offline caching
- IndexedDB for offline data
- Offline event creation with queue
- Sync strategy
- Offline indicator UI
- Network status monitoring

**Success Criteria:**
- App functions offline for basic operations
- Queued events sync when connection restored
- Users informed of offline status

---

### Stage 10: Testing & QA (Weeks 19-20)

**Deliverables:**
- Unit test suite for critical functions
- Integration tests for major flows
- Cross-device testing (Android, iOS)
- Performance testing
- Bug tracking and resolution

**Success Criteria:**
- Critical bugs resolved
- App performs well on target devices

---

### Stage 11: Beta Deployment (Weeks 21-22)

**Deliverables:**
- Beta build distribution
- Crash reporting integration
- Feedback collection mechanism
- Performance monitoring
- Issue tracking

**Success Criteria:**
- Beta testers can use app
- Critical issues identified
- Performance metrics gathered

---

### Stage 12: Production Launch (Weeks 23-24)

**Deliverables:**
- Production build with code signing
- App Store submission (iOS + Android)
- Production infrastructure deployed
- Monitoring configured

**Success Criteria:**
- App available for download
- Production infrastructure stable

---

## Final Summary

**Platforms:** Tauri Android, Tauri iOS (no desktop, no tablet)
**Region:** USA only
**Language:** English only
**Account Model:** One device per phone number
**Event Expiration:** 48 hours automatic
**Broadcast Radius:** 50 miles for public alerts
**Event Resolution:** Not supported (events only cleared by users or expire)
**Public Events:** Anonymous (no creator information shown)
**Private Events:** Shows which contact created the alert
**Contacts:** Single list for private alert delivery
**Description Limit:** 500 characters
**Opt-out:** Users can opt-out of public alerts
