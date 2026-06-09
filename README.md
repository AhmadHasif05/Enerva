# Enerva 🏃‍♂️

**Enerva** is an Android app for *social cardio* — track your runs, walks, and rides; turn them into reels; discover and follow other runners; chat; and keep each other moving. Inspired by Strava, built as a modern Android engineering showcase.

> 📐 **Full design & architecture:** see [`plan.md`](plan.md) for the complete master document (concept, architecture diagrams, data model, security, roadmap, and UX).

---

## Problem Statement

People want stronger health and a fitter body, and they turn to cardio — jogging, running, brisk walking — to get there. But the journey usually ends too soon: motivation fades, routines break, routes feel uncertain, progress is hard to measure, and going it alone makes cardio a struggle instead of a habit. Enerva attacks the *motivation* problem by making progress **measurable**, **social**, and **shareable**.

---

## Features

- 📍 **Record** — real-time GPS activity tracking (run / walk / ride) with live distance, pace, and time.
- 🎞️ **Gallery (Reels)** — every run can become an Instagram-style reel; view your own and others'.
- 👥 **Social** — search, follow, profile pages, and cross-device user discovery.
- 💬 **Messaging** — 1:1 and group chat, synced across devices via Firestore.
- 🗺️ **Routes** — browse and bookmark suggested weekend routes.
- 🔐 **Accounts** — email/password + Google Sign-In, with password reset.
- 📶 **Offline-first** — the app reads instantly from a local cache and syncs to the cloud in the background.

---

## Architecture

**MVVM + an offline-first Repository layer.** Room is the read source of truth (so the UI never waits on the network); Cloud Firestore is the write-through target and real-time push source.

```
Compose UI  →  ViewModels  →  Repositories  →  Room (local cache, read truth)
                                            ↘  Firebase (Auth + Firestore, cloud truth)
                              ⤺  Firestore listeners fold remote changes back into Room
```

See [`plan.md` §3](plan.md#3-architecture) for layered + sequence diagrams.

---

## Screens (10)

Single-Activity Compose app with **5 bottom-nav tabs** (Home · Search · Record · Gallery · Profile):

| Auth | Core tabs | Detail screens |
|------|-----------|----------------|
| Login, Signup | Home, Search, Record, Gallery, Profile | Edit Profile, Messages, Chat |

---

## Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3
- **Navigation:** [Navigation-Compose](https://developer.android.com/jetpack/compose/navigation)
- **Local DB:** [Room](https://developer.android.com/training/data-storage/room) (KSP)
- **Cloud:** [Firebase Auth](https://firebase.google.com/docs/auth) + [Cloud Firestore](https://firebase.google.com/docs/firestore)
- **Async:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Images:** [Coil](https://coil-kt.github.io/coil/)
- **Location:** Google Play Services Location (`FusedLocationProviderClient`)
- **Camera:** [CameraX](https://developer.android.com/training/camerax) *(media capture — Phase 5)*
- **Networking:** [Retrofit](https://square.github.io/retrofit/) + [Moshi](https://github.com/square/moshi) *(reserved for Maps/places)*
- **Prefs:** DataStore (Preferences)

---

## Project Structure

```
model/        UI/domain models (not Room entities)
data/         AppDatabase, entities/, dao/, cloud/ (Firestore DTOs), repository/
view_model/   ViewModels (User, Message, Gallery, Record, Login, Signup)
view/         MainActivity, Navigation, components/, screen/
ui/theme/     Material 3 theme (Color, Theme, Type)
```

> Dependencies point downward only: `view` → `view_model` → `data/repository` → `data/{dao,cloud}`.

---

## Build & Setup

This app needs Firebase config and (for Google Sign-In) a Web client ID:

1. Place `google-services.json` in `app/` — see [`setupfirebase.md`](setupfirebase.md).
2. For Google Sign-In, set `GOOGLE_WEB_CLIENT_ID=...` in `local.properties` (gitignored) — see [`setupfirebase-google-signin.md`](setupfirebase-google-signin.md).
3. Build & run from Android Studio.

---

## Roadmap

| Phase | Status |
|-------|--------|
| Screen trim → 10 screens | ✅ |
| Room (local persistence) | ✅ |
| Firebase Auth + Firestore | ✅ (rules deploy + checkpoint pending) |
| Google Maps (real map in Record) | 🔜 |
| Camera capture → reels (CameraX + Storage) | 🔜 |

---

*Enerva is an educational / portfolio project — not a commercial product. The codebase uses `RunTrack` as an internal codename.*
