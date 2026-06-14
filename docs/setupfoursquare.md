# Foursquare Places API setup

The Home **"Plan Your Weekend Run"** section fetches real nearby places to run
(parks) from the Foursquare Places API. Without a key the app still works — it
falls back to the built-in sample routes.

## Get a key (free, no credit card)

The key you need is a **Service API Key** on Foursquare's current developer
platform (`places-api.foursquare.com`). Ignore older tutorials that mention a
"v3 API key" — that flow is being retired.

1. **Sign in / sign up** at https://foursquare.com/developers/login
   (free account, includes starter credit, no credit card).
2. In the **Developer Console**, create a **Project** (any name, e.g. "Enerva").
3. Open that project's **Settings** page.
4. In the **Service API Keys** section, click **Generate Service API Key**.
5. Give it a name (e.g. "android-app") → **Next**.
6. **Copy the key immediately** with the copy icon. ⚠️ This is the *only* time
   it is shown — if you lose it, revoke it and generate a new one.
7. Add it to `local.properties` at the repo root (this file is git-ignored):

   ```
   FOURSQUARE_API_KEY=paste_your_key_here
   ```
   Paste only the raw key — no quotes, no spaces, and no `Bearer ` prefix (the
   app adds `Bearer ` for you). Example: `FOURSQUARE_API_KEY=ABCD1234...`
8. Rebuild. The section now shows live nearby parks with photos and
   "x.x km away" distances.

> No key? The app still runs — it falls back to the built-in sample routes
> (Teratai, Lakeside Trail). The key only adds *live* data.

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
