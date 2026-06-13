# Weekend Run Spots — Live Foursquare Places API

> **Status:** Design — approved 2026-06-14, implementation pending.
> **Goal:** Make the Home "Plan Your Weekend Run" section show **real nearby
> places to run** (parks, trails, recreation areas) fetched live from a public
> REST API, each with a **photo + name + distance + category**, and keep them
> bookmark-saveable. This is the first feature to exercise the Retrofit/Moshi/
> OkHttp stack that has been wired-but-unused since Phase 3 (see plan.md §2),
> and it delivers the §10.2 "Smarter routes — real route discovery from the
> Maps/places API" vision item.

---

## 1. Why this, why Foursquare

The "Plan Your Weekend Run" section in `HomeScreen.kt` (`WeekendRunSection`,
~line 211) today renders **two hardcoded cards** from `routeList` in
`model/RunRoute.kt` — local drawable images, fixed title/distance/difficulty.
Nothing comes from the internet.

The assignment needs **live data taken from the internet via a REST API and
displayed**. The data the user wants is *places to run* — with a picture and
some info. No free public API serves curated "running routes with photos +
route distances", so the realistic match is a **Places / POI API** that returns
real nearby parks/trails with photos.

**Foursquare Places API** was chosen over Google Places because it issues a free
API key with **no credit card / billing account** required — lower friction and
no billing risk for a student/demo project. It returns exactly what the cards
need: nearby place **name, distance-from-user, category, and a photo**.

This mirrors the project's existing key-handling pattern (`MAPTILER_API_KEY`,
`GOOGLE_WEB_CLIENT_ID`): the key lives in `local.properties` (git-ignored) and
is surfaced via `BuildConfig`, with a graceful fallback when blank.

---

## 2. Scope

**In scope**
- A Retrofit-based networking layer (first real use of the wired stack).
- A repository that fetches nearby run-friendly places from Foursquare and maps
  them to a UI model.
- The Home Weekend Run section consuming a loading / success / fallback state.
- Bookmark-save of a fetched spot (photo URL + distance) into the existing Room
  `saved_routes` table.
- A unit test for the DTO → UI mapper (pure, no network).
- A short setup doc for getting + configuring the Foursquare key.

**Out of scope (YAGNI)**
- Weather (explicitly dropped during brainstorming).
- A dedicated full-screen route browser; we enhance the existing section only.
- Caching place results in Room (the cards are ephemeral discovery, not synced
  data; only *saved* routes persist — as they already do).
- Pagination / infinite scroll.

---

## 3. Architecture & data flow

Fits the existing **UI → ViewModel → Repository** layering. The network call is
a one-shot suspend fetch (not a Room-backed `Flow`), because discovery results
are transient — only bookmarks persist.

```mermaid
flowchart TD
    UI["WeekendRunSection (Compose)"]
    VM["UserViewModel<br/>weekendSpots: StateFlow<WeekendRunUiState>"]
    REPO["RunSpotRepository"]
    API["FoursquareApi (Retrofit)"]
    FS["Foursquare Places REST API"]
    LOC["Location (fused GPS + fixed fallback)"]
    ROOM[("Room saved_routes<br/>(bookmarks only)")]

    UI -->|observes state| VM
    VM -->|fetchNearbySpots(lat,lng)| REPO
    VM -->|get location| LOC
    REPO -->|GET places/search| API
    API --> FS
    FS -->|JSON results| REPO
    REPO -->|map DTO→UI, build photo URL| VM
    VM -.->|Success / Fallback / Loading| UI
    UI -->|bookmark toggle| VM
    VM -->|save title+imageUrl+distance| ROOM
```

**State machine** (`WeekendRunUiState`):
- `Loading` — show a shimmer/placeholder row.
- `Success(List<RunSpot>)` — live results from Foursquare.
- `Fallback(List<RunSpot>)` — network/parse error or blank API key → the
  existing hardcoded list, mapped into the same `RunSpot` model so the UI is
  uniform. (Optionally surfaces a subtle "showing sample routes" note.)

The section **never blocks and never shows a hard error** — worst case it shows
sample routes, so the screen always renders in a demo.

---

## 4. Components

