-- Migration: Create event_recipients table
-- Description: Creates public.event_recipients table for per-recipient tracking of private events only
--              Tracks delivery, open, and clear state per recipient

BEGIN;

-- Create event_recipients table
CREATE TABLE IF NOT EXISTS public.event_recipients (
  event_id UUID NOT NULL,
  user_id UUID NOT NULL,
  delivery_status VARCHAR(20) DEFAULT 'PENDING',
  notified_at TIMESTAMP WITHOUT TIME ZONE,
  opened_at TIMESTAMP WITHOUT TIME ZONE,
  cleared_at TIMESTAMP WITHOUT TIME ZONE,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Composite primary key
ALTER TABLE public.event_recipients
  ADD CONSTRAINT event_recipients_pkey
  PRIMARY KEY (event_id, user_id);

-- Foreign key constraints
ALTER TABLE public.event_recipients
  ADD CONSTRAINT event_recipients_event_id_fkey
  FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE;

ALTER TABLE public.event_recipients
  ADD CONSTRAINT event_recipients_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- Check constraint for delivery status
ALTER TABLE public.event_recipients
  ADD CONSTRAINT event_recipients_delivery_status_check
  CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'));

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_event_recipients_user_id ON public.event_recipients (user_id);
CREATE INDEX IF NOT EXISTS idx_event_recipients_delivery_status ON public.event_recipients (delivery_status);
CREATE INDEX IF NOT EXISTS idx_event_recipients_notified_at ON public.event_recipients (notified_at);
CREATE INDEX IF NOT EXISTS idx_event_recipients_user_cleared_at ON public.event_recipients (user_id, cleared_at);

-- Create trigger to auto-update updated_at timestamp
CREATE TRIGGER event_recipients_updated_at
  BEFORE UPDATE ON public.event_recipients
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMIT;
