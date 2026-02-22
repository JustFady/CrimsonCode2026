-- 1. CLEANUP
DROP TABLE IF EXISTS event_recipients CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS user_contacts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 2. USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY, -- Links to Supabase Auth ID
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    device_id VARCHAR(255) UNIQUE NOT NULL,
    fcm_token VARCHAR(255),
    platform VARCHAR(10), -- 'ANDROID' or 'IOS'
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);

-- 3. CONTACTS TABLE
CREATE TABLE user_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    contact_phone_number VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    has_app BOOLEAN DEFAULT false
);

-- 4. EVENTS TABLE (The main alert table)
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES users(id),
    severity VARCHAR(20),  -- 'ALERT' or 'CRISIS'
    category VARCHAR(50),  -- 'MEDICAL', 'FIRE', etc.
    lat FLOAT NOT NULL,
    lon FLOAT NOT NULL,
    broadcast_type VARCHAR(20), -- 'PUBLIC' or 'PRIVATE'
    description VARCHAR(500),
    is_anonymous BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT now(),
    expires_at TIMESTAMP DEFAULT (now() + interval '48 hours')
);

-- 5. RECIPIENTS TABLE (Tracking who got the alert)
CREATE TABLE event_recipients (
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    notified_at TIMESTAMP DEFAULT now(),
    acknowledged_at TIMESTAMP,
    PRIMARY KEY (event_id, user_id)
);