### 4.1 `data/remote/FoursquareApi.kt` (new) — Retrofit interface
```
GET places/search
  query: ll = "lat,lng"
         radius = <meters, e.g. 8000>
         categories = <comma-separated run-friendly category ids>
         fields = "name,distance,categories,photos,location"
         limit = 10
  header: Authorization: <api key>   (per Foursquare auth scheme)
          Accept: application/json
```
Run-friendly categories (Foursquare taxonomy): parks, trails, recreation /
outdoors (e.g. Park, Trail, Athletics & Sports / Track). Exact id list pinned in
the implementation plan after a quick taxonomy check; a small constant `Set` in
the repo keeps it editable.

### 4.2 `data/remote/FoursquareDtos.kt` (new) — Moshi DTOs
Mirror only the fields requested: response wrapper → `results: List<PlaceDto>`;
each `PlaceDto` has `name`, `distance` (meters, Int), `categories` (→ first
category name), `location` (formatted address), and `photos`
(`prefix` + `suffix` → photo URL builder `"$prefix$size$suffix"`, e.g. size
`"original"` or `"800x600"`). `@JsonClass(generateAdapter = true)` (Moshi
codegen is already a KSP dep).

### 4.3 `data/remote/NetworkModule.kt` (new) — Retrofit/OkHttp factory
- `OkHttpClient` with `HttpLoggingInterceptor` (BODY in debug, NONE in release —
  `logging-interceptor` already a dep).
- `Retrofit.Builder` with `MoshiConverterFactory`, base URL = Foursquare Places
  base, exposing a lazily-built `FoursquareApi`.
- Owned as a singleton by `RunTrackApplication` (same place the repos live), or a
  simple `object`. No Hilt (consistent with the current manual-DI approach;
  Hilt remains a P3 backlog item).

### 4.4 `data/repository/RunSpotRepository.kt` (new)
- `suspend fun nearbyRunSpots(lat: Double, lng: Double): List<RunSpot>` —
  switches to `Dispatchers.IO`, calls the API, maps DTO → `RunSpot`, builds the
  photo URL. Any exception (IO, parse, blank key, empty results) → returns the
  hardcoded fallback mapped to `RunSpot`. Returns a sealed/marked result so the
  VM knows live vs fallback, or two distinct methods — decided in the plan.
- Reads the key from `BuildConfig.FOURSQUARE_API_KEY`; **blank → fallback** (so
  the build/app work before the key is set, matching the MapTiler pattern).

### 4.5 `model/RunSpot.kt` (new) — UI model
```
data class RunSpot(
    val title: String,        // place name
    val distance: String,     // "1.2 km away" (formatted from meters)
    val category: String,     // e.g. "Park", "Trail"
    val imageUrl: String?,    // Foursquare photo URL, null → placeholder
)
```
Kept separate from `RunRoute`/`RunRouteModel` so the existing hardcoded
section/save code isn't disturbed; the fallback mapper converts `routeList`
entries into `RunSpot`s (drawable → no `imageUrl`, use the existing image).

### 4.6 ViewModel — extend `UserViewModel`
- Add `weekendSpots: StateFlow<WeekendRunUiState>`.
- On init (or first Home composition) resolve a location, then call the repo.
- Keep the existing `isRouteSaved` / `toggleRouteSave` API; saving a `RunSpot`
  routes through a small adapter so the bookmark stores `title + imageUrl +
  distance` into `saved_routes` (see §6).

### 4.7 UI — `WeekendRunSection` in `HomeScreen.kt`
- Reads `weekendSpots`. `Loading` → 1–2 placeholder cards (shimmer or simple
  surface). `Success`/`Fallback` → the existing `RouteCard` look, with
  `AsyncImage(model = spot.imageUrl)` (Coil; falls back to a placeholder
  drawable when `imageUrl == null`). Subtitle stays "Explore these popular
  places near you" (wording tweak from "routes").
- Bookmark icon behaviour unchanged; wired to the save adapter.

---

## 5. Location

**Device GPS with a fixed-city fallback.** The app already holds location
permission and a `FusedLocationProviderClient` (Record screen / Phase 4). The VM
requests last-known/current location; if unavailable or permission not yet
granted, it falls back to **hardcoded coordinates** (a sensible default city —
pinned in the plan, e.g. the campus/Kuala Lumpur area) so the section always has
something to query. This keeps the demo deterministic while still being "real"
when GPS is available.

