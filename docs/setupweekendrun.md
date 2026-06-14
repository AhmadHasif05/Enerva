# Weekend Run spots (OpenStreetMap) — no setup required

The Home **"Plan Your Weekend Run"** section fetches real nearby places to run
(parks, gardens, nature reserves, recreation grounds, tracks, sports centres)
from the **OpenStreetMap Overpass API**.

**There is no API key and no account to set up.** Overpass is free and keyless,
so the section works out of the box — just build and run.

## How it works
- **Location:** uses the device's last-known GPS fix (the same location
  permission the Record screen already requests — no extra prompt). With no fix
  available it defaults to Kuala Lumpur (`3.1390, 101.6869`).
- **Endpoint:** `GET https://overpass-api.de/api/interpreter?data=<Overpass QL>`
  - Header: `User-Agent: Enerva-Android/1.0` (Overpass etiquette)
  - Query (Overpass QL): nearby `leisure` areas within an 8 km radius —
    `park|garden|nature_reserve|recreation_ground|track|sports_centre`
- **Distance:** computed on-device (Haversine) from your location to each place,
  then the nearest 10 named places are shown.
- **Photos:** real photos come from each place's Wikidata/Commons data — the OSM
  `wikidata` tag is resolved (one batched call) to its Wikidata **P18 image**, then
  loaded from **Wikimedia Commons** (`Special:FilePath`). A direct `image=` file URL
  or `wikimedia_commons=File:...` tag is used as-is. Places with none of these fall
  back to a bundled cover image (Teratai / Lakeside Trail). All keyless.
- **Fallback:** no network, an API error, or zero results all fall back to the
  built-in sample routes (Teratai, Lakeside Trail) — the screen never crashes or
  hangs. Fetch failures are logged to logcat under the tag `RunSpotRepository`.

## Verify quickly (optional)
```bash
curl -s -G "https://overpass-api.de/api/interpreter" \
  --data-urlencode 'data=[out:json][timeout:25];(nwr["leisure"~"^(park|garden|nature_reserve|recreation_ground|track|sports_centre)$"](around:8000,3.1390,101.6869););out center 5;' \
  -H "User-Agent: Enerva-Android/1.0"
```
A working endpoint returns a JSON object with an `elements` array of nearby places.

## Code map
- `data/remote/OverpassDtos.kt` — response DTOs + distance/category/photo helpers (Haversine, Commons URL)
- `data/remote/OverpassApi.kt` — Overpass Retrofit interface
- `data/remote/WikidataApi.kt` + `WikidataDtos.kt` — P18 image lookup
- `data/remote/NetworkModule.kt` — shared OkHttp (User-Agent, timeouts) + Moshi + Overpass/Wikidata Retrofit
- `data/repository/RunSpotRepository.kt` — build query + resolve photos + map to `RunRoute` + fallback
- `view_model/UserViewModel.kt` — `weekendRun` state + last-known-location fetch
- `view/screen/HomeScreen.kt` — `WeekendRunSection` renders the cards
