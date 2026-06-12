# Profile Improvements — Design

Date: 2026-06-12
Status: Approved (pending spec review)

## Goal

Four related improvements to the profile + gallery experience:

1. **Multi-select delete** in the user's own profile gallery grid — long-press to
   pick posts, then delete.
2. **Tap a reel's name/avatar bar** in the public Gallery feed to open that user's
   profile.
3. **Other users' profiles** reuse the Profile layout but expose **only a
   Follow/Following button** — no Edit Profile, no Logout, no Saved section, and
   their gallery tiles open read-only (no delete).
4. **Real profile pictures in reels**, visible cross-device to other users.

Scope note: real accounts only. Demo seeded authors (Sarah/Daniel/Aisha) have no
real account, so they fall back to a person icon and seeded data — no special work.

## Background (current state)

- `ProfileScreen` is hardwired to the current user (`userViewModel.userProfile`)
  and always shows Logout + Edit Profile + Follow. There is no route to view
  another user's profile; tapping a gallery tile opens `userGalleryRoute` (a
  read-only reels view) only.
- `ReelPage` shows the author name but the avatar is a flat gray circle, and the
  name/avatar bar is not clickable.
- No delete exists anywhere; no multi-select in the profile grid.
- Reel/profile DTOs carry no author photo. `photoUri` is a device-local
  `content://` URI that does not load on other devices. The project already
  worked around this for reel **images** by storing a compressed JPEG as a
  Firestore `Blob` and decoding it to a cache file on receive
  (`GalleryRepository`). This design mirrors that approach for avatars.

## Design

### 1. Cross-device avatars (foundation)

Avatars are stored once on the profile and joined by display name where needed —
no per-reel denormalization.

- `PublicProfileDoc` gains `photoBlob: Blob? = null`.
- `UserRepository` takes a `cacheDir: File` (mirroring `GalleryRepository`), wired
  from `RunTrackApplication`.
- **Upload:** when the user picks a *new* avatar in Edit Profile, the UI compresses
  it to a small JPEG (~256px, reusing the reel-image compression approach) and
  passes the bytes through:
  `updatePhotoUri(uri, bytes)` → `saveProfile(user, avatarBytes)` →
  `pushProfile(user, avatarBytes)`.
  - `pushProfile` writes `publicProfiles/{uid}` with `SetOptions.merge()` and only
    includes `photoBlob` when `avatarBytes != null`. **This also changes the
    existing `publicProfiles` write from a full `.set(...)` to a merge**, so a
    later name/bio edit can no longer wipe the stored avatar blob.
- **Download:** the `publicProfiles` snapshot listener in
  `UserRepository.startListeners` decodes any `photoBlob` to
  `cacheDir/remote_avatars/<uid>.jpg` and stores that absolute path in
  `UserDirectoryEntity.photoUri`. When no blob is present, fall back to the doc's
  `photoUri` (unchanged behaviour).
  - Idempotent on uid; only rewrites the cache file when the blob is present.

Because `observeOtherUsers` already feeds `userViewModel.otherUsers` from the
directory, real photos now flow to Search, reels, and other-user profiles
cross-device with no further plumbing.

### 2. Real avatars in reels

- In `ReelPage`, replace the gray `Box` with an `AsyncImage` whose `model` is the
  resolved author photo, falling back to a person icon when null.
- New `UserViewModel.photoForAuthor(name: String): String?`:
  - returns `userProfile.photoUri` when `name == userProfile.runnerName`,
  - else `otherUsers.firstOrNull { it.runnerName == name }?.photoUri`,
  - else `null` (icon fallback).
- `GalleryScreen` computes the photo per reel and passes it into `ReelPage`.

### 3. Tap name/avatar → that user's profile

- New nav destination `Screen.UserProfile = "profile/user/{authorName}"` and a
  `userProfileRoute(name)` builder (URL-encoded, mirroring `userGalleryRoute`).
- `MainActivity` adds the composable, decoding `authorName` and passing it into
  `ProfileScreen`.
- The reel author `Row` (avatar + name) becomes clickable:
  - if `reel.author == myName` → navigate to `Screen.Profile`,
  - else → `userProfileRoute(reel.author)`.
  `GalleryScreen` passes an `onAuthorClick` lambda into `ReelPage`.

### 4. `ProfileScreen` — dual mode (self vs. other)

`ProfileScreen` gains `authorName: String? = null`.

- **null (self):** unchanged top bar (Gallery + Logout actions), Edit Profile +
  Follow-of-self omitted as today, Saved section present, plus the new
  multi-select grid (section 5). Profile data = `userViewModel.userProfile`.
