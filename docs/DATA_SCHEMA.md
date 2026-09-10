# Quran data schema and API abstraction

This document defines the data the app works with, where it comes from, and how a
different source can be plugged in without changing the UI or business logic.

## 1. Layers

```
UI (Compose screens, ViewModels)
        │  uses domain models only
Domain  (domain/model, domain/error, repository interfaces)
        │
Data    (repositories)  ──►  Local: Room database (offline source of truth)
                        └─►  Remote: QuranRemoteDataSource  ◄── AlQuranCloudDataSource
```

The UI never talks to the network. Repositories download through
`QuranRemoteDataSource`, validate, store in Room, and the UI reads from Room.
After the first download, reading works with no network at all.

## 2. Current source: alquran.cloud

Base URL `https://api.alquran.cloud/v1/`. Quran text is the Tanzil project's verified
text, served unchanged.

| Purpose | Endpoint | Notes |
|---|---|---|
| Surah list | `GET surah` | 114 surahs, names, ayah counts, revelation type |
| Surah in several editions | `GET surah/{n}/editions/{e1,e2,…}` | One request per surah for script, search script and translation |
| One surah, one edition | `GET surah/{n}/{edition}` | Audio editions include per-ayah URLs |
| One ayah, one edition | `GET ayah/{s}:{a}/{edition}` | On-demand tafsir |
| Editions | `GET edition?type=…&format=…` | `translation`, `tafsir`, `quran`, `versebyverse`+`audio` |

Editions used by default:

| Role | Edition | Displayed? |
|---|---|---|
| Quran script | `quran-uthmani` (Uthmani, full diacritics) | Yes |
| Search script | `quran-simple-clean` (standard spelling, no diacritics) | **No**, only used for search |
| Translation | `en.sahih` (Saheeh International), user-selectable (118 available) | Yes |
| Tafsir | `ar.muyassar`, user-selectable (6 available) | Yes, on demand |
| Recitation | `ar.alafasy`, user-selectable (32 available) | Audio |

## 3. Source-neutral contract (`data/remote`)

Every source implements `QuranRemoteDataSource` and returns these types:

**RemoteSurah**: `number` (1–114), `nameArabic`, `nameTransliterated`, `nameTranslated`,
`ayahCount`, `revelationType` (`MECCAN` / `MEDINAN` / `UNKNOWN`).

**RemoteAyah**

| Field | Range | Meaning |
|---|---|---|
| `globalNumber` | 1–6236 | Stable ayah key shared by every edition and by audio |
| `surah`, `numberInSurah` | | Position, e.g. 2:255 |
| `text` | | Exact text in the requested edition |
| `juz` | 1–30 | |
| `hizbQuarter` | 1–240 | Rub' al-hizb; hizb = (hizbQuarter − 1) / 4 + 1 |
| `page` | 1–604 | Madani mushaf page |
| `manzil` | 1–7 | |
| `ruku` | | |
| `sajda` | nullable | `{id, obligatory}` on the 15 sajdah ayahs |

**RemoteSurahEdition**: `edition`, `surah`, `ayahs`. One surah in one edition.

**RemoteEdition**: `identifier`, `language` (ISO 639-1), `name`, `englishName`,
`type` (`QURAN` / `TRANSLATION` / `TAFSIR` / `AUDIO`), `isRightToLeft`.

**RemoteAyahAudio**: `globalNumber`, `numberInSurah`, `url`, `lowBitrateUrl`.

Errors are always one of `QuranError`: `NoConnection`, `Server(httpCode)`, `NotFound`,
`InvalidData`, `Database`, `Unknown`. `isRetryable` is true for `NoConnection` and 5xx.

## 4. Integrity rules

The Quran text is religiously sensitive, so these rules apply everywhere:

1. **Verbatim.** Text is stored and shown exactly as delivered: no trimming,
   normalization, re-encoding or "fixing". (Surah 1 ayah 1 starts with an invisible
   U+FEFF character in the source. It is kept as is and renders as nothing.)
2. **Validated before storage.** `QuranDataValidator` rejects a surah unless:
   - its ayah count equals the canonical Hafs count (built-in table, 6236 total);
   - ayahs are numbered 1…n without gaps, belong to that surah, and have contiguous
     global numbers;
   - no text is blank;
   - juz, page, hizb quarter and manzil are within their ranges.
   Rejected data is never written. The download simply retries.
3. **Basmala.** In this source, ayah 1 of every surah except 1 and 9 begins with the
   basmala. It is shown as part of that ayah, exactly as provided, and the app adds no
   separate basmala line.
4. **Search index is separate.** Diacritic-free, normalized copies used for search live
   in their own table and are never displayed. Results always show the original text.
5. **Only decorations are added.** The reader adds ayah-number markers and page/juz
   dividers *around* the text, never inside it.

## 5. Local database (Room), added in Step 3

| Table | Key | Columns |
|---|---|---|
| `surahs` | `number` | nameArabic, nameTransliterated, nameTranslated, ayahCount, revelationType |
| `ayahs` | `id` (global number) | surah, numberInSurah, text, juz, hizbQuarter, page, manzil, ruku, sajdaId, sajdaObligatory. Indexed on (surah, numberInSurah), juz, page, hizbQuarter |
| `ayah_search` | `ayahId` | normalized Uthmani text, normalized plain text |
| `editions` | `identifier` | language, name, englishName, type, isRightToLeft |
| `edition_texts` | (`edition`, `ayahId`) | text, searchText (translations and tafsir) |
| `audio_urls` | (`reciter`, `ayahId`) | url, lowBitrateUrl |
| `bookmarks` | `id` | surah, ayah, note, createdAt (unique surah+ayah) |
| `reading_positions` | `type` (`LAST_READ` / `MARK`) | surah, ayah, page, juz, scroll offset, updatedAt |

Settings (theme, fonts, reading mode, editions, reciter…) are stored in Jetpack DataStore.
Downloaded audio lives under the app's files directory, one file per ayah.

## 6. Adding another source

1. Create `data/remote/<name>/` with DTOs, a Retrofit interface, and a class
   implementing `QuranRemoteDataSource`. Map to the `Remote*` types and call
   `QuranDataValidator` on each surah.
2. Wrap failures with `QuranError` (see `AlQuranCloudDataSource.call`).
3. Change the one line in `AppContainer` that creates the data source.
4. Add recorded responses to `src/test/resources` and copy `AlQuranCloudDataSourceTest`.

Nothing in `domain/`, the repositories' public API, or `ui/` needs to change.
