# Edge Functions

Supabase Edge Functions for the Emergency Response App.

## Functions Directory Structure

Each function should be in its own subdirectory:

```
functions/
├── deliver-private-event/
│   └── index.ts
├── send-push-notification/
│   └── index.ts
└── resolve-contacts/
    └── index.ts
```

## Available Functions

Functions will be added as the project progresses:

- **deliver-private-event**: Routes private events to matched contacts via `event_recipients`
- **send-push-notification**: Sends FCM push notifications to recipients
- **resolve-contacts**: Matches phone numbers to app users for contact lists

## Development

To deploy a function:

```bash
supabase functions deploy <function-name>
```

To invoke locally:

```bash
supabase functions serve <function-name>
```

## Environment Access

Edge Functions have access to:
- `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` automatically
- Custom secrets via `supabase secrets set`

## Security

- Edge Functions use service role key for admin operations
- All database operations respect RLS policies when using client
- Functions validate caller identity via `auth.uid()`
