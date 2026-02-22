# Emergency Response App - Database Schema Reference

**Project Stage:** Hackathon MVP (iterating, spec may evolve)
**Database:** Supabase PostgreSQL
**Modeling Principle:** Separate event facts from per-user delivery/view state
**Retention:** Events expire after 48 hours (MVP)

---

## 1. Design Principles (Why This Schema)

### 1. `events` stores what happened
An event is the core fact: who created it, what happened, where, how severe, and when it expires.

### 2. Recipient state is separate
Notification delivery, opened state, and clear/dismiss actions are user-specific. Those do not belong on the event row.

### 3. Contacts are stored by phone number
Private alerts depend on a user's contact list, and many contacts will not have the app yet. We store normalized phone numbers first and resolve to app users when possible.

### 4. Public and private flows share the same `events` table
`broadcast_type` determines delivery behavior:
- `PUBLIC`: queried by location
- `PRIVATE`: delivered to matched contacts via `event_recipients`

---

## 2. Core Tables (MVP)

### `users`
Represents the app user profile tied to Supabase Auth and one active device.

Why we need it:
- Stores app-specific profile data not handled by `auth.users`
- Enforces one-device-per-phone behavior
- Stores FCM token for push notifications

Recommended columns:
| Column | Type | Why it exists |
| :--- | :--- | :--- |
| `id` | `UUID` PK | Matches `auth.users.id` and app identity |
| `phone_number` | `VARCHAR(20)` unique | Primary account identity (E.164) |
| `display_name` | `VARCHAR(100)` | Shown to private recipients |
| `device_id` | `VARCHAR(255)` unique | One-device-per-account binding |
| `fcm_token` | `VARCHAR(255)` nullable | Push notification targeting |
| `platform` | `VARCHAR(10)` | Android/iOS-specific behavior/debugging |
| `is_active` | `BOOLEAN` | Soft-disable account if needed |
| `created_at` | `TIMESTAMP` | Audit trail |
| `updated_at` | `TIMESTAMP` | Audit trail |
| `last_active_at` | `TIMESTAMP` | Activity tracking and cleanup |

Notes:
- `display_name` is mutable; private event views should resolve the current name from `users`.

### `user_contacts`
Each user's private emergency contact list.

Why we need it:
- Defines the target audience for private alerts
- Supports both app and non-app contacts
- Enables `has_app` UI badges and matching optimization

Recommended columns:
| Column | Type | Why it exists |
| :--- | :--- | :--- |
| `id` | `UUID` PK | Stable row identifier |
| `user_id` | `UUID` FK -> `users.id` | Contact-list owner |
| `contact_phone_number` | `VARCHAR(20)` | Canonical routing key for matching |
| `display_name` | `VARCHAR(100)` | User-local contact label |
| `has_app` | `BOOLEAN` | Cached UI indicator |
| `contact_user_id` | `UUID` nullable FK -> `users.id` | Cached match when contact has registered |
| `created_at` | `TIMESTAMP` | Audit trail |
| `updated_at` | `TIMESTAMP` | Audit trail |

Required constraint:
- Unique `(user_id, contact_phone_number)`

Why not `contact_id` only:
- Non-app contacts would have no ID in `users`
- Phone number matching is needed for onboarding and backfill when contacts later join

### `events`
The canonical record of emergency alerts.

Why we need it:
- Single source of truth for event metadata
- Supports both public and private alerts
- Powers map display, event list, and expiration lifecycle

Recommended columns:
| Column | Type | Why it exists |
| :--- | :--- | :--- |
| `id` | `UUID` PK | Event identity |
| `creator_id` | `UUID` FK -> `users.id` | Creator reference |
| `severity` | `VARCHAR(20)` | Alert vs crisis behavior/UI |
| `category` | `VARCHAR(50)` | Marker icon/color + filtering |
| `lat` | `DOUBLE PRECISION` | Map placement |
| `lon` | `DOUBLE PRECISION` | Map placement |
| `location_override` | `VARCHAR(255)` nullable | Manual location text |
| `broadcast_type` | `VARCHAR(20)` | Public/private routing |
| `description` | `VARCHAR(500)` | Human-readable details |
| `is_anonymous` | `BOOLEAN` | Hide creator identity for public alerts |
| `created_at` | `TIMESTAMP` | Event time |
| `expires_at` | `TIMESTAMP` | Auto-expiration cutoff |
| `deleted_at` | `TIMESTAMP` nullable | Optional moderation/soft delete |

