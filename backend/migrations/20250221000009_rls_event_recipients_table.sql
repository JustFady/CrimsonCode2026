-- Migration: Enable Row Level Security (RLS) on event_recipients table
-- Description: Enables RLS and creates policies for private event recipient tracking
--              Recipients can view/update their own delivery state
--              Service role has full access

BEGIN;

-- Enable RLS on event_recipients table
ALTER TABLE public.event_recipients ENABLE ROW LEVEL SECURITY;

-- Policy: Recipients can view their own event recipient records
CREATE POLICY event_recipients_select_own ON public.event_recipients
  FOR SELECT
  USING (auth.uid() = user_id);

-- Policy: Recipients can update their own delivery state (opened, cleared, etc.)
CREATE POLICY event_recipients_update_own ON public.event_recipients
  FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can insert recipient records for their own events (via service role, but policy here for completeness)
CREATE POLICY event_recipients_insert_event_creator ON public.event_recipients
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.events
      WHERE events.id = event_recipients.event_id
      AND events.creator_id = auth.uid()
    )
  );

-- Policy: Service role has full access (used by Edge Functions for notification delivery)
CREATE POLICY event_recipients_service_role_full ON public.event_recipients
  FOR ALL
  USING (auth.role() = 'service_role')
  WITH CHECK (auth.role() = 'service_role');

COMMIT;
