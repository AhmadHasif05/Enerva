# Phase 3 — Firebase Auth + Cloud Firestore (Design Spec)

**Date:** 2026-05-30
**Status:** Approved, implementation in progress
**Companion doc:** `plan.md` §5 (full design lives there)

---

## Goal

Replace the in-memory + Room-only auth/data layer with **Firebase Auth (email/password)** for accounts and **Cloud Firestore** for cross-device data sync. Keep the offline-first UX by routing all reads through Room.

## Decisions (locked)

| Topic | Decision | Why |
|---|---|---|
| Auth method | Email + password | Standard, secure, simple. Email-link was rejected — Firebase Dynamic Links is deprecated (shutdown Aug 2025). |
| Google Sign-In | Deferred | Provider is enabled in Firebase, but wiring needs SHA-1 + web client ID. Button stays visible but disabled. |
| Architecture | Repository layer between ViewModels and (Room + Firestore) | ViewModel public API unchanged → screens untouched. |
| Read source of truth | Room | Instant UI, works offline. Firestore listeners write *into* Room. |
| Write strategy | Write-through (Room → Firestore) | Firestore SDK queues offline writes automatically. |
| Primary key | Firebase `uid` (was `email`) | Emails can change, uids can't. Room adds `firebaseUid` column. |
| Conversations | Top-level shared docs (was per-user rows) | Required so both participants see the same chat. |
| Security | Owner-scoped rules — `request.auth.uid == uid` | Minimum viable; tighten later if needed. |

## Scope — In

- Firebase project setup + `google-services.json` (✅ done by user, see `setupfirebase.md`)
- Gradle: `google-services` plugin, Firebase BoM, Auth + Firestore deps
- New `AuthRepository`, `UserRepository`, `MessageRepository`, `GalleryRepository`
- LoginScreen + SignupScreen: add password field(s)
- ViewModels refactored to call repos instead of DAOs directly
- Session restore via `FirebaseAuth.currentUser` (replaces SharedPreferences)
- Firestore real-time listeners for profile, follows, chats, messages
- Basic Firestore security rules

## Scope — Out (deferred to later phases)

- Real Google Sign-In (needs SHA-1, web client ID, Credential Manager)
- Firebase Storage for media uploads (Phase 5 with camera)
- Password reset flow
- Email verification gate
- Multi-device conflict resolution beyond Firestore defaults

## File-level impact

**New files:**
- `data/repository/AuthRepository.kt`
- `data/repository/UserRepository.kt`
- `data/repository/MessageRepository.kt`
- `data/repository/GalleryRepository.kt`
- `data/cloud/FirestoreSchema.kt` (collection name constants + DTOs)
- `firestore.rules` (security rules, deployable via Firebase CLI later)

**Modified:**
- `libs.versions.toml` — add Firebase BoM, Auth, Firestore, google-services plugin
- root `build.gradle.kts` — register google-services plugin
- `app/build.gradle.kts` — apply google-services plugin, add deps
- `RunTrackApplication.kt` — initialize Firebase, expose repos
- `data/entities/Entities.kt` — `UserEntity` gains `firebaseUid` column; `ConversationEntity` gains `conversationId`
- `data/AppDatabase.kt` — bump version to 4
- `view_model/UpdateUserViewModel.kt` — delegate to `AuthRepository` + `UserRepository`
- `view_model/UpdateLoginScreen.kt` — add password state + validation
- `view_model/UpdateSignupScreen.kt` — add password + confirm
- `view_model/MessageViewModel.kt` — delegate to `MessageRepository`
- `view_model/GalleryViewModel.kt` — delegate to `GalleryRepository`
- `view/screen/LoginScreen.kt` — password field, error Snackbar
- `view/screen/SignupScreen.kt` — password + confirm fields
- `view/MainActivity.kt` — startDestination derived from `FirebaseAuth.currentUser`

## Checkpoint (Phase 3 complete when)

1. New user can sign up with email + password → record appears in Firestore Console.
2. Logging in on a fresh install pulls profile + media + chats from Firestore into Room.
3. Two emulators logged in as different users can chat in real time.
4. Closing the app while offline, sending a message, then reconnecting → message appears in Firestore.
5. Security rules block reading another user's `users/{uid}` doc (verify in Firebase Console "Rules Playground").

## Implementation order

1. Gradle wiring (deps + plugin) — compile check.
2. `AuthRepository` + LoginScreen/SignupScreen password fields — sign up + sign in working against Firebase Console.
3. `UserRepository` + Firestore profile sync — profile edits reach Firestore.
4. Real-time listeners + Room mirror — fresh-install pulls cloud data.
5. `MessageRepository` + conversation refactor — two users can chat.
6. `GalleryRepository` — media syncs.
7. Firestore security rules — deploy and verify in Rules Playground.
8. Manual checkpoint testing.
