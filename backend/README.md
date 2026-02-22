# Backend - Supabase Infrastructure

This directory contains the Supabase backend infrastructure for the CrimsonCode2026 Emergency Response App.

## Directory Structure

```
backend/
├── config/           # Supabase configuration files
├── migrations/       # SQL database migrations
└── functions/        # Supabase Edge Functions
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Required variables:
- `SUPABASE_URL`: Your Supabase project URL
- `SUPABASE_ANON_KEY`: Public key for mobile app
- `SUPABASE_SERVICE_ROLE_KEY`: Admin key for Edge Functions
- `FCM_SERVER_KEY`: Firebase Cloud Messaging key for push notifications

### Supabase Setup

1. Create a Supabase project at https://supabase.com
2. Enable Phone Auth in Auth -> Providers
3. Configure Twilio Verify for SMS OTP
4. Copy credentials to `.env`

## Migrations

Database schema changes go in `migrations/`. Each migration should:
- Be idempotent (safe to run multiple times)
- Include comments explaining changes
- Follow the naming convention: `YYYYMMDDHHMMSS_description.sql`

Migration file: `migrations/20250221000000_schema_migration_pragmatic_mvp.sql`

## Edge Functions

Server-side logic for:
- Private event notification delivery
- Push notification routing via FCM
- Contact matching and user resolution

Edge Functions use Deno runtime and can import Supabase client libraries.

## Database Schema

See `docs/dbSchema.md` for full schema documentation.

Core tables:
- `users`: User profiles linked to Supabase Auth
- `user_contacts`: Private emergency contacts
- `events`: Emergency alert records
- `event_recipients`: Private delivery tracking
- `event_user_state`: Optional public dismiss persistence (deferred in MVP)

## Security Notes

- **NEVER** commit `.env` file to version control
- **NEVER** use `SUPABASE_SERVICE_ROLE_KEY` in mobile apps
- Row Level Security (RLS) is enabled on all tables
- Service role key is only used in Edge Functions
