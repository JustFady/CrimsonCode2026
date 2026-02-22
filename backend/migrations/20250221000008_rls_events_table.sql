-- Migration: Enable Row Level Security (RLS) on events table
-- Description: Enables RLS and creates policies for public/private event access control
--              Public events: all authenticated users can view active events
--              Private events: only creator and recipients can view
--              Service role has full access

BEGIN;

-- Enable RLS on events table
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view active public events
CREATE POLICY events_select_public ON public.events
  FOR SELECT
  USING (
    broadcast_type = 'PUBLIC'
    AND deleted_at IS NULL
    AND expires_at > now()
  );

-- Policy: Users can view events they created
CREATE POLICY events_select_own ON public.events
  FOR SELECT
  USING (auth.uid() = creator_id);

-- Policy: Users can view private events they are recipients of
CREATE POLICY events_select_private_recipient ON public.events
  FOR SELECT
  USING (
    broadcast_type = 'PRIVATE'
    AND deleted_at IS NULL
    AND EXISTS (
      SELECT 1 FROM public.event_recipients
      WHERE event_recipients.event_id = events.id
      AND event_recipients.user_id = auth.uid()
    )
  );

-- Policy: Users can create events
CREATE POLICY events_insert_own ON public.events
  FOR INSERT
  WITH CHECK (auth.uid() = creator_id);

-- Policy: Users can update events they created
CREATE POLICY events_update_own ON public.events
  FOR UPDATE
  USING (auth.uid() = creator_id)
  WITH CHECK (auth.uid() = creator_id);

-- Policy: Users can delete events they created (soft delete)
CREATE POLICY events_delete_own ON public.events
  FOR DELETE
  USING (auth.uid() = creator_id);

-- Policy: Service role has full access (used by Edge Functions)
CREATE POLICY events_service_role_full ON public.events
  FOR ALL
  USING (auth.role() = 'service_role')
  WITH CHECK (auth.role() = 'service_role');

COMMIT;
