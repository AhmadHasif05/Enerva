# RunTrack — Update Plan

Goal: slim the app to **exactly 10 screens**, then layer in **Room (local DB)**, **Cloud Firestore + Firebase Auth (cloud)**, **Google Maps API**, and **camera capture in the Record screen**.

---

## Decisions (locked)

- **Launch screen:** app opens directly on **Login** (Welcome removed).
- **Settings:** deleted as a screen — only the **Log out** action survives, moved into **Profile**.
- **Groups:** deleted as a screen — group features move into **Message** (create groups from your friends).
- **Activity:** deleted as a screen — activity history is shown as **reels in Gallery**.
- **Record + Maps:** **one combined screen** (map fills screen, recording stats/controls overlay).
- **Gallery (reels):** per-user reels, like a profile feed. You see **your own** gallery and can view **friends'** galleries (different people → different reels).
- **Auth (Phase 3):** **Firebase Auth** (real per-user accounts + security rules).

---

## 0. Package rename — DONE ✅

Code package `...drnelson_Project1` renamed to `...drnelson_Project2` to match the app namespace/applicationId. All 40 source files + 3 source-set directories (main/test/androidTest) updated. No code errors.

---

## 1. Current State (as-is)

- **Architecture:** Jetpack Compose + MVVM, single-Activity (`MainActivity`), navigation in `view/Navigation.kt` + `MainActivity.kt`.
- **Persistence:** None. All state in-memory in ViewModels (`mutableStateOf`). Lost on restart.
- **Room:** Gradle deps present, but **no `@Entity`/`@Dao`/`@Database`** yet. Unused.
- **Firestore / Firebase:** Not present in code or Gradle.
- **Maps:** Fake — hand-drawn `Canvas` trails. Real GPS works via `FusedLocationProviderClient`.
- **Camera:** CameraX deps present, **not used**.

---

## 2. Target — 10 Screens

| # | Screen | Source | Action |
|---|--------|--------|--------|
| 1 | Login | `LoginScreen.kt` | Keep (now the launch screen) |
| 2 | Sign Up | `SignupScreen.kt` | Keep |
| 3 | Home | `HomeScreen.kt` | Keep |
| 4 | Profile | `ProfileScreen.kt` | Keep + add Log out, link to Gallery |
| 5 | Message | `MessageScreen.kt` | Keep + group creation from friends |
| 6 | Edit Profile | `EditProfile.kt` | Keep |
| 7 | Record + Maps | merge `RecordScreen.kt` + `MapsScreen.kt` | **Merge into one** |
| 8 | Gallery (reels) | new `GalleryScreen.kt` | **Create** (own + friends' reels) |
| 9 | Search | `SearchScreen.kt` | Keep |
| 10 | Chat | `ChatScreen.kt` | Keep |

### Screens to DELETE
Welcome (`MainScreen.kt`), Groups (`GroupsScreen.kt`), You (`YouScreen.kt`), Settings (`SettingScreen.kt`), Notifications (`NotifyScreen.kt`), Activity (`ActivityScreen.kt`).

### Bottom navigation (5 tabs)
Home · Search · Record+Maps · Gallery · Profile

---

## 3. Phase 1 — Trim & restructure to 10 screens — DONE ✅

Pure UI/navigation restructure — no new tech yet. **Compiles clean (`BUILD SUCCESSFUL`).**

1. `Navigation.kt`: trim `Screen` sealed class (remove Welcome/Groups/You/Settings/Notifications/Activity/Maps), add `Gallery`; set `bottomNavItems` to the 5 tabs.
2. `MainActivity.kt`: `startDestination = Login`; auth routes = Login + Signup; remove deleted composables; add Gallery; Record route = combined screen.
3. `ProfileScreen.kt`: replace Settings icon with **Log out** (→ Login, clear back stack); point Activities → Gallery.
4. `HomeTopBar.kt`: remove the Notifications button.
5. `HomeScreen.kt`: "see all" → Gallery instead of Activity.
6. `MessageScreen.kt`: replace Settings icon with **New Group** entry (friends-based; full feature later).
7. Create `GalleryScreen.kt`: vertical reels pager (own + friends' reels; sample data for now).
8. Merge `MapsScreen` UI into `RecordScreen` (map search/filters overlay + recording controls); delete `MapsScreen.kt`.
9. Delete the 6 screen files + `ChallengeViewModel` (only used by Groups).
10. Build & run. **Checkpoint: app launches on Login, all 10 screens reachable, compiles.**

---

## 4. Phase 2 — Room (local persistence) — IN PROGRESS 🚧

1. Entities: `UserEntity`, `ActivityRecordEntity` (saved run → also a reel), `MessageEntity`, `GroupEntity`, `MediaEntity` (reel item).
2. DAOs with suspend / `Flow` queries.
3. `AppDatabase : RoomDatabase` + singleton.
4. Refactor ViewModels to read/write via DAOs.
5. Persist login/profile, saved activities, messages, gallery media.
6. **Checkpoint: data survives restart.**

---

## 5. Phase 3 — Firebase Auth + Cloud Firestore

1. Firebase project + `google-services.json` in `app/`; add `google-services` plugin + Firebase BoM, Auth, Firestore deps. **(needs your Firebase project)**
2. Firebase Auth (email/password) replaces the in-app email login.
3. **Real Google Sign-In** replaces the current mock. The "Continue with Google" button on `LoginScreen` currently calls `UserViewModel.loginWithGoogle()`, which just sets a fake/registered user and navigates Home — no real account picker or verification. Real flow: add Credential Manager + `play-services-auth` (or Firebase Auth Google provider), a **web client ID** from the Firebase console, launch the Google account picker, then sign in to Firebase Auth with the returned ID token. **(needs your Firebase project + web client ID)**
4. Firestore collections: `users/{uid}`, `users/{uid}/activities`, `users/{uid}/media`, `chats/{chatId}/messages`, `groups/{groupId}`.
5. Repository layer between ViewModels and (Room + Firestore) — offline-first.
6. Real-time listeners for chat/messages and friends' galleries.
7. Security rules scoped to the signed-in user.
8. **Checkpoint: data syncs across installs; friends' reels load from cloud.**

---

## 6. Phase 4 — Google Maps API (real map in Record+Maps)

1. Add `maps-compose` + `play-services-maps`.
2. Maps API key via `local.properties`/secrets plugin (**never commit the key**). **(needs your API key)**
3. Replace `Canvas` backdrop with `GoogleMap` composable.
4. Draw live trail as a `Polyline` from `RecordViewModel.path`; current-location marker.
5. **Checkpoint: live GPS path on a real map.**

---

## 7. Phase 5 — Camera in Record screen

1. Add `CAMERA` permission.
2. CameraX capture (`PreviewView` + `ImageCapture`/`VideoCapture`) using existing deps.
3. Capture button on Record; request camera + location permissions.
4. Save media → `MediaEntity` (Phase 2) + optional Firebase Storage upload (Phase 3).
5. Captured media appears in Gallery reels.
6. **Checkpoint: capture from Record → appears in Gallery.**

---

## 8. Order of work

Phase 0 (rename ✅) → Phase 1 (trim ✅) → **Phase 2 (Room) ← current** → Phase 3 (Firebase) → Phase 4 (Maps) → Phase 5 (Camera).

Each phase ends buildable. Phases 3 & 4 need credentials from you (Firebase project file, Maps API key).
