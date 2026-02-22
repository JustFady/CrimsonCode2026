# Emergency Response Application - Technical Specification

---

## Executive Summary

Cross-platform emergency response mobile application for USA users. Single device per account, phone number OTP authentication, 50-mile public alert radius, 48-hour event expiration.

**Target Platforms:** Android, iOS (Kotlin Multiplatform / Jetpack Compose Multiplatform)
**License:** MIT
**Target Region:** USA only
**Target Language:** English only
**Account Model:** One device per phone number

---

## Repository Structure

```
├── apps/
│   └── mobile/              # Kotlin Multiplatform mobile app
│       ├── androidApp/        # Android-specific code and configuration
│       ├── iosApp/           # iOS-specific code and configuration
│       └── shared/           # Common code shared across platforms
│           ├── compose/        # Shared UI with Compose Multiplatform
│           ├── data/           # Data layer, repositories
│           ├── domain/         # Business logic, use cases
│           └── di/            # Dependency injection
├── backend/                  # Supabase Edge Functions
│   ├── migrations/           # SQL database migrations
│   ├── functions/            # Edge functions
│   └── config/              # Supabase configuration
├── infrastructure/           # Infrastructure as code
│   ├── supabase/            # Supabase project setup
│   └── firebase/             # Firebase FCM configuration
└── docs/                    # Project documentation
    ├── emergency-response-app-technical-specification.md
    └── dbSchema.md
```

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Jetpack Compose Multiplatform |
| Mobile Framework | Kotlin Multiplatform (Android + iOS) |
| Database | Supabase PostgreSQL |
| DB Access | Supabase SQL migrations + supabase-kt |
| Authentication | Supabase Auth (Phone OTP) |
| Session Management | KSafe / KDataNest encrypted storage + moko-biometry |
| Real-time | Supabase Realtime (supabase-kt) |
| Maps | MapLibre Compose (cross-platform) |
| Push Notifications | KMPNotifier with Firebase FCM (cross-platform) |
| Geolocation | moko-geo |
| Contacts | Kontacts |
| Storage | Supabase Storage (for future attachments) |
| Backend | Supabase Edge Functions |

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│          CLIENT LAYER                  │
│                                       │
│  ┌────────────────┐  ┌──────────────┐│
│  │  KMP Android  │  │   KMP iOS    ││
│  │                │  │              ││
│  │  - Compose UI  │  │- Compose UI ││
│  │  - MapLibre    │  │- MapLibre   ││
│  │  - KSafe       │  │- KSafe      ││
│  │  - KMPNotifier     │  │- KMPNotifier│
│  │  - moko-geo    │  │- moko-geo   ││
│  │  - Kontacts    │  │- Kontacts   ││
│  │  - moko-biometry│ │- moko-biometry│
│  └────────┬───────┘  └──────┬───────┘│
│           │                  │        │
│           └───────┬──────────┘        │
│                   │                    │
│          ┌────────┴────────┐           │
│          │   COMMON CODE   │           │
│          │                 │           │
│          │ - Business Logic│           │
│          │ - Data Models   │           │
│          │ - Supabase-kt   │           │
│          │ - State Mgmt    │           │
│          └────────┬────────┘           │
└───────────────────┼────────────────────┘
                    │
        ┌───────────┴──────────┐
        │   HTTPS / WebSocket  │
        └───────────┬──────────┘
                    │
        ┌───────────┴──────────┐
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
  - DisplayName: string (shown to contacts for private events)
  - Device ID (unique, indexed)
  - FCM token (indexed)
  - Platform: enum (ANDROID, IOS)
  - isActive: boolean
  - createdAt: timestamp
  - updatedAt: timestamp
  - lastActiveAt: timestamp
- Constraint: One user per phone number
- Notes:
  - `displayName` is mutable; private event UI should resolve sender name from current Users row
  - One-device-per-phone behavior is enforced by rebinding `device_id` on login

### UserContacts Table
- Primary Key: UUID
- Fields:
  - User ID (foreign key)
  - Contact phone number (indexed)
  - Display name
  - hasApp: boolean
  - contactUserId: UUID (nullable, resolved app user cache)
  - createdAt: timestamp
  - updatedAt: timestamp
