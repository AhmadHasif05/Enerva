# Design: Cross-device reel images via Firestore image blob

> **Status:** Approved (2026-06-12). Closes the Phase 5 image-sync gap. Phase 5's
> camera capture, local save, and local-gallery display already shipped; the only
> missing piece is making the posted image travel to other devices.

## Problem

A posted reel currently syncs its **text/stats** across devices but **not the
picture**. The image is saved as a device-local path (`MediaEntity.imageUri` → an
absolute file path under `filesDir/runs`, or a `content://` URI for a picked
photo) and that string is meaningless on any other device. So a reel created on
phone A shows the branded run-summary card on A, but falls back to the generic
drawable (`imageRes`) on phone B.

The intended Phase 5 fix was Firebase Storage. **Firebase Storage is unavailable**
on this project: new Firebase projects require the pay-as-you-go Blaze plan to
provision a Cloud Storage bucket, and this project has no billing card (the same
constraint that drove the Google Maps → OpenFreeMap switch in Phase 4).

## Approach

Store the compressed reel image as a **Firestore `Blob`** field on the reel
documents, right next to the text/stats that already sync. This rides the existing
write-through + snapshot-listener path in `GalleryRepository`, needs no new
service, account, API key, or billing, and stays within the free Firestore tier.

The trade-off is Firestore's **1 MB per-document** hard limit. A run-summary card
is roughly square and already downscaled; compressed to JPEG it is typically
50–200 KB. The compression helper enforces a **900 KB cap** (downscaling as needed)
so we are provably under the ceiling, with headroom for the other doc fields.

### Considered and rejected
- **Free third-party image host (Cloudinary / Supabase Storage):** "real" CDN
  hosting and would finally exercise the unused Retrofit stack, but adds an
  external account, an API key to manage, and new network code. More moving parts
  than this project needs.
- **Do not sync images:** defeats the purpose of the Phase 5 checkpoint.

## Components & changes

### 1. Schema — `data/cloud/FirestoreSchema.kt`
Add two fields to **both** `MediaDoc` (`users/{uid}/media/{id}`) and
`PublicReelDoc` (`publicReels/{id}`):

| Field | Type | Why |
|-------|------|-----|
| `imageBlob` | `com.google.firebase.firestore.Blob? = null` | The compressed JPEG bytes of the reel image. |
| `isCard` | `Boolean = false` | Currently device-local. Now that the image syncs, a remote run-summary card must render `ContentScale.Fit` (whole frame, footer not clipped) instead of `Crop`. |

Both keep a default value so Firestore's no-arg POJO mapping still works (matches
the existing DTO convention documented at the top of the file).

### 2. Compression helper — `view/screen/RouteSnapshot.kt`
A pure `Bitmap → ByteArray?` function alongside the existing image utilities:

```
fun compressReelImage(bitmap: Bitmap, maxBytes: Int = 900_000): ByteArray?
```

- JPEG-encode; if over `maxBytes`, step quality down (e.g. 80 → 40) and, if still
  over, halve the longest edge and retry.
- Returns the bytes once under the cap, or `null` if it cannot fit (caller then
  posts with no blob and the reel falls back to the drawable on other devices).

### 3. Write path — encode at the call sites, store in the repo

> **Correction to the original draft (found during planning):** the Record path
> does **not** currently flow through `GalleryRepository.createPost`.
> `RecordViewModel.saveActivity` writes the reel *directly* to the DAO
> (`activityDao.insertMedia`) and never touches Firestore — so **run-summary posts
> have never synced to the cloud at all**, image or not. Only the CreatePostDialog
> path (`GalleryViewModel.createPost`) does write-through. This fix therefore also
> closes that latent gap by routing the Record post through the cloud write.

To avoid duplicating the Firestore-write logic, extract it from `createPost` into a
reusable repository method:

```
suspend fun pushMediaToCloud(entity: MediaEntity, imageBytes: ByteArray?)
```

It looks up the owner's `firebaseUid` (by `entity.ownerEmail`), wraps
`imageBytes` as `Blob.fromBytes(...)`, and writes `entity.toDoc(blob)` to
`users/{uid}/media/{id}` + `entity.toPublicReel(uid, blob)` to `publicReels/{id}`
inside the existing best-effort `runCatching`. `createPost` is refactored to build
its entity, insert to Room, then call `pushMediaToCloud`.

`ByteArray?` (not a Firestore `Blob`) is threaded from the UI/VM down, so
**Firestore types stay confined to the data layer** and the VMs remain
Android/Firestore-free.

- **Record path:** `RecordScreen.onPost` already has the `cardBitmap`. Encode it
  with `compressReelImage` and pass the bytes through
  `RecordViewModel.saveActivity(... imageBytes)`, which (after the existing
  `activityDao.insertMedia`) calls `galleryRepository.pushMediaToCloud(media,
  imageBytes)`. `RecordViewModel` gains a `galleryRepository` reference (from
  `RunTrackApplication`).
- **CreatePostDialog path:** `GalleryScreen` has the picked `content://` URI and a
  `Context`. Decode the URI to a bitmap, encode it, and pass the bytes through
  `GalleryViewModel.createPost(... imageBytes)` → `createPost`. This makes raw
  photo posts sync too, not just run-summary cards.
