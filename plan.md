# RunTrack — Update Plan

Goal: slim the app to **exactly 10 screens**, then layer in **Room (local DB)**, **Cloud Firestore + Firebase Auth (cloud)**, **Google Maps API**, and **camera capture in the Record screen**.

---

## Current Phase

> **Phase 3 — Firebase Auth + Cloud Firestore** (in progress)

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Package rename | ✅ Done |
| 1 | Trim & restructure to 10 screens | ✅ Done |
| 2 | Room (local persistence) | ✅ Done |
| **3** | **Firebase Auth + Cloud Firestore** | **⏳ In Progress — see sub-phase tracker in §5** |
| 4 | Google Maps API (real map) | 🔜 Next |
| 5 | Camera in Record screen | 🔜 Upcoming |

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

## 4. Phase 2 — Room (local persistence) — DONE ✅

1. Entities: `UserEntity`, `ActivityRecordEntity` (saved run → also a reel), `MessageEntity`, `ConversationEntity`, `FollowEntity`, `SavedRouteEntity`, `MediaEntity` (reel item).
2. DAOs with suspend / `Flow` queries (`UserDao`, `MessageDao`, `ActivityDao`).
3. `AppDatabase : RoomDatabase` (version 3) + singleton in `RunTrackApplication`; `fallbackToDestructiveMigration` for dev.
4. ViewModels refactored to read/write via DAOs: `UpdateUserViewModel`, `MessageViewModel`, `RecordViewModel`, **new** `GalleryViewModel`.
5. Persists: login session (active email in prefs), profile (incl. **photoUri** picked via `PickVisualMedia`), follows, saved routes, conversations + messages, gallery media (own + demo cross-user posts).
6. **Checkpoint: data survives restart. ✅**

### 4a. Phase 2 polish — DONE ✅

Done in the same Room pass:

- **Gallery scoping:** `GalleryViewModel` exposes three modes — `showFeed` (cross-user, bottom-nav Gallery), `showMyPosts` (Profile + Home "Your Progress"), `showAuthorGallery(name)` (visiting another user). Each screen scopes its own VM via `NavBackStackEntry` so modes don't clobber each other.
- **Home "Your Progress"** now reads from Room filtered to the active user (replaced the static `sampleGalleryActivities` mix).
- **Cross-user demo seed:** `seedDemoAuthorsIfNeeded()` one-time-seeds 6 reels from Sarah/Daniel/Aisha so the Gallery feed shows variety pre-signups (gated by `galleryDemoSeeded` pref).
- **Profile photo:** added `photoUri` to `UserEntity`/`UserData` + `updatePhotoUri(...)` in VM; EditProfile wires `PickVisualMedia` + `takePersistableUriPermission`; ProfileScreen + EditProfile avatars render the picked photo.
- **Profile grid → own gallery:** tapping a tile now navigates to `userGalleryRoute(item.author)` instead of the all-users feed, keeping Profile→reels filtered to that user.
- **Record FAB:** moved from per-screen `BottomEnd` to a global overlay in `MainActivity` aligned `BottomCenter`, sized 80dp with `RadioButtonChecked` icon, straddling the top edge of the nav bar over the Record tab. Shown on all bottom-nav screens.

---

## 5. Phase 3 — Firebase Auth + Cloud Firestore

**Status: design approved, implementation in progress.** See `setupfirebase.md` for the one-time Firebase console setup (done ✅) and `docs/superpowers/specs/2026-05-30-phase3-firebase-design.md` for the locked spec.

### Sub-phase tracker