- Constraint: Unique combination of user and contact phone number
- Purpose: The list of private contacts for alert delivery
- Notes:
  - Contacts are stored by normalized phone number (not only user ID) so non-app contacts can exist in the same list
  - `contactUserId` is a cache/optimization and may be null if contact has not registered

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
  - IsAnonymous: boolean (typically true for public, false for private)
  - ExpiresAt: timestamp (createdAt + 48 hours)
  - CreatedAt: timestamp
  - deletedAt: timestamp (nullable, optional soft-delete/moderation)
- Indexes:
  - User ID
  - Location (PostGIS preferred; float lat/lon fallback acceptable for hackathon MVP)
  - Severity
  - BroadcastType
  - ExpiresAt
  - CreatedAt

### EventRecipients Table
- Primary Key: Composite (eventId, userId)
- Fields:
  - Event ID (foreign key)
  - User ID (foreign key, recipient)
  - deliveryStatus: enum/text (PENDING, SENT, FAILED)
  - NotifiedAt: timestamp
  - OpenedAt: timestamp (optional)
  - ClearedAt: timestamp (optional, when user clears from their list)
- Purpose: Track private recipients and delivery status
- Notes:
  - This table is used for private broadcasts only
  - `notifiedAt` must live here (recipient-specific), not on `Events`

### EventUserState Table (Optional for server-tracked public dismiss)
- Primary Key: Composite (eventId, userId)
- Fields:
  - Event ID (foreign key)
  - User ID (foreign key)
  - isDismissed: boolean
  - dismissedAt: timestamp (optional)
  - lastSeenAt: timestamp (optional)
- Purpose:
  - Optional table for persisting per-user state for public events
  - Can be deferred in MVP if public dismiss remains local/session-only

---

## Data Model Relationships

```
Users (1) ----< (N) UserContacts
Users (1) ----< (N) Events
Users (1) ----< (N) EventRecipients ----> (N) Events
Users (1) ----< (N) EventUserState ----> (N) Events   [optional]
```

---

## Authentication System Design

### Account Model

**One Device Per Account:**
- User account uniquely identified by phone number
- No support for multiple devices per phone number
- If login occurs from a new device, account is rebound to the new device ID (hackathon behavior)

### Authentication Flow

**Registration:**
1. User opens app, enters USA phone number
2. Phone number format validated (E.164 format for USA: +1XXXXXXXXXX)
3. Supabase Auth sends 6-digit OTP via SMS
4. User enters OTP code
5. Server validates code
6. User prompted to enter display name
7. Device ID generated from app/device context
8. Server creates or updates user record with phone number + device ID + displayName
9. Access token and refresh token generated
10. Tokens stored in encrypted key-value storage (KSafe / KDataNest)
11. User navigates to contact selection screen

**Subsequent App Opens:**
1. App checks for existing session in encrypted storage (KSafe / KDataNest)
2. If valid refresh token exists: Request biometric authentication
3. Biometric verified: New access token issued, refresh token remains
4. No OTP required
5. Navigate to main application

**Re-authentication Required When:**
- Refresh token expires (30 days)
- Device is factory reset
- App is reinstalled
- Encrypted storage data is cleared

### Session Management

| Token Type | Storage | Expiration |
|------------|---------|-----------|
| Access Token | Memory only | 15 minutes |
| Refresh Token | KSafe / KDataNest encrypted storage | 30 days |
| FCM Push Token | Supabase database | Until device uninstalls app |

### Biometric Authentication

- Required to unlock app and access features
- Required to create emergency events
- Falls back to device passcode if biometrics unavailable

### Security Measures (MVP)

- Phone OTP login via Supabase Auth
- Tokens encrypted in KSafe / KDataNest
- Device ID checked against current bound device on each session refresh
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

**Low-Power Mode:**
- Reduced update frequency while app remains open
- Accuracy target: Under 1 kilometer
- Update interval: 2-5 minutes
- Battery impact: Minimal

### Permission Handling

**Foreground Permission (Required):**
- Prompted on first location request
- Required for creating events
- Required for viewing nearby public events

**Background Permission:**
- Not used in MVP

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

**Public Event Delivery (MVP):**
- Public events are fetched by location query (simple lat/lon) on app open, map move, and manual refresh
- No precomputed public-recipient rows
- No public push notification fanout in MVP

### Message Flow

**Event Creation:**
1. User creates event with location, severity, category, broadcast type
2. Server validates event data
3. Event written to database with expires_at set to 48 hours from creation
4. Server routes based on broadcast type

**Private Broadcast:**
1. Query creator's UserContacts list
2. Match contact phone numbers to Users table
3. Create EventRecipient records for each matched user
4. Push notification sent via FCM to matched recipients
5. Real-time message sent to each recipient's private channel

