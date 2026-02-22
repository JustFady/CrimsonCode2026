-- Migration: Enable Row Level Security (RLS) on user_contacts table
-- Description: Enables RLS and creates policies to restrict access to user's own contacts
--              Service role (used in Edge Functions) has full access

BEGIN;

-- Enable RLS on user_contacts table
ALTER TABLE public.user_contacts ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own contacts
CREATE POLICY user_contacts_select_own ON public.user_contacts
  FOR SELECT
  USING (auth.uid() = user_id);

-- Policy: Users can insert their own contacts
CREATE POLICY user_contacts_insert_own ON public.user_contacts
  FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own contacts
CREATE POLICY user_contacts_update_own ON public.user_contacts
  FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can delete their own contacts
CREATE POLICY user_contacts_delete_own ON public.user_contacts
  FOR DELETE
  USING (auth.uid() = user_id);

-- Policy: Service role has full access (used by Edge Functions for contact matching)
CREATE POLICY user_contacts_service_role_full ON public.user_contacts
  FOR ALL
  USING (auth.role() = 'service_role')
  WITH CHECK (auth.role() = 'service_role');

COMMIT;
