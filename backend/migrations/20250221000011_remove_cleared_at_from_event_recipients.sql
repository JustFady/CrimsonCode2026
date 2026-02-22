BEGIN;

DROP INDEX IF EXISTS public.idx_event_recipients_user_cleared_at;

ALTER TABLE public.event_recipients DROP COLUMN IF EXISTS cleared_at;

COMMIT;