| # | Sub-phase | Status | Testable by |
|---|---|---|---|
| 5.0 | Firebase console setup (project, app, JSON, Auth, Firestore) | ✅ Done | `app/google-services.json` exists; Auth + Firestore enabled in console |
| 5.1 | Gradle wiring (BoM, plugin, deps) | ✅ Done | `BUILD SUCCESSFUL` with `processDebugGoogleServices` green |
| 5.4 | **Auth (email/password)** — Firebase init, `AuthRepository`, password fields, MainActivity start route | ✅ Done | Sign up in app → user appears in Firebase Console → Authentication → Users |
| 5.4a | **Signup → Login redirect** — sign out after signup so user logs in explicitly | ✅ Done | Sign up → toast "Account created. Please log in." → lands on Login (not Home) |
| 5.4c | **Forgot password** — "Forgot password?" link on Login → email reset flow via `sendPasswordResetEmail` | ✅ Done | Tap link → enter email → Firebase sends reset email → user resets via link → can log in with new password |
| 5.4b | **Google Sign-In** — Credential Manager + Web client ID + real Firebase Auth credential | ✅ Done & verified on-device (lands on Home, profile seeded from Google name/photo) | "Continue with Google" picks an account → lands on Home → user appears in Firebase Console |
| 5.2 | Schema migrations (Room v4 + `firebaseUid` + shared `conversationId`) | ✅ Done — `BUILD SUCCESSFUL`; additive `MIGRATION_3_4` (nullable `ADD COLUMN`s) preserves all rows | Existing data preserved on upgrade |
| 5.3 | Repositories + Firestore listeners (`UserRepository`, `MessageRepository`, `GalleryRepository`) | ✅ Code-complete — all 3 repos built, `assembleDebug` green. ⏳ on-device Firestore sync not yet manually verified | Profile edits + chats + gallery posts appear in Firestore Console |
| 5.5 | Security rules deploy | ✅ Rules written (`firestore.rules` + `firebase.json` + `.firebaserc`); ⏳ **deploy pending** (run `firebase deploy --only firestore:rules` or paste in Console) | Rules Playground blocks reads of another user's `users/{uid}` |
| 5.6 | Manual checkpoint testing | ⏳ Last | Cross-install sync verified per Checkpoint §5.6 |

> **What you can test right now:** email/password signup + login, "Forgot password?" reset email, **and Google Sign-In** all work end-to-end against Firebase Auth (users appear in Firebase Console → Authentication → Users). Signup auto-signs-out and bounces back to Login.

> **5.4b manual Firebase config — DONE ✅** (one-time, see `setupfirebase-google-signin.md`):
> 1. Debug SHA-1 `F2:D9:93:2F:4F:6C:DF:1F:EF:1D:EB:0D:1B:9C:7C:CC:C5:31:FD:DE` added to the Firebase Android app.
> 2. Google provider enabled in Authentication.
> 3. `google-services.json` re-downloaded — now has the Android (type 1) + Web (type 3) OAuth clients.
> 4. Web client ID `95395130249-bobte28q…apps.googleusercontent.com` set in `local.properties` → injected via `BuildConfig.GOOGLE_WEB_CLIENT_ID`.

---

### Design reference (locked)

### 5.1 Architecture — Offline-first Repository layer

```
Compose UI
   ↓
ViewModels (unchanged surface — Compose still observes mutableStateOf)
   ↓
Repositories (NEW) ── decide: read Room, write to both Room + Firestore
   ↓                 ↘
Room (local cache)   Firebase (Auth + Firestore)
```

- ViewModels keep their public API → screens stay untouched.
- Room remains the **read** source of truth → app works offline, no UI lag.
- Firestore is the **write-through** target + push source via real-time listeners.
- Auth state lives in `FirebaseAuth.currentUser` — replaces the `activeEmail` SharedPreference.

**New files:** `data/repository/AuthRepository.kt`, `UserRepository.kt`, `MessageRepository.kt`, `GalleryRepository.kt`, `FirestoreSchema.kt`
**Modified:** All 4 ViewModels, `LoginScreen.kt`, `SignupScreen.kt`, `RunTrackApplication.kt`, `app/build.gradle.kts`, root `build.gradle.kts`, `libs.versions.toml`

### 5.2 Firestore schema