---

## 6. Saving / bookmarks

Saving already writes to `SavedRouteEntity` (`ownerEmail`+`title` PK, with
distance/time/elevation/difficulty/imageRes) and write-throughs to Firestore
`users/{uid}/savedRoutes/{title}`. A fetched `RunSpot` has a remote `imageUrl`
rather than a drawable `imageRes`. Implementation plan will confirm the cleanest
of:
- **(preferred)** store the photo URL in the saved row (a nullable `imageUrl`
  column → an additive Room migration v5→v6, additive `ADD COLUMN`, matching the
  existing migration style), so a saved spot keeps its photo, **or**
- map a `RunSpot` onto the existing `RunRoute` save call with a placeholder
  drawable if a no-migration path is preferred.

The choice is small and isolated; defaulting to the migration so saved spots
keep their real photo. This is the one schema touch and will be called out
explicitly in the plan.

---

## 7. Build / config

`app/build.gradle.kts`:
- Read `FOURSQUARE_API_KEY` from `local.properties` (default `""`), expose via
  `buildConfigField("String", "FOURSQUARE_API_KEY", ...)` — identical to the
  `MAPTILER_API_KEY` lines already present.
- No new dependencies — Retrofit, `converter-moshi`, OkHttp, `logging-
  interceptor`, `moshi-kotlin` + codegen are all already declared.

`AndroidManifest.xml`: `INTERNET` permission (confirm present; map/image
loading implies it, but verify).

New doc `docs/setupfoursquare.md`: how to create a free Foursquare developer
account, generate a Places API key, and paste it into `local.properties`.

---

## 8. Testing

- **Unit (JVM):** `RunSpotMapperTest` — feed a sample Foursquare JSON / DTO into
  the DTO→`RunSpot` mapper and assert title, formatted distance ("1.2 km away"),
  category, and built photo URL. Pure function, no network — matches the
  existing `GalleryMapperTest` / `ConversationIdTest` style.
- Networking itself isn't unit-tested (thin Retrofit interface); verification is
  on-device.
- **On-device checkpoint:** open Home with a valid key + network → real nearby
  places with photos render; with no key / airplane mode → sample routes render
  (no crash, no spinner stuck).

---

## 9. File-change summary

| File | Change |
|------|--------|
| `data/remote/FoursquareApi.kt` | **new** — Retrofit interface |
| `data/remote/FoursquareDtos.kt` | **new** — Moshi DTOs + photo-URL builder |
| `data/remote/NetworkModule.kt` | **new** — Retrofit/OkHttp/Moshi factory |
| `data/repository/RunSpotRepository.kt` | **new** — fetch + map + fallback |
| `model/RunSpot.kt` | **new** — UI model + `WeekendRunUiState` |
| `view_model/UserViewModel` | extend — `weekendSpots` state + save adapter |
| `view/screen/HomeScreen.kt` | `WeekendRunSection` consumes state, `AsyncImage` from URL |
| `app/build.gradle.kts` | `FOURSQUARE_API_KEY` buildConfig field |
| `data/entities/Entities.kt` + `AppDatabase.kt` | (if chosen) additive `imageUrl` column + v5→v6 migration |
| `app/src/test/.../RunSpotMapperTest.kt` | **new** — mapper unit test |
| `docs/setupfoursquare.md` | **new** — key setup |
| `RunTrackApplication.kt` | wire the new repo/network singleton |

---

## 10. Risks & mitigations

- **Foursquare API surface drift** — Foursquare reworked its Places platform;
  the exact base URL, auth header form, and required version header will be
  pinned during implementation against current docs (the plan step will verify a
  live `curl` before wiring). The architecture (interface + DTOs + mapper) is
  insulated from these details.
- **No key / offline** — handled by the fallback path; app never breaks.
- **Photo sizes / broken URLs** — Coil handles load failure; `imageUrl == null`
  → placeholder drawable.
- **TLS-inspecting WiFi** (noted in plan.md for GMS) — same networks can throttle
  REST; the fallback covers it for demos.