**Public Broadcast:**
1. Store public event in Events table
2. Do not create EventRecipient rows
3. Clients query public events by current map bounds + 50-mile rule
4. Respect user public-alert opt-out in query filters

**Public Event Anonymity:**
- creator_id is stored in Events table for tracking and moderation
- Public broadcasts do not include creator's phone number or any identifying information
- Recipients see anonymous public event (no creator information)

**Private Event Identification:**
- Private events include creator's information to recipients
- Recipients can see which contact created the private alert

---

## Push Notification Design

### Notification Library

**KMPNotifier with Firebase FCM:**
- Single KMP library handling both Android and iOS
- Firebase bridges FCM to APNs for iOS
- Shared API reduces platform-specific code
- Built-in deep linking, permissions

### Notification Types

**Crisis Alert:**
- Vibration: Aggressive pattern
- Priority: High
- Actions: "View on Map"

**Alert (Warning):**
- Vibration: Standard pattern
- Priority: Normal
- Actions: "View on Map"

### Payload Structure

**Standard Alert Payload:**
- Title: Severity + Category (e.g., "CRISIS - Medical")
- Body: Brief description
- Icon: Severity-appropriate icon
- Data: event_id, coordinates, severity, deep link URL

**Deep Linking:**
- Custom URL scheme: `crimsoncode://event/{eventId}`
- Opens app, navigates to event on map

### Notification Channels (Android)

**Emergency Alerts:**
- Importance: High
- Bypass DND: Not guaranteed (platform-managed)

**Alerts:**
- Importance: Default

---

## Maps and Visualization Design

### Map Provider

**MapLibre Compose (Cross-Platform):**
- Open-source map SDK with unified API for Android and iOS
- Online maps using OpenStreetMap tiles (no API key required)
- Supports markers, user location, camera controls
- Active community, mature implementation

### Marker System

**Severity-Based Styling:**
- Crisis: Red circle with pulsing animation
- Alert: Orange circle with static styling

**Category Indicators:**
- Small icon overlay on marker indicating category
- Icons: Fire (flame), Medical (cross), Weather (cloud), etc.

**Clustering:**
- Map clustering for dense areas
- Cluster color: Maximum severity within cluster
- Cluster expands to show individual markers at maximum zoom

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
2. Contacts normalized to E.164 format
3. Contacts stored only in current user's UserContacts rows
4. `has_app` flag refreshed by matching against Users table

**App Detection:**
1. Phone numbers checked against Users table
2. Contacts with matching phone number marked has_app = true
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
- Event list toggle: Top-right button showing count of notified events
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
- If private: shows creator's display name from Users table (updates if sender changes name)
- If public: anonymous (no creator information)
- "Clear from list" button to remove from user's event list (uses local device cache to hide event, no database update)

**Event List View:**
- Accessed via top-right button on map
- Shows all private events user has received and nearby public events
- Tapping event zooms map to location
- "Clear all" button to remove all events from list (uses local device cache to hide events, no database update)

### Event Lifecycle

**Active State:**
- Default state for new events
- Marker displayed on map
- Event expires 48 hours after creation (automatic database cleanup)
- User can manually clear event from their list (uses local device cache to hide event, no database update)

**Cleared State:**
- Private events: User clears event from list, ClearedAt stored in EventRecipients
- Public events: User can dismiss in current session only (no server-side clear record)

**Expired State:**
- Automatic after 48 hours from creation
- Server removes from Events table and EventRecipients table
- No longer visible to any user

**Note:** Events are never resolved or marked as completed. Events only expire or are hidden locally by individual users.

---

## User Settings & Preferences

### Settings Storage: Local vs Database

**Stored in Database (Users table):**
- Phone number
- Display name (used for private event identification)
- Device ID
- FCM token
- Platform and device model
- Account status

**Stored Locally on Device:**
- Public alert opt-out preference
- Notification preferences (master toggle, crisis toggle, warning toggle, vibration)
- Location accuracy mode preference
- Biometric settings

### Notification Settings

**Push Notifications:**
- Master toggle: Enable/disable all notifications (local)
- Crisis alerts: Toggle (local)
- Warning alerts: Toggle (local)
- Public alerts: Toggle - user can opt-out of receiving all public alerts (local)
- Private alerts: Toggle (local)

**Notification Preferences:**
- Vibration toggle (local)