Important modeling rule:
- Do **not** store `notified_at` on `events` because notifications are recipient-specific.

### `event_recipients` (Private delivery state)
Per-recipient tracking for private events only.

Why we need it:
- A private event can have many recipients
- Delivery/open/clear state differs per recipient
- Powers the recipient’s “my private events” list and delivery debugging

Recommended columns:
| Column | Type | Why it exists |
| :--- | :--- | :--- |
| `event_id` | `UUID` FK -> `events.id` | Which event |
| `user_id` | `UUID` FK -> `users.id` | Which recipient |
| `delivery_status` | `VARCHAR(20)` | `PENDING/SENT/FAILED` delivery tracking |
| `notified_at` | `TIMESTAMP` nullable | Push or realtime sent time |
| `opened_at` | `TIMESTAMP` nullable | User opened event |
| `cleared_at` | `TIMESTAMP` nullable | User cleared from list |

Primary key:
- `(event_id, user_id)`

---

## 3. Optional Table (Add When Needed)

### `event_user_state` (Public dismiss persistence)
Stores per-user state for public events if session-only dismiss is not enough.

Why we might need it:
- Persist public event dismiss/seen state across app restarts and devices
- Prevent repeated resurfacing of dismissed public events

Recommended columns:
| Column | Type | Why it exists |
| :--- | :--- | :--- |
| `event_id` | `UUID` FK -> `events.id` | Which public event |
| `user_id` | `UUID` FK -> `users.id` | Which user |
| `is_dismissed` | `BOOLEAN` | Hide from list/map logic |
| `dismissed_at` | `TIMESTAMP` nullable | Audit/debugging |
| `last_seen_at` | `TIMESTAMP` nullable | UX tuning/debugging |

Primary key:
- `(event_id, user_id)`

MVP note:
- Can be skipped initially if public dismiss remains local/session-only.

---

## 4. Relationships

```text
users (1) -> (N) user_contacts
users (1) -> (N) events                [creator_id]
events (1) -> (N) event_recipients
users (1) -> (N) event_recipients      [private recipients]
events (1) -> (N) event_user_state     [optional]
users (1) -> (N) event_user_state      [optional]
```

---

## 5. Constraints and Checks (Recommended)

Use `CHECK` constraints for fast iteration (hackathon-friendly) instead of PostgreSQL enums initially.

Examples:
- `severity IN ('ALERT', 'CRISIS')`
- `broadcast_type IN ('PUBLIC', 'PRIVATE')`
- `char_length(description) <= 500`
- `expires_at > created_at`
- `phone_number LIKE '+1%'` (lightweight guard, app should still normalize/validate)

---

## 6. Indexes (MVP)

### `users`
- Unique: `phone_number`
- Unique: `device_id`
- Index: `fcm_token` (optional but useful for backend send jobs)

### `user_contacts`
- Unique: `(user_id, contact_phone_number)`
- Index: `contact_phone_number`
- Index: `contact_user_id`

### `events`
- Index: `creator_id`
- Index: `broadcast_type`
- Index: `severity`
- Index: `created_at`
- Index: `expires_at`
- Geo index preferred (PostGIS) if/when moving beyond simple float bounding-box queries

### `event_recipients`
- PK `(event_id, user_id)` covers event lookup
- Add index on `user_id` for recipient event-list queries
- Add index on `(user_id, cleared_at)` if “active private events” query is frequent

---

## 7. Query Rules (Application Behavior)

- Always filter active events with `WHERE expires_at > now()`
- Public events: query by map bounds + radius rule (MVP can use bounding box approximation)
- Private events: join `events` + `event_recipients` for current `user_id`
- Hide creator identity for public events when `is_anonymous = true`
- Treat `event_recipients.cleared_at IS NULL` as active in recipient lists

---

## 8. Security / Supabase Notes

- Enable RLS on all `public.*` app tables (`users`, `user_contacts`, `events`, `event_recipients`, optional `event_user_state`)
- Write explicit policies per table (owner-only for contacts/users, recipient/creator access for private event rows)
- Never expose service-role/secret keys in mobile apps

---

## 9. Migration Priorities From Current Schema

If upgrading the current schema shown in the Supabase dump:
1. Add `event_recipients`
2. Remove `events.notified_at`
3. Replace `user_contacts.contact_id` with `contact_phone_number` (or add phone column first and backfill)
4. Add `users.display_name`, `users.updated_at`, `users.last_active_at`
5. Add foreign keys, indexes, and RLS policies
