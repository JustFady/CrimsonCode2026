-- Migration: Create users table
-- Description: Creates public.users table to store app user profiles linked to Supabase Auth
--              Enforces one-device-per-phone behavior and stores FCM token for push notifications

BEGIN;

-- Create users table in public schema
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone_number VARCHAR(20) NOT NULL UNIQUE,
  display_name VARCHAR(100),
  device_id VARCHAR(255) UNIQUE,
  fcm_token VARCHAR(255),
  platform VARCHAR(10) CHECK (platform IN ('ANDROID', 'IOS')),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  last_active_at TIMESTAMP WITHOUT TIME ZONE
);

-- Add constraint to ensure USA phone numbers follow E.164 format (lightweight guard)
-- App should still normalize/validate, this is a database-level check
ALTER TABLE public.users
  ADD CONSTRAINT users_phone_format_check
  CHECK (phone_number ~ '^\+1[0-9]{10}$');

-- Create indexes for common queries
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_number ON public.users (phone_number);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_device_id ON public.users (device_id) WHERE device_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_fcm_token ON public.users (fcm_token) WHERE fcm_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_last_active_at ON public.users (last_active_at);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON public.users (is_active);

-- Create trigger to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at
  BEFORE UPDATE ON public.users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMIT;
