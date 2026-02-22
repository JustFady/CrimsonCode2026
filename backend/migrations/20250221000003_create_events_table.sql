-- Migration: Create events table
-- Description: Creates public.events table as canonical record of emergency alerts
--              Supports both public and private alerts with 48-hour expiration

BEGIN;

-- Create events table
CREATE TABLE IF NOT EXISTS public.events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  creator_id UUID NOT NULL,
  severity VARCHAR(20) NOT NULL,
  category VARCHAR(50) NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  location_override VARCHAR(255),
  broadcast_type VARCHAR(20) NOT NULL,
  description VARCHAR(500),
  is_anonymous BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITHOUT TIME ZONE
);

-- Add foreign key constraint
ALTER TABLE public.events
  ADD CONSTRAINT events_creator_id_fkey
  FOREIGN KEY (creator_id) REFERENCES public.users(id) ON DELETE SET NULL;

-- Add check constraints for data quality
ALTER TABLE public.events
  ADD CONSTRAINT events_severity_check
  CHECK (severity IN ('ALERT', 'CRISIS'));

ALTER TABLE public.events
  ADD CONSTRAINT events_broadcast_type_check
  CHECK (broadcast_type IN ('PUBLIC', 'PRIVATE'));

ALTER TABLE public.events
  ADD CONSTRAINT events_description_len_check
  CHECK (char_length(description) <= 500);

ALTER TABLE public.events
  ADD CONSTRAINT events_expires_at_after_created_check
  CHECK (expires_at > created_at);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_events_creator_id ON public.events (creator_id);
CREATE INDEX IF NOT EXISTS idx_events_broadcast_type ON public.events (broadcast_type);
CREATE INDEX IF NOT EXISTS idx_events_severity ON public.events (severity);
CREATE INDEX IF NOT EXISTS idx_events_category ON public.events (category);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON public.events (created_at);
CREATE INDEX IF NOT EXISTS idx_events_expires_at ON public.events (expires_at);
CREATE INDEX IF NOT EXISTS idx_events_lat_lon ON public.events (lat, lon);
CREATE INDEX IF NOT EXISTS idx_events_deleted_at ON public.events (deleted_at) WHERE deleted_at IS NOT NULL;

-- Create partial index for active (non-deleted, non-expired) events
CREATE INDEX IF NOT EXISTS idx_events_active ON public.events (created_at, expires_at)
  WHERE deleted_at IS NULL AND expires_at > now();

COMMIT;
