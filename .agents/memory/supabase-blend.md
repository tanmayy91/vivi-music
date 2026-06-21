---
name: Supabase blend storage
description: How Supabase is integrated for storing shared Blend sessions in Nerox Music
---

## Setup
- Credentials stored in `local.properties` as `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- Read in `app/build.gradle.kts` defaultConfig block → baked into `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`
- `SupabaseBlendClient.isConfigured` returns false if either is blank → UI hides the "Share Blend" button gracefully

## Supabase table required
```sql
CREATE TABLE blends (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT UNIQUE NOT NULL,
  user1_username TEXT,
  user2_username TEXT,
  user1_top_artists TEXT,
  user2_top_artists TEXT,
  compatibility_score FLOAT,
  shared_artists TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);
```

## HTTP client
- Uses OkHttp directly (available transitively via media3/coil/ktor deps in app module)
- REST: POST to `/rest/v1/blends` with `Prefer: return=representation` header to get saved record back
- GET: `/rest/v1/blends?code=eq.{code}&limit=1`

**Why:** User requested Supabase for blend persistence; avoided adding a new Supabase SDK dependency by using OkHttp directly.
