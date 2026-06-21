---
name: Last.fm credentials
description: How Last.fm API credentials are stored and used in Nerox Music
---

## Current state
- API key and secret are hardcoded directly in `app/build.gradle.kts` defaultConfig block
- Registered to Last.fm user: `tanmayy3128`
- The `local.properties` / env-var approach is commented out in build.gradle.kts

**Why:** The original project used local.properties but the user provided new credentials directly for tanmayy3128's registered app.
**How to apply:** To update keys, edit the two hardcoded string values in `app/build.gradle.kts` lines ~41-42.