```
users/{uid}
   ├─ email, runnerName, location, fitnessLevel, personalGoal, bio
   ├─ following (int), followers (int), photoUri (string?)
   └─ createdAt (timestamp)

users/{uid}/follows/{friendUid}      ← friendName, createdAt
users/{uid}/savedRoutes/{title}      ← distance, time, elevation, difficulty, imageRes
users/{uid}/activities/{activityId}  ← type, title, date, distanceKm, durationMinutes, elevationM, avgPace
users/{uid}/media/{mediaId}          ← caption, activity, distanceKm, tint, imageRes, imageUri, likes, createdAtMs

conversations/{conversationId}       ← top-level, shared between participants
   ├─ participants: [uid1, uid2, ...]
   ├─ isGroup, groupName
   └─ lastMessageAt
conversations/{conversationId}/messages/{messageId} ← senderUid, text, timestampMs
```

**Schema changes from current Room:**
- Primary key shifts from `email` to Firebase `uid` (emails can change, uids can't). Room adds a `firebaseUid` column.
- Conversations become top-level + shared (both participants reference the same `conversationId`).
- Follows reference uids (required for security rules).

**5.2 implementation — DONE ✅** (Room v3 → v4):
- `UserEntity` + `firebaseUid: String?` (indexed `index_users_firebaseUid`); kept `email` as the local primary key (offline cache + every DAO query stays keyed on email) — `firebaseUid` maps each row to its Firestore doc.
- `ConversationEntity` + `messages` each get `conversationId: String?` (shared top-level Firestore id, reconciled in 5.3).
- `UserData` model + VM mappers round-trip `firebaseUid` so `upsertUser` (REPLACE) can't clobber it.
- `UserDao.findByFirebaseUid` / `setFirebaseUid` accessors added (5.3 backfills on cloud sign-in).
- `AppDatabase` bumped to **version 4** with a real `MIGRATION_3_4` (three additive nullable `ADD COLUMN`s + the uid index) registered ahead of the destructive fallback → **existing data preserved on upgrade**.

### 5.3 Sync strategy

- **Read:** Room → UI (instant). Firestore listener → Room → UI auto-refreshes via Flow.
- **Write:** Repository writes to Room first, then to Firestore. Offline writes are queued + retried by Firestore SDK automatically.
- **Login on new device:** Repository pulls all user docs from Firestore → seeds Room → starts listeners.

**5.3 implementation — UserRepository slice DONE ✅** (`BUILD SUCCESSFUL`, on-device sync not yet manually verified):
- New `data/cloud/FirestoreSchema.kt` — collection-name constants (`FirestoreCollections`) + DTOs (`UserDoc`, `FollowDoc`, `SavedRouteDoc`, `ActivityDoc`, `MediaDoc`, `ConversationDoc`, `MessageDoc`). All DTO fields default-valued so Firestore's `toObject` no-arg mapping works.
- New `data/repository/UserRepository.kt` — **full ownership** of the user domain (profile, follows, saved routes): Room is the read source of truth (exposes `observe*` Flows), writes are write-through (Room → Firestore `users/{uid}` + `follows/`, `savedRoutes/` sub-collections), `firebaseUid` is backfilled on sign-in, and `users/{uid}` + `follows/*` snapshot listeners fold cloud changes back into Room. On sign-in the cloud doc wins (fresh-install pull); `propagateRename` (display-name fan-out) moved here.
- `UpdateUserViewModel.kt` refactored to hold **no DAO refs** — delegates all persistence to `UserRepository`; auth stays in `AuthRepository`. Public VM surface (state + method signatures) unchanged → screens untouched. `applyActiveSession()` mirrors state + starts Room observers + cloud listeners; `logout()` tears both down.
- `RunTrackApplication` exposes `userRepository` (lazy singleton).
**5.3 implementation — MessageRepository slice DONE ✅** (`BUILD SUCCESSFUL`, on-device sync not yet manually verified):
- New `data/repository/MessageRepository.kt` — owns conversation + message persistence. Local Room rows stay keyed by (ownerEmail, friendName); the shared cloud model is top-level `conversations/{conversationId}` (+ `messages` sub-collection) with `participants` as uids.
- **Bridge decisions:** 1:1 `conversationId` is derived **deterministically from the two sorted uids** (both devices compute the same id, no lookup); groups use a generated UUID stored on the local row. A message is written to Room and Firestore under the **same id**, so the listener echo is idempotent — this let me **drop the old single-device "mirror into the recipient's rows" hack**; real cross-user delivery now flows through Firestore.
- **Sync:** `startSync` listens to `conversations whereArrayContains participants == myUid`, reconciles each into a local `ConversationEntity` (resolving the other participant's display name from local cache → Firestore `users/{uid}` fallback), and attaches a per-conversation `messages` listener that upserts `MessageEntity` with `fromMe = senderUid == myUid`.
- Users without a `firebaseUid` (seeded demo runners) have no cloud peer → those conversations stay **local-only**, so demos/offline still work.
- New DAO query `MessageDao.findConversationByCloudId(conversationId)` maps a shared cloud id back to the local row.
- `MessageViewModel` refactored to hold **no DAO refs** — delegates all persistence to `MessageRepository`; `startSync` runs on `setActiveUser`, torn down on `clearActiveUser`. Public surface unchanged.
- `RunTrackApplication` exposes `messageRepository` (lazy singleton).

**5.3 implementation — GalleryRepository slice DONE ✅** (`assembleDebug` green, on-device sync not yet manually verified):
- New `data/repository/GalleryRepository.kt` — owns reel persistence. Reads from Room (`observeFeed` / `observeMine` / `observeByAuthor`).
- **Own reels** sync write-through to `users/{uid}/media/{id}` (same id in Room + Firestore → idempotent listener echo); `startMineSync` pulls them back so a fresh install restores *your* reels.
- **Scope decision:** the cross-user feed stays **Room-backed + locally-seeded demo authors**, because the §5.5 owner-scoped rules (only the owner reads their own `users/{uid}` subtree) forbid reading other users' media. A real cross-user social feed would need a public top-level `media` collection or follow-based fan-out — deferred.
- **Image limitation:** `imageRes` (drawable id) and `imageUri` (content://) are device-local, so synced reels carry text/stats across devices but not the picture. Real cloud images = Phase 5 (Firebase Storage).
- `GalleryViewModel` refactored to hold **no DAO refs** — delegates persistence to `GalleryRepository`; keeps the local demo-seed scaffolding (gated by the `galleryDemoSeeded` pref) and feeds reels into state via the chosen repo flow. Public surface unchanged.
- `RunTrackApplication` exposes `galleryRepository` (lazy singleton).

**All three 5.3 repositories are now code-complete and the app assembles.** Remaining for Phase 3: **5.5** (deploy security rules) and **5.6** (manual cross-install checkpoint testing).

**5.3a Cross-device discovery — DONE ✅ (`assembleDebug` green; needs rules redeploy + on-device test):**
Fixes "users created on one device don't appear in Search / Gallery on another." Root cause: `otherUsers` (Search) and the Gallery feed read **only local Room**, and owner-only `users/{uid}` rules forbid reading other accounts. Added public, any-authed-user-readable directories:
- New Room table **`user_directory`** (`UserDirectoryEntity`, keyed by uid) — Room **v4 → v5** with additive `MIGRATION_4_5` (CREATE TABLE, data preserved). DAO gains directory upsert/observe/`findDirectoryByName`.
- Firestore **`publicProfiles/{uid}`** (public slice: name, location, fitnessLevel, photoUri) — written by `UserRepository.pushProfile` (so every profile save) and on every sign-in (`onSignIn` now always republishes, idempotent merge). A `publicProfiles` listener mirrors all other users into `user_directory`. `observeOtherUsers(email, myUid)` merges local users + directory (deduped by uid/name) → Search shows everyone.
- Firestore **`publicReels/{mediaId}`** — `GalleryRepository.createPost` writes a public copy alongside `users/{uid}/media`; `startFeedSync` folds *other* users' reels into Room (skipping my own; stored under synthetic `ownerEmail = "uid:<ownerUid>"` so they don't pollute my "my posts"). Wired into `GalleryViewModel.showFeed`.
- `MessageRepository` uid resolution (`uidForName`) now falls back to the directory, so you can chat/group with users discovered via Search.
- `firestore.rules`: `publicProfiles/{uid}` + `publicReels/{mediaId}` — read by any signed-in user, write only by the owner (`ownerUid`/uid == `auth.uid`). Private `users/{uid}` stays owner-only (5.6 #5 still holds).
- **Requires:** redeploy `firestore.rules` (new collections will be denied otherwise), and each existing user must sign in once so their `publicProfiles` entry is created. Images still don't cross devices until Phase 5 (Storage); reels carry text/stats only.
- **Not yet verified:** actual Firestore writes/reads on a device + Console (needs a network + Firebase Console check). For chat sync specifically, the real test is **two devices/emulators signed in as different real accounts** (demo runners can't sign in, so they don't sync).

### 5.4 Auth flow — **Email/Password (Option A)**

> Passwordless email-link was considered but rejected — Firebase Dynamic Links is deprecated (shutdown Aug 2025) and the alternative requires custom domain + App Links plumbing that's overkill for this app.

**LoginScreen changes:**
- Add password `OutlinedTextField` (show/hide toggle, `KeyboardType.Password`).
- `isValid` checks email format AND password length ≥ 6 (Firebase minimum).
- "Continue" calls `authRepository.signIn(email, password)`.
- Error handling: `Snackbar` for wrong-password / user-not-found / network-error.
- Remove the hardcoded `hasif@gmail.com` fallback.
- Google Sign-In button stays visible but disabled with "Coming soon" tooltip (deferred to a later phase).

**SignupScreen changes:**
- Add password + confirm password fields. Validate match + length ≥ 6.
- Call `authRepository.signUp(email, password, name)`:
  1. `auth.createUserWithEmailAndPassword(...)`
  2. Write `UserData` to Firestore `users/{uid}`.
  3. Mirror to Room with the new `firebaseUid`.
  4. Auto sign-in → navigate Home.

**Session restore:**
- `RunTrackApplication.onCreate()` checks `FirebaseAuth.getInstance().currentUser`.
- User present → start on Home, begin syncing. Null → start on Login.
- Logout → `auth.signOut()`. Room data stays (instant login next time on same device).

**Real-time listeners** (started on login, stopped on logout):
- `users/{uid}` — my profile.
- `users/{uid}/follows/*` — my follow list.
- `conversations where participants contains uid` — my chats.
- Per visible conversation → `messages` (limit last 50).

### 5.4a Signup → Login redirect — DONE ✅

Originally signup auto-navigated to Home (Firebase auto-signs in the new user after `createUserWithEmailAndPassword`). Changed so the user has to log in explicitly:

- After `userViewModel.registerUser(...)` succeeds in `MainActivity.kt`, call `app.authRepository.signOut()` to clear the auto-sign-in.
- Show toast: "Account created. Please log in."
- Navigate to `Screen.Login.route` with `popUpTo(Login) { inclusive = true }` so Signup is removed from the back stack.

### 5.4c Forgot password — email reset flow — DONE ✅

> Firebase Auth ships a built-in password reset flow via `sendPasswordResetEmail(email)`. No custom backend needed — Firebase sends the reset email and hosts the reset page. **Verified end-to-end: reset email received, new password works.**

**LoginScreen changes:**
- Add a "Forgot password?" `TextButton` below the password field, aligned to the end.
- Tapping it opens a `ForgotPasswordDialog` (small `AlertDialog`):
  - Single email `OutlinedTextField` (pre-filled with whatever's already typed in the Login email field).
  - "Send reset link" button → calls `userViewModel.sendPasswordReset(email)`.
  - "Cancel" button dismisses.
- On success → toast "Reset email sent. Check your inbox." + close dialog.
- On failure → toast with mapped Firebase error (e.g. "No account found for that email").

**AuthRepository changes:**
- Add `suspend fun sendPasswordReset(email: String): Result<Unit>` wrapping `auth.sendPasswordResetEmail(email).await()`.
- Map `FirebaseAuthInvalidUserException` → "No account found for that email" (already in `mapAuthError`).
- Map `FirebaseAuthInvalidCredentialsException` → "Invalid email format".

**UpdateUserViewModel changes:**
- Add `fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit)` that launches a coroutine, calls `authRepository.sendPasswordReset(email)`, and forwards the result.

**MainActivity wiring:**
- Pass an `onForgotPassword: (String, (Boolean, String?) -> Unit) -> Unit` callback into `LoginScreen` that calls `userViewModel.sendPasswordReset(...)`.

**Flow:**
1. User taps "Forgot password?" on Login
2. Dialog opens with email pre-filled
3. Tap "Send reset link"
4. Firebase emails a reset link (template editable in Firebase Console → Authentication → Templates)
5. User opens the link, sets a new password on Firebase's hosted page
6. User returns to the app, logs in with the new password

**Checkpoint:** Tap "Forgot password?" → enter registered email → receive email → reset → log in successfully with new password.

### 5.4b Google Sign-In (Credential Manager)

> Google deprecated the legacy `GoogleSignIn` API. We use **Credential Manager + Google ID helper** — the current recommended path.

**Prerequisites (one-time, manual):**

1. **Get debug SHA-1 fingerprint** — run `./gradlew :app:signingReport` and copy the `SHA1` value under `Variant: debug`.
2. **Add SHA-1 to Firebase Console:**
   - Project Settings → Your apps → Android app → **Add fingerprint** → paste SHA-1 → Save.
3. **Re-download `google-services.json`** from Project Settings → replace the one in `app/`.
4. **Copy Web client ID** — Authentication → Sign-in method → Google → expand → **Web SDK configuration** → copy the `Web client ID` (looks like `123456789-abc...apps.googleusercontent.com`). Store it in `local.properties` as `GOOGLE_WEB_CLIENT_ID=...` (gitignored).

**Code changes:**

| File | Change |
|---|---|
| `libs.versions.toml` + `app/build.gradle.kts` | Add `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid` |
| `app/build.gradle.kts` | Inject `GOOGLE_WEB_CLIENT_ID` from `local.properties` into `BuildConfig` via `buildConfigField` |
| `AuthRepository.kt` | Add `suspend fun signInWithGoogle(idToken: String): Result<AuthUser>` — wraps `GoogleAuthProvider.getCredential(idToken, null)` + `auth.signInWithCredential(...)` |
| New `GoogleSignInHelper.kt` | Wraps `CredentialManager.getCredential(GetGoogleIdOption)` — returns the ID token. Activity-scoped because Credential Manager needs an Activity context. |
| `UpdateUserViewModel.kt` | `loginWithGoogle(idToken, onResult)` — call `authRepository.signInWithGoogle`, then mirror profile to Room (same flow as email login). Display name comes from Google account. |
| `LoginScreen.kt` | "Continue with Google" button: launches `GoogleSignInHelper` via a `LaunchedEffect`/coroutine. On success → forwards ID token to ViewModel. Disable button while in-flight. |
| `MainActivity.kt` | Wire `onGoogleSignIn` to the helper instead of the "coming soon" Toast. |

**Flow:**

1. User taps "Continue with Google"
2. `CredentialManager` opens the system account picker
3. User selects their Google account
4. Helper returns a Google **ID token**
5. ViewModel sends it to `AuthRepository.signInWithGoogle(idToken)`
6. Firebase verifies the token → returns `FirebaseUser`
7. ViewModel mirrors the profile to Room → navigates Home

**Failure cases handled:**
- User cancels picker → no-op (no Toast spam)
- No Google account on device → Toast "No Google account found"
- Token invalid / network error → Toast with Firebase error message
- SHA-1 missing → `DEVELOPER_ERROR` → Toast "Google Sign-In not configured" + Logcat hint to add SHA-1

**Checkpoint:** Tap Google button → account picker → pick account → land on Home. Same email in Firebase Console (linked as Google provider, not Email/Password). **✅ Verified on-device.**

#### 5.4b — what actually shipped (after debugging on a physical Realme device)

A few things changed from the original design above during implementation + on-device debugging:

- **`GetSignInWithGoogleOption` instead of `GetGoogleIdOption`.** `GoogleSignInHelper` uses the button-driven flow, which always shows the full account picker. `GetGoogleIdOption` (the seamless/bottom-sheet flow) threw `NoCredentialException` on an explicit button press before any account was authorized — that was the first bug.
- **`AuthUser` carries `displayName` + `photoUrl`;** `loginWithGoogle` seeds a new user's `runnerName` and profile `photoUri` from the Google account.
- **`loginWithGoogle` guards the post-sign-in hydration** in try/catch so `onResult` always fires — a Room error after sign-in can no longer leave the UI stuck on Login with no feedback.
- **Diagnostics:** failures log to Logcat tag `GoogleSignIn` and show a `LENGTH_LONG` toast.
- **`MainActivity`** wires `onGoogleSignIn` to launch the helper in `rememberCoroutineScope()`, handling cancel / no-credential / not-configured / generic errors.

**Network gotcha (not a code bug):** on restricted WiFi (e.g. campus/office AP doing TLS inspection), Google Play Services can't hold the HTTP/2 connection to Google's auth servers → `net::ERR_HTTP2_PING_FAILED` / `UNAVAILABLE`, the GMS flow aborts, and the app bounces back to Login. **Fix: use mobile data or an unrestricted network** for Google sign-in. Also worth checking on ColorOS/Realme: don't battery-restrict Google Play services; set date & time automatically. Confirmed working on mobile data.

---

### 5.5 Security rules

- `users/{uid}/**` — read+write only if `request.auth.uid == uid`.
- `conversations/{id}` — read+write only if `request.auth.uid in resource.data.participants`.

**5.5 implementation — rules written ✅, deploy pending ⏳:**
- New `firestore.rules` (rules_version 2):
  - `users/{uid}` + all sub-collections (`follows`, `savedRoutes`, `activities`, `media`) — **owner-only** (`request.auth.uid == uid`). Satisfies checkpoint §5.6 #5 (another user's `users/{uid}` is unreadable).
  - `conversations/{id}` — **participants-only**; `create` validates `request.auth.uid in request.resource.data.participants` (since `resource` is absent on create).
  - `conversations/{id}/messages/{mid}` — read if you're a participant (via `get()` on the parent); `create` additionally requires `senderUid == request.auth.uid` (no spoofing); `update`/`delete` denied (messages immutable).
- New `firebase.json` (points `firestore.rules`) + `.firebaserc` (default project `enerva-be887`) → deploy with `firebase deploy --only firestore:rules`, or paste the file into Firebase Console → Firestore → Rules → Publish.
- **Code interaction handled:** owner-only user docs mean a chat participant can't read the other user's profile to get their display name. Fixed by **denormalising `participantNames` (uid → name) onto the conversation doc** — `ConversationDoc` gained the field; `MessageRepository.sendMessage`/`createGroup` write it; `reconcileConversation` reads names from there (`nameForParticipant`) instead of fetching `users/{uid}`. So profiles stay private *and* chat labels resolve. (`BUILD SUCCESSFUL`.)

### 5.6 Checkpoint
Data syncs across installs; logging in on a fresh device pulls profile + reels + chats from cloud; offline writes queue and replay on reconnect.

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

Phase 0 (rename ✅) → Phase 1 (trim ✅) → Phase 2 (Room ✅) → **Phase 3 (Firebase) ← next** → Phase 4 (Maps) → Phase 5 (Camera).

Each phase ends buildable. Phases 3 & 4 need credentials from you (Firebase project file, Maps API key).
