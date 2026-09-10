# quran-app

A native Android Quran reader in Kotlin + Jetpack Compose. It's built to be lightweight,
work offline, and run smoothly on older, low-end phones (Android 6.0+).

> **Status:** in progress, built step by step. Steps 1–2 of 11 are done.

## Features (planned)

- Quran text as structured data (Uthmani script, not images), cached for offline use
- Continuous (Mushaf-style) and ayah-by-ayah reading, adjustable font and line spacing
- Translations (100+ editions) and tafsir, loaded on demand
- Continue Reading, bookmarks with notes, reading mark
- Fast offline search (Arabic without diacritics, translation, surah names)
- Recitation with ayah highlighting, background playback and offline downloads
- Arabic and English UI, RTL, dark mode

## Progress

| Step | Scope | Status |
|---|---|---|
| 1 | Project skeleton, theme, Quran font, localization | ✅ |
| 2 | Quran data schema, API abstraction, alquran.cloud adapter, integrity validation | ✅ |
| 3 | Room database, offline download (resumable, retries) | ⏳ |
| 4 | Home, surah list, juz/hizb/page navigation | |
| 5 | Quran reader | |
| 6 | Continue reading, bookmarks, reading mark | |
| 7 | Ayah actions: tafsir, translation, share | |
| 8 | Search | |
| 9 | Audio recitation | |
| 10 | Settings, dark mode, language pickers, data management | |
| 11 | Release build, final tests | |

## Build

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleDebug        # build app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # run unit tests
```

## Architecture

Clean architecture in a single module: `ui` → `domain` ← `data` (Room + remote API).
The Quran API sits behind `QuranRemoteDataSource`, so it can be replaced without UI changes.
See [docs/DATA_SCHEMA.md](docs/DATA_SCHEMA.md).

## Data sources and credits

- Quran text: [Tanzil](https://tanzil.net) (verified Uthmani text), via [alquran.cloud](https://alquran.cloud/api)
- Translations, tafsir and recitations: alquran.cloud / Islamic Network CDN
- Font: [Amiri Quran](https://github.com/aliftype/amiri), SIL Open Font License 1.1

The Quran text is displayed exactly as provided by the source. It is never generated or modified.