- The local `MediaEntity` is unchanged (still stores only the local path).

### 4. Receive / display path — `data/repository/GalleryRepository.kt`
Add a `cacheDir: File` constructor parameter (wired from `RunTrackApplication`,
which constructs the repo). Both listeners — `startMineSync` and `startFeedSync` —
when a doc arrives with a non-null `imageBlob`:

1. Decode the blob bytes to `cacheDir/remote_reels/<id>.jpg` (write once;
   idempotent on the reel id).
2. Fold a `MediaEntity` into Room with `imageUri` = that cache-file path and
   `isCard` carried from the doc.

The existing renderer `AsyncImage(model = reel.imageUri ?: reel.imageRes)` in
`GalleryScreen.ReelPage` then displays the synced image with **no change**.

**Own-post guard:** an own post already inserted with a crisp local PNG path must
not be clobbered by its own `startMineSync` echo. When a row for that id already
exists with a non-null local `imageUri`, keep the existing path rather than
re-decoding the blob to cache.

### 5. Explicitly unchanged
- `MediaEntity` / Room schema: no new column, **no DB version bump**. Room stays
  lean; image bytes live only in Firestore + the on-device cache file.
- `GalleryScreen.ReelPage` renderer: unchanged (the `imageUri`/`isCard` it already
  reads now arrive populated for remote reels).
- Security rules: `publicReels` write is already owner-only (`ownerUid ==
  auth.uid`); a blob is just another field, so **no rule change**. (The separate,
  pre-existing rules *deploy-pending* item from Phase 3 §5.5 is unrelated to this
  work.)

## Data flow (post → cross-device)

```
Phone A: End run → RunSummarySheet Post
  → cardBitmap → compressReelImage() → bytes
  → saveActivity(imageBytes=bytes)
      → Room: MediaEntity (local PNG path, isCard)            [instant local display]
      → pushMediaToCloud(media, bytes):
          → Firestore users/{uid}/media/{id}: MediaDoc(imageBlob, isCard)
          → Firestore publicReels/{id}: PublicReelDoc(imageBlob, isCard)

Phone B: publicReels listener fires
  → PublicReelDoc(imageBlob) → write cacheDir/remote_reels/{id}.jpg
  → Room: MediaEntity(imageUri = cache path, isCard)
  → AsyncImage renders the synced card (Fit because isCard)
```

## Error handling
- `compressReelImage` returns `null` if it cannot get under the cap → post
  proceeds with no blob; the reel uses the drawable fallback on other devices
  (same as a stats-only post today). No crash, no oversized write.
- Firestore writes stay inside the existing `runCatching` best-effort block in
  `createPost`, consistent with the rest of the write-through layer (offline-first:
  the local Room write already succeeded).
- Blob-decode-to-cache failures on receive are caught and skipped → the reel falls
  back to the drawable rather than crashing the listener.

## Testing
- **JVM unit tests** (extend the existing repository/mapper suite):
  - `MediaEntity.toDoc` / `toPublicReel` carry `isCard`, and `createPost` puts the
    passed bytes onto the docs as `imageBlob`.
  - `PublicReelDoc.toEntity` (with a blob) writes a cache file and sets `imageUri`
    to it; the own-post guard keeps an existing local path.
  - These use a `Blob` constant / temp dir — no `Bitmap`, so they stay on the JVM.
- **Instrumented test** (`connectedDebugAndroidTest`): `compressReelImage` output
  is non-null and under the cap for a large bitmap (`Bitmap` is an Android type, so
  this needs a device/Robolectric).

## Verification (on-device)
1. Build with git-bash + `JAVA_HOME` = Android Studio jbr (memory `build-env.md`).
2. **Cross-device checkpoint (Phase 5 §6):** post a run-summary card on phone A;
   sign in on phone B → the reel appears in the feed **with the branded card image**
   (not the generic drawable), footer readable (Fit). Repeat with a raw photo via
   CreatePostDialog.
3. Confirm a stats-only post (no image) still falls back to the drawable cleanly.

## Files touched (summary)
- `data/cloud/FirestoreSchema.kt` — `imageBlob` + `isCard` on `MediaDoc` & `PublicReelDoc`
- `view/screen/RouteSnapshot.kt` — `compressReelImage` helper
- `data/repository/GalleryRepository.kt` — `cacheDir` param; `pushMediaToCloud`; decode-to-cache on receive; own-post guard; mappers (`blob`/`isCard`/`imageUriOverride`)
- `data/dao/ActivityDao.kt` — `suspend fun getMediaById(id): MediaEntity?` (own-post guard)
- `view_model/RecordViewModel.kt` — `imageBytes` through `saveActivity`
- `view_model/GalleryViewModel.kt` — `imageBytes` through `createPost`
- `view/screen/RecordScreen.kt` — encode `cardBitmap`, pass bytes
- `view/screen/GalleryScreen.kt` — decode picked URI, encode, pass bytes
- `RunTrackApplication.kt` — pass `cacheDir` into `GalleryRepository`
- tests — mapper/repo unit tests + `compressReelImage` instrumented test
