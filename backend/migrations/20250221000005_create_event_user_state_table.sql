-- Migration: Create event_user_state table (Optional for MVP)
-- Description: Creates public.event_user_state table for persisting per-user state for public events
--              Optional table - can be skipped if public dismiss remains local/session-only
--              Used to prevent repeated resurfacing of dismissed public events across app restarts

BEGIN;

-- Create event_user_state table
CREATE TABLE IF NOT EXISTS public.event_user_state (
  event_id UUID NOT NULL,
  user_id UUID NOT NULL,
  is_dismissed BOOLEAN DEFAULT false,
  dismissed_at TIMESTAMP WITHOUT TIME ZONE,
  last_seen_at TIMESTAMP WITHOUT TIME ZONE
);

-- Composite primary key
ALTER TABLE public.event_user_state
  ADD CONSTRAINT event_user_state_pkey
  PRIMARY KEY (event_id, user_id);

-- Foreign key constraints
ALTER TABLE public.event_user_state
  ADD CONSTRAINT event_user_state_event_id_fkey
  FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;

ALTER TABLE public.event_user_state
  ADD CONSTRAINT event_user_state_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_event_user_state_user_id ON public.event_user_state (user_id);
CREATE INDEX IF NOT EXISTS idx_event_user_state_is_dismissed ON public.event_user_state (is_dismissed);
CREATE INDEX IF NOT EXISTS idx_event_user_state_dismissed_at ON public.event_user_state (dismissed_at);

COMMIT;