### Location Settings

**Location Accuracy:**
- High precision mode toggle (local)
- Default: Balanced mode (local)

**Background Location:**
- Not included in MVP

### Contact Settings

**Emergency Contacts:**
- Add/remove contacts
- Import from device contacts
- Manual entry option
- This is the single list that receives private alerts

### Account Settings

**Display Name:**
- Edit display name (stored in database)
- Shown to contacts for private events
- Required at registration
- Historical events update to reflect new name when changed

**Authentication:**
- Logout button
- Re-authentication required on next app open

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
- Indexes on lat/lon columns for radius queries
- Index on expires_at for automatic cleanup job

**Query Optimization:**
- Prepared statements for repeated queries
- Connection pooling via Supabase

---

## Security Architecture

### Data Security

**Encryption:**
- Tokens encrypted in KSafe / KDataNest
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

**Network Testing:**
- Intermittent/slow network conditions
- Connection recovery

---

## Deployment Strategy

### Production Environment

**Infrastructure:**
- Supabase Pro plan
- Supabase Auth (Phone OTP)
- Firebase FCM (free)
- MapLibre Compose (free, OpenStreetMap tiles)

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
| Supabase Auth SMS | Variable usage |
| Firebase FCM | $0 |
| MapLibre | $0 (OpenStreetMap tiles) |
| **Total** | **$25 + SMS usage** |

### Projected Annual

**Base:** $300/year + SMS usage

---

## Implementation Stages

### Stage 1: Foundation (Weeks 1-4)

**Deliverables:**
- Kotlin Multiplatform project initialized with Compose Multiplatform
- Supabase project created and configured
- SQL schema defined and initial migration applied
- Authentication flow (Supabase Auth phone OTP via supabase-kt)
- KSafe / KDataNest encrypted storage for secure token storage
- moko-biometry integration for biometric authentication flow
- Basic Compose UI shell with navigation
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
- Map integration via MapLibre Compose (cross-platform)
- moko-geo integration with permission handling
- User location marker with accuracy visualization
- Map interaction patterns
- Location fallback chain
- Accuracy display with color coding

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
- Public event query by map bounds + radius
- Private event recipient tracking
- Message delivery confirmation
- Connection management and reconnection

**Success Criteria:**
- Private contacts receive notifications
- Public events load for users within 50-mile radius
- Private delivery status tracked

---

### Stage 6: Push Notifications (Weeks 13-14)

**Deliverables:**
- KMPNotifier integration with Firebase FCM (cross-platform)
- Device token registration
- Push notification payload construction
- Severity-based notification styling
- Deep link handling from notifications
- Notification channel configuration (Android)

**Success Criteria:**
- Users receive push notifications for events
- Tapping notification opens app to event on map
- Notification behavior is consistent on iOS and Android

---

### Stage 7: Event Display & Interaction (Weeks 15-16)

**Deliverables:**
- Event marker display on map
- Severity and category visualization
- Marker clustering
- Event details bottom panel
- Event list view with filtering
- Map navigation to event
- Clear from list functionality (local device cache to hide events, no database update)
- Private events show creator's display name, public events anonymous

**Success Criteria:**
- Events display with appropriate styling
- Users can view details and navigate to location
- Users can clear events from their list (using local device cache, no database update)

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

### Stage 9: Reliability & Network Resilience (Week 18)

**Deliverables:**
- Retry handling for event creation and acknowledgments
- Network status monitoring
- Error-state UI for failed network operations

**Success Criteria:**
- Temporary network failures are handled gracefully
- Users get clear feedback when actions fail or recover

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

**Platforms:** Android, iOS (Kotlin Multiplatform / Compose Multiplatform)
**Region:** USA only
**Language:** English only
**Account Model:** One device per phone number
**Event Expiration:** 48 hours automatic
**Broadcast Radius:** 50 miles for public alerts
**Event Resolution:** Not supported (events only hidden locally by users or expire)
**Public Events:** Anonymous (no creator information shown)
**Private Events:** Shows creator's display name to recipients
**Contacts:** Single list for private alert delivery
**Private Delivery Tracking:** Stored per recipient in `EventRecipients`
**Public Dismiss Persistence:** Optional (`EventUserState`), local/session-only by default in MVP
**Offline Support:** Not included in MVP
**Description Limit:** 500 characters
**Opt-out:** Users can opt-out of public alerts (local device setting)
**License:** MIT
