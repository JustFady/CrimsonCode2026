-- Migration: Create user_contacts table
-- Description: Creates public.user_contacts table to store each user's private emergency contact list
--              Supports both app and non-app contacts via phone number matching

BEGIN;

-- Create user_contacts table
CREATE TABLE IF NOT EXISTS public.user_contacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  contact_phone_number VARCHAR(20),
  display_name VARCHAR(100),
  has_app BOOLEAN DEFAULT false,
  contact_user_id UUID,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Add foreign key constraints
ALTER TABLE public.user_contacts
  ADD CONSTRAINT user_contacts_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.user_contacts
  ADD CONSTRAINT user_contacts_contact_user_id_fkey
  FOREIGN KEY (contact_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

-- Add unique constraint: each user can have only one entry per contact phone number
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_contacts_user_phone_unique
  ON public.user_contacts (user_id, contact_phone_number)
  WHERE contact_phone_number IS NOT NULL;

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_user_contacts_user_id ON public.user_contacts (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contacts_contact_phone_number ON public.user_contacts (contact_phone_number) WHERE contact_phone_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_contacts_contact_user_id ON public.user_contacts (contact_user_id) WHERE contact_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_contacts_has_app ON public.user_contacts (has_app);

-- Create trigger to auto-update updated_at timestamp
CREATE TRIGGER user_contacts_updated_at
  BEFORE UPDATE ON public.user_contacts
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMIT;
