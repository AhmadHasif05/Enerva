# Foursquare Places API setup

The Home **"Plan Your Weekend Run"** section fetches real nearby places to run
(parks) from the Foursquare Places API. Without a key the app still works — it
falls back to the built-in sample routes.

## Get a key (free, no credit card)
1. Sign up at https://foursquare.com/developers/
2. Create a project / app and generate a **Places API** key (a Service Key /
   Bearer token for the new `places-api.foursquare.com` platform).
3. Add it to `local.properties` at the repo root (this file is git-ignored):

   ```
   FOURSQUARE_API_KEY=YOUR_KEY_HERE
   ```
4. Rebuild. The section now shows live nearby parks with photos and
   "x.x km away" distances.

## How it works
- **Location:** uses the device's last-known GPS fix (the same location
  permission the Record screen already requests — no extra prompt). With no fix
  available it defaults to Kuala Lumpur (`3.1390, 101.6869`).
- **Endpoint:** `GET https://places-api.foursquare.com/places/search`
  - Headers: `Authorization: Bearer <key>`, `X-Places-Api-Version: 2025-06-17`
  - Query: `ll=<lat,lng>`, `query=park`, `radius=8000`, `sort=DISTANCE`,
    `limit=10`, `fields=name,distance,categories,photos`
- **Fallback:** a blank key, no network, an API error, or zero results all fall
  back to the sample routes — the screen never crashes or hangs. Fetch failures
  are logged to logcat under the tag `RunSpotRepository`.

## Verify the key quickly (optional)
```bash
curl "https://places-api.foursquare.com/places/search?ll=3.1390,101.6869&query=park&radius=8000&limit=3&fields=name,distance,categories,photos" \
  -H "Authorization: Bearer YOUR_KEY" \
  -H "X-Places-Api-Version: 2025-06-17" \
  -H "Accept: application/json"
```
A valid key returns a JSON object with a `results` array of nearby parks.

## Code map
- `data/remote/PlaceDtos.kt` — response DTOs + photo-URL / distance helpers
- `data/remote/FoursquareApi.kt` — Retrofit interface
- `data/remote/NetworkModule.kt` — OkHttp (auth/version interceptor) + Moshi + Retrofit
- `data/repository/RunSpotRepository.kt` — fetch + map to `RunRoute` + fallback
- `view_model/UserViewModel.kt` — `weekendRun` state + last-known-location fetch
- `view/screen/HomeScreen.kt` — `WeekendRunSection` renders the cards
