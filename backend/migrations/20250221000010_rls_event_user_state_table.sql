-- Migration: Enable Row Level Security (RLS) on event_user_state table (Optional MVP)
-- Description: Enables RLS and creates policies for public event user state
--              Users can view/update their own public event dismiss state
--              Service role has full access
--              This table is optional for MVP - can be skipped if public dismiss remains local/session-only

BEGIN;

-- Enable RLS on event_user_state table
ALTER TABLE public.event_user_state ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own public event state
CREATE POLICY event_user_state_select_own ON public.event_user_state
  FOR SELECT
  USING (auth.uid() = user_id);

-- Policy: Users can insert their own public event state (when dismissing an event)
CREATE POLICY event_user_state_insert_own ON public.event_user_state
  FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own public event state
CREATE POLICY event_user_state_update_own ON public.event_user_state
  FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can delete their own public event state
CREATE POLICY event_user_state_delete_own ON public.event_user_state
  FOR DELETE
  USING (auth.uid() = user_id);

-- Policy: Service role has full access (used by Edge Functions)
CREATE POLICY event_user_state_service_role_full ON public.event_user_state
  FOR ALL
  USING (auth.role() = 'service_role')
  WITH CHECK (auth.role() = 'service_role');

COMMIT;