- **non-null (other):** resolve the target `UserData` by name — prefer
  `userViewModel.otherUsers.firstOrNull { it.runnerName == authorName }`, fall back
  to `userViewModel.findUserByName(authorName)`. Render:
  - avatar + name + location (+ bio if present),
  - stats row from whatever the resolved `UserData` carries (directory-only users
    may show 0 — acceptable),
  - **only** a Follow/Following button (same toggle as the reel button, seeding /
    dropping a conversation to match `GalleryScreen`/`SearchScreen`),
  - **no** Edit Profile button, **no** Saved section, and the top bar shows only a
    back button (no Gallery/Logout actions),
  - their gallery grid (their posts) with tiles opening the existing read-only
    `userGalleryRoute` — **no selection / delete**.

The gallery grid is driven the same way as today
(`galleryViewModel.showMyPosts(...)` for self; `showAuthorGallery(name)` for
other), reading `galleryViewModel.reels`.

### 5. Multi-select delete (self gallery grid only)

- `ProfileScreen` (self mode) holds selection state:
  `selectionMode: Boolean`, `selectedIds: Set<String>` (remembered Compose state).
- Interactions:
  - long-press a tile → enter selection mode and select it,
  - tap in selection mode → toggle that tile,
  - tap outside selection mode → open reels (today's behaviour).
- A contextual action bar (replacing/overlaying the Gallery section header while
  active) shows "N selected" with **Delete** and **Cancel (X)**. Selected tiles
  get a check overlay + dim.
- **Delete** opens an `AlertDialog` confirm ("Delete N post(s)?"). On confirm →
  `galleryViewModel.deletePosts(selectedIds)` then exit selection mode.

#### Delete data flow

- `GalleryViewModel.deletePosts(ids: Set<String>)` → `viewModelScope.launch {
  repository.deletePosts(activeEmail, ids) }`.
- `GalleryRepository.deletePosts(email, ids)`:
  - `activityDao.deleteMediaByIds(ids)` (new DAO method),
  - resolve `uid = userDao.findByEmail(email)?.firebaseUid`; for each id delete
    `users/{uid}/media/{id}` and `publicReels/{id}` (best-effort `runCatching`),
  - delete the decoded cache file `cacheDir/remote_reels/<id>.jpg` if present.
- `ActivityDao` gains
  `@Query("DELETE FROM media WHERE id IN (:ids)") suspend fun deleteMediaByIds(ids: List<String>)`.

#### Listener removal handling

`startMineSync` and `startFeedSync` currently only *insert* on snapshots, so a
remote deletion would never remove the local Room row (the post would reappear on
the owner's other devices and linger in other users' feeds). Both listeners will
iterate `snapshot.documentChanges` and, on `DocumentChange.Type.REMOVED`, call
`activityDao.deleteMediaByIds(listOf(removedId))` (and remove the cache file). The
device performing the delete already removed the row locally; this keeps the
others consistent.

## Touched files

- `data/cloud/FirestoreSchema.kt` — `photoBlob` on `PublicProfileDoc`.
- `data/repository/UserRepository.kt` — `cacheDir`; avatar blob upload (merge) +
  decode-on-receive; `saveProfile`/`pushProfile` signatures.
- `RunTrackApplication.kt` — pass `cacheDir` into `UserRepository`.
- `view_model/UserViewModel.kt` — `updatePhotoUri(uri, bytes)`,
  `photoForAuthor(name)`.
- `view/screen/EditProfile.kt` — compress picked avatar → bytes → `updatePhotoUri`.
- `data/dao/ActivityDao.kt` — `deleteMediaByIds`.
- `data/repository/GalleryRepository.kt` — `deletePosts`; REMOVED handling in both
  sync listeners.
- `view_model/GalleryViewModel.kt` — `deletePosts(ids)`.
- `view/Navigation.kt` — `Screen.UserProfile` + `userProfileRoute`.
- `view/MainActivity.kt` — UserProfile composable route.
- `view/screen/ProfileScreen.kt` — dual mode + multi-select grid.
- `view/screen/GalleryScreen.kt` — real reel avatar + clickable author bar.
- Shared image-compression util — extract/reuse `compressReelImage`, add
  `compressAvatarImage`.

## Out of scope

- Demo seeded authors stay icon-fallback with seeded data.
- Avatar blob re-uploads only when a new photo is picked, not on plain sign-in.
- No comments/share work (existing TODOs untouched).
