# Emergency Response App - Database Schema Reference

**Project Stage:** 24-Hour Hackathon Build
**Architecture:** Supabase PostgreSQL
**Geolocation:** Simplified Float (Lat/Lon)
**Retention:** 48-Hour Auto-Expiration

---

## 1. Tables Overview

### `users`
Binds phone numbers to specific hardware devices.
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary Key (Matches `auth.users.id`) |
| `phone_number` | `VARCHAR` | Unique E.164 number (+1...) |
| `display_name` | `VARCHAR` | Display name shown to contacts for private events |
| `device_id` | `VARCHAR` | Unique Hardware ID |
| `device_model` | `VARCHAR` | Device model string |
| `fcm_token` | `VARCHAR` | Firebase Push Token |
| `platform` | `VARCHAR` | 'ANDROID' or 'IOS' |
| `is_active` | `BOOLEAN` | Account status |
| `created_at` | `TIMESTAMP` | Record creation time |
| `last_active_at` | `TIMESTAMP` | Last active timestamp |

**Constraints:**
- One user per phone number (unique on `phone_number`)

### `user_contacts`
Private list for targeted alert delivery.
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary Key |
| `user_id` | `UUID` | FK to `users.id` |
| `contact_phone_number`| `VARCHAR` | Recipient phone number |
| `display_name` | `VARCHAR` | Contact name from device |
| `has_app` | `BOOLEAN` | True if contact is a registered user |
| `added_at` | `TIMESTAMP` | When contact was added |

**Constraints:**
- Unique combination of `user_id` and `contact_phone_number`

### `events`
Active emergency alerts.
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary Key |
| `creator_id` | `UUID` | FK to `users.id` |
| `severity` | `VARCHAR` | 'ALERT' or 'CRISIS' |
| `category` | `VARCHAR` | 'MEDICAL', 'FIRE', 'CRIME', etc. |
| `lat` | `FLOAT` | Latitude |
| `lon` | `FLOAT` | Longitude |
| `location_override` | `VARCHAR` | Optional, for manual location entry |
| `broadcast_type` | `VARCHAR` | 'PUBLIC' or 'PRIVATE' |
| `description` | `VARCHAR` | Max 500 characters |
| `is_anonymous` | `BOOLEAN` | Hide creator info in Public view |
| `created_at` | `TIMESTAMP` | Event timestamp |
| `expires_at` | `TIMESTAMP` | `created_at` + 48 hours |

**Indexes:**
- `creator_id`
- `lat`, `lon` (for radius queries)
- `severity`
- `broadcast_type`
- `expires_at`
- `created_at`

### `event_recipients`
Tracking for private alert delivery.
| Column | Type | Description |
| :--- | :--- | :--- |
| `event_id` | `UUID` | FK to `events.id` |
| `user_id` | `UUID` | FK to `users.id` (Recipient) |
| `notified_at` | `TIMESTAMP` | Time alert was sent |

**Constraints:**
- Primary Key: Composite (`event_id`, `user_id`)

---

## 2. Implementation Rules

* **Radius Query:** For 50-mile radius, use ±0.7 degrees Lat/Lon offset in `SELECT` queries.
* **Realtime:** Listen to `events` table changes via Supabase Realtime for map updates.
* **Expiration:** Always filter using `WHERE expires_at > now()`.
* **Privacy:** If `is_anonymous` is `TRUE`, the frontend MUST hide `creator_id` info.
