# Map Setup Guide — Enerva

The Record screen shows a real vector map using **MapLibre** (open-source) with
**OpenStreetMap**-based tiles. You have two options:

- **No setup at all** — the map works out of the box using **OpenFreeMap** tiles (no account, no
  API key, no credit card). You can stop reading here if that's enough.
- **Add a free MapTiler key** — unlocks the extra map styles (Streets, **Satellite**, Outdoor) on
  the Layers button. Free, **no credit card required**. Steps below.

The key is read from `local.properties` and is **never committed**.

---

## Why not Google Maps?

Google Maps Platform requires a **billing account with a credit card** to issue a working API key,
even though the Android SDK is free. To avoid that, Enerva uses **MapLibre + MapTiler/OpenFreeMap**,
which need no card.

---

## Option A — Do nothing (keyless)

Leave `MAPTILER_API_KEY` blank in `local.properties` (or absent). The app falls back to OpenFreeMap
Liberty/Bright styles and the map renders normally. The Layers button cycles those two styles.

---

## Option B — Add a free MapTiler key (no credit card)

### Step 1 — Create a free MapTiler account

1. Go to **https://www.maptiler.com/cloud/** and click **Sign up**.
2. Register (email or Google/GitHub). **No credit card is requested** on the Free plan.

### Step 2 — Copy your API key

1. After signing in, open the **MapTiler Cloud** dashboard.
2. Go to **API Keys** (left menu). A **Default** key is created automatically.
3. Copy the key (a short alphanumeric string).

> Free plan: 100,000 map loads / month. Enerva's tracking screen uses a tiny fraction of that.

### Step 3 — Put the key in local.properties

Open `local.properties` (project root, git-ignored) and set:

```properties
MAPTILER_API_KEY=your-key-here
```

### Step 4 — Rebuild

Rebuild the app. On the Record screen, the **Layers** button now also cycles through MapTiler
**Streets → Satellite → Outdoor** in addition to the OpenFreeMap styles.

> (Optional) In the MapTiler dashboard you can restrict the key to your app's package name
> `com.example.a211198_hasif_drnelson_Project2` under the key's **Authorized origins / restrictions**.

---

## Troubleshooting

- **Blank / dark map** → if you set a MapTiler key, double-check it's pasted correctly with no spaces.
  Remove it to fall back to keyless OpenFreeMap and confirm the network works.
- **No tiles on a restricted network** → tile servers need outbound HTTPS; corporate/campus proxies
  can block them. Try mobile data.
- **`local.properties` got overwritten by Android Studio** → re-add the `MAPTILER_API_KEY` line;
  Studio only manages `sdk.dir`.

---

## What you should have at the end

- ✅ A real map rendering on the Record screen (keyless OpenFreeMap, or MapTiler if a key is set)
- ✅ (Optional) `MAPTILER_API_KEY=...` in `local.properties`
- ✅ No Google billing account, no credit card
