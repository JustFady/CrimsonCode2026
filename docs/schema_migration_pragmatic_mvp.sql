BEGIN;

-- 1) USERS: add practical MVP profile/session fields
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS display_name VARCHAR(100),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP WITHOUT TIME ZONE;

-- Backfill updated_at for existing rows if null
UPDATE public.users
SET updated_at = COALESCE(updated_at, created_at, now())
WHERE updated_at IS NULL;

-- 2) USER_CONTACTS: move toward phone-based contacts without destructive rewrite
-- Current schema uses contact_id. Keep data, add the new columns and backfill what we can.
ALTER TABLE public.user_contacts
  ADD COLUMN IF NOT EXISTS id UUID DEFAULT gen_random_uuid(),
  ADD COLUMN IF NOT EXISTS contact_phone_number VARCHAR(20),
  ADD COLUMN IF NOT EXISTS contact_user_id UUID,
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now();

-- If old contact_id exists, copy it into contact_user_id (best-effort migration)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'user_contacts' AND column_name = 'contact_id'
  ) THEN
    EXECUTE 'UPDATE public.user_contacts SET contact_user_id = COALESCE(contact_user_id, contact_id)';
  END IF;
END $$;

-- Backfill phone numbers for contacts that are already app users
UPDATE public.user_contacts uc
SET contact_phone_number = u.phone_number
FROM public.users u
WHERE uc.contact_phone_number IS NULL
  AND uc.contact_user_id = u.id;

-- 3) EVENTS: remove recipient-specific column from event rows; add optional location/deletion fields
ALTER TABLE public.events
  DROP COLUMN IF EXISTS notified_at,
  ADD COLUMN IF NOT EXISTS location_override VARCHAR(255),
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

-- 4) EVENT_RECIPIENTS: private-delivery tracking (idempotent create)
CREATE TABLE IF NOT EXISTS public.event_recipients (
  event_id UUID NOT NULL,
  user_id UUID NOT NULL,
  delivery_status VARCHAR(20) DEFAULT 'PENDING',
  notified_at TIMESTAMP WITHOUT TIME ZONE,
  opened_at TIMESTAMP WITHOUT TIME ZONE,
  cleared_at TIMESTAMP WITHOUT TIME ZONE,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  CONSTRAINT event_recipients_pkey PRIMARY KEY (event_id, user_id),
  CONSTRAINT event_recipients_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE,
  CONSTRAINT event_recipients_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
  CONSTRAINT event_recipients_delivery_status_check CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);

-- 5) Foreign keys for app tables (best-effort, only if missing)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'events_creator_id_fkey'
  ) THEN
    ALTER TABLE public.events
      ADD CONSTRAINT events_creator_id_fkey
      FOREIGN KEY (creator_id) REFERENCES public.users(id) ON DELETE SET NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'user_contacts_user_id_fkey'
  ) THEN
    ALTER TABLE public.user_contacts
      ADD CONSTRAINT user_contacts_user_id_fkey
      FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'user_contacts_contact_user_id_fkey'
  ) THEN
    ALTER TABLE public.user_contacts
      ADD CONSTRAINT user_contacts_contact_user_id_fkey
      FOREIGN KEY (contact_user_id) REFERENCES public.users(id) ON DELETE SET NULL;
  END IF;
END $$;

-- 6) Indexes for common queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_users_fcm_token ON public.users (fcm_token);
CREATE INDEX IF NOT EXISTS idx_users_last_active_at ON public.users (last_active_at);

CREATE INDEX IF NOT EXISTS idx_user_contacts_user_id ON public.user_contacts (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contacts_contact_phone_number ON public.user_contacts (contact_phone_number);
CREATE INDEX IF NOT EXISTS idx_user_contacts_contact_user_id ON public.user_contacts (contact_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_contacts_user_phone_unique
  ON public.user_contacts (user_id, contact_phone_number)
  WHERE contact_phone_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_events_creator_id ON public.events (creator_id);
CREATE INDEX IF NOT EXISTS idx_events_broadcast_type ON public.events (broadcast_type);
CREATE INDEX IF NOT EXISTS idx_events_severity ON public.events (severity);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON public.events (created_at);
CREATE INDEX IF NOT EXISTS idx_events_expires_at ON public.events (expires_at);

CREATE INDEX IF NOT EXISTS idx_event_recipients_user_id ON public.event_recipients (user_id);
CREATE INDEX IF NOT EXISTS idx_event_recipients_user_cleared_at ON public.event_recipients (user_id, cleared_at);

-- 7) Lightweight checks for data quality (only if absent)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'events_severity_check'
  ) THEN
    ALTER TABLE public.events
      ADD CONSTRAINT events_severity_check CHECK (severity IN ('ALERT', 'CRISIS'));
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'events_broadcast_type_check'
  ) THEN
    ALTER TABLE public.events
      ADD CONSTRAINT events_broadcast_type_check CHECK (broadcast_type IN ('PUBLIC', 'PRIVATE'));
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'events_description_len_check'
  ) THEN
    ALTER TABLE public.events
      ADD CONSTRAINT events_description_len_check CHECK (char_length(description) <= 500);
  END IF;
END $$;

COMMIT;
