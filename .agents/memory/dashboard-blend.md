---
name: Dashboard & Blend features
description: Architecture decisions for the Spotify-style dashboard and Last.fm blend screens added to Nerox Music
---

## Dashboard Screen
- Route: `"dashboard"`, accessible from StatsScreen top bar via `R.drawable.trending_up` icon
- ViewModel: `DashboardViewModel` — queries Room DB directly for hourly/weekly/monthly/all-time stats
- Hourly chart: custom `getHourlyPlayTime(dayStart, dayEnd)` DAO query using epoch-ms arithmetic `(timestamp / 3600000) % 24`
- Monthly stats: uses existing `playCount` table (has `year`, `month`, `count` columns, table name = "playCount")
- Bar charts: implemented with Compose `Canvas { drawRoundRect }` — no external charting library
- All-time range uses `0L to Long.MAX_VALUE / 2` (not `Long.MAX_VALUE` to avoid SQLite overflow)

## Blend Screen
- Route: `"blend"`, accessible from StatsScreen top bar via `R.drawable.group_outlined` icon; also reachable from DashboardScreen top bar
- ViewModel: `BlendViewModel` — calls `LastFM.getUserTopArtists/getUserTopTracks` (public API, no auth) then computes Jaccard similarity score
- Compatibility formula: 40% Jaccard artist overlap + 20% Jaccard track overlap + 40% top-N shared artists
- Score is scaled ×1.5 and clamped to 10–99%
- Supabase storage: optional (shows "Share Blend" button only if `SupabaseBlendClient.isConfigured` is true)
- Blend codes: 6-char alphanumeric (no 0/O/1/I to avoid confusion)

## New files created
- `app/.../blend/SupabaseBlendClient.kt`
- `app/.../viewmodels/DashboardViewModel.kt`
- `app/.../viewmodels/BlendViewModel.kt`
- `app/.../ui/screens/DashboardScreen.kt`
- `app/.../ui/screens/BlendScreen.kt`
- `app/.../db/entities/HourPlayTime.kt`
- `app/.../db/entities/MonthPlayCount.kt`
- `lastfm/.../models/UserStats.kt`

**Why:** Spotify-style listening insights requested by user; Blend uses Last.fm public API (no user auth required).
**How to apply:** If adding more stats screens, follow the same pattern: DAO query → StateFlow in ViewModel → Compose collectAsState().
