# Cross-device Reel Images (Firestore Blob) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a posted reel's image (run-summary card or raw photo) travel to other devices by storing the compressed JPEG as a Firestore `Blob` on the reel docs, decoding it to a local cache file on receive.

**Architecture:** Compress the bitmap to a ≤900 KB JPEG (`compressReelImage`), thread the bytes from the UI/VM into the data layer as `ByteArray`, wrap as a Firestore `Blob` on `MediaDoc`/`PublicReelDoc`, and on the receive listeners decode the blob to `cacheDir/remote_reels/<id>.jpg` and point `MediaEntity.imageUri` at it. Also routes the Record run-summary post through the cloud write for the first time (it was DAO-only before) and syncs the `isCard` flag so remote cards render `Fit`.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Cloud Firestore (`com.google.firebase.firestore.Blob`), Coil, JUnit4 (JVM) + AndroidX instrumented tests.

**Spec:** `docs/superpowers/specs/2026-06-12-reel-image-sync-design.md`

**Build env:** Bash + git-bash, `JAVA_HOME` = Android Studio jbr (memory `build-env.md`). Unit tests: `./gradlew testDebugUnitTest`. Compile: `./gradlew compileDebugKotlin`.

---

## File Structure

- **Modify** `data/cloud/FirestoreSchema.kt` — add `imageBlob: Blob?` + `isCard: Boolean` to `MediaDoc` and `PublicReelDoc`.
- **Modify** `view/screen/RouteSnapshot.kt` — add `compressReelImage(bitmap, maxBytes)` helper.
- **Modify** `data/repository/GalleryRepository.kt` — `cacheDir` ctor param; `pushMediaToCloud`; mappers gain `blob`/`isCard`/`imageUriOverride`; listeners decode blob→cache; own-post guard; `writeReelCache` helper.
- **Modify** `data/dao/ActivityDao.kt` — `getMediaById`.
- **Modify** `RunTrackApplication.kt` — pass `cacheDir` into `GalleryRepository`.
- **Modify** `view_model/RecordViewModel.kt` — `galleryRepository` ref; `imageBytes` param on `saveActivity`; call `pushMediaToCloud`.
- **Modify** `view_model/GalleryViewModel.kt` — `imageBytes` param on `createPost`.
- **Modify** `view/screen/RecordScreen.kt` — encode `cardBitmap`, pass bytes.
- **Modify** `view/screen/GalleryScreen.kt` — decode picked URI, encode, pass bytes.
- **Modify** `app/src/test/.../data/repository/GalleryMapperTest.kt` — blob/isCard mapper assertions.
- **Create** `app/src/androidTest/.../view/screen/CompressReelImageTest.kt` — cap enforcement.

---

## Task 1: Schema — add `imageBlob` + `isCard` to the reel DTOs

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/cloud/FirestoreSchema.kt`

- [ ] **Step 1: Add the import and the two fields to both DTOs**

At the top of the file, under the `package` line, add:

```kotlin
import com.google.firebase.firestore.Blob
```

In `data class MediaDoc(...)`, add the two fields after `imageUri`:

```kotlin
/** users/{uid}/media/{mediaId} */
data class MediaDoc(
    val id: String = "",
    val author: String = "",
    val caption: String = "",
    val activity: String = "",
    val distanceKm: String = "",
    val tint: Long = 0L,
    val imageRes: Int = 0,
    val imageUri: String? = null,
    val imageBlob: Blob? = null,
    val isCard: Boolean = false,
    val likes: Int = 0,
    val createdAtMs: Long = 0L
)
```

In `data class PublicReelDoc(...)`, add the same two fields after `imageUri`:

```kotlin
/** publicReels/{mediaId} — a reel surfaced in the cross-user Gallery feed. */
data class PublicReelDoc(
    val id: String = "",
    val ownerUid: String = "",
    val author: String = "",
    val caption: String = "",
    val activity: String = "",
    val distanceKm: String = "",
    val tint: Long = 0L,
    val imageRes: Int = 0,
    val imageUri: String? = null,
    val imageBlob: Blob? = null,
    val isCard: Boolean = false,
    val likes: Int = 0,
    val createdAtMs: Long = 0L
)
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the mappers in `GalleryRepository.kt` still compile — the new fields have defaults).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/cloud/FirestoreSchema.kt
git commit -m "P5: reel DTOs carry imageBlob + isCard for cross-device images"
```

---

## Task 2: `compressReelImage` helper

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt`
- Test: `app/src/androidTest/java/com/example/a211198_hasif_drnelson_Project2/view/screen/CompressReelImageTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/example/a211198_hasif_drnelson_Project2/view/screen/CompressReelImageTest.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CompressReelImageTest {

    // A large, noisy bitmap is the worst case for JPEG (noise does not compress).
    private fun noisyBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rnd = Random(7)
        for (x in 0 until size step 4) {
            for (y in 0 until size step 4) {
                canvas.drawColor(Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
                bmp.setPixel(x, y, Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
            }
        }
        return bmp
    }

    @Test
    fun stays_under_cap_for_large_noisy_bitmap() {
        val bytes = compressReelImage(noisyBitmap(2000), maxBytes = 900_000)
        assertNotNull("should produce bytes", bytes)
        assertTrue("under cap: ${bytes!!.size}", bytes.size <= 900_000)
    }

    @Test
    fun small_bitmap_round_trips() {
        val bytes = compressReelImage(noisyBitmap(300))
        assertNotNull(bytes)
        assertTrue(bytes!!.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew compileDebugAndroidTestKotlin`
Expected: FAIL — `compressReelImage` is unresolved.

- [ ] **Step 3: Implement `compressReelImage`**

In `RouteSnapshot.kt`, add `import java.io.ByteArrayOutputStream` to the imports, then add at the end of the file:

```kotlin
// Compress a reel bitmap to a JPEG ByteArray that fits under maxBytes (Firestore's
// per-document limit is 1 MB; 900 KB leaves room for the other doc fields). Steps
// quality down first, then halves the longest edge, retrying until under the cap.
// Returns null if it still cannot fit (caller then posts without an image blob and
// the reel falls back to the drawable on other devices).
fun compressReelImage(bitmap: Bitmap, maxBytes: Int = 900_000): ByteArray? {
    var current = bitmap
    repeat(4) {
        for (quality in intArrayOf(80, 65, 50, 40)) {
            val out = ByteArrayOutputStream()
            current.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes) return bytes
        }
        // Still too big at the lowest quality — halve the dimensions and retry.
        val w = current.width / 2
        val h = current.height / 2
        if (w < 1 || h < 1) return null
        current = Bitmap.createScaledBitmap(current, w, h, true)
    }
    return null
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "*CompressReelImageTest*"`
Expected: PASS (needs a connected device/emulator). If no device is attached, at minimum `./gradlew compileDebugAndroidTestKotlin` must succeed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt app/src/androidTest/java/com/example/a211198_hasif_drnelson_Project2/view/screen/CompressReelImageTest.kt
git commit -m "P5: compressReelImage caps reel JPEG under the Firestore doc limit"
```

---

## Task 3: DAO — `getMediaById` (own-post guard support)

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/dao/ActivityDao.kt`

- [ ] **Step 1: Add the query**

After the existing `insertMedia` declaration (line ~21), add:

```kotlin
    @Query("SELECT * FROM media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): MediaEntity?
```

(`@Query` and `MediaEntity` are already imported in this file; if not, add
`import androidx.room.Query` and
`import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity`.)

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (Room generates the query implementation).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/dao/ActivityDao.kt
git commit -m "P5: ActivityDao.getMediaById for the own-post sync guard"
```

---

## Task 4: Repository mappers carry blob + isCard (TDD)

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt`
- Test: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryMapperTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these tests inside the `GalleryMapperTest` class (before the closing brace).
Add `import com.google.firebase.firestore.Blob` to the test imports.

```kotlin
    @Test
    fun `toDoc carries isCard and the image blob`() {
        val blob = Blob.fromBytes(byteArrayOf(1, 2, 3))
        val doc = sampleEntity().copy(isCard = true).toDoc(blob)
        assertEquals(true, doc.isCard)
        assertEquals(blob, doc.imageBlob)
    }

    @Test
    fun `toDoc without a blob leaves imageBlob null`() {
        val doc = sampleEntity().toDoc(null)
        assertEquals(null, doc.imageBlob)
        assertEquals(false, doc.isCard)
    }

    @Test
    fun `toPublicReel carries isCard and the image blob`() {
        val blob = Blob.fromBytes(byteArrayOf(9))
        val reel = sampleEntity().copy(isCard = true).toPublicReel("uid-abc", blob)
        assertEquals("uid-abc", reel.ownerUid)
        assertEquals(true, reel.isCard)
        assertEquals(blob, reel.imageBlob)
    }

    @Test
    fun `remote public reel uses the cache path override and carries isCard`() {
        val doc = sampleEntity().copy(isCard = true).toPublicReel("uid-abc", Blob.fromBytes(byteArrayOf(1)))
        val entity = doc.toEntity(imageUriOverride = "/cache/remote_reels/reel-1.jpg")
        assertEquals("/cache/remote_reels/reel-1.jpg", entity.imageUri)
        assertEquals(true, entity.isCard)
        assertEquals("uid:uid-abc", entity.ownerEmail)
    }

    @Test
    fun `remote public reel with no override falls back to null image`() {
        val doc = sampleEntity().toPublicReel("uid-abc", null)
        val entity = doc.toEntity(imageUriOverride = null)
        assertEquals(null, entity.imageUri)
    }

    @Test
    fun `media doc maps back with the cache path override and isCard`() {
        val doc = sampleEntity().copy(isCard = true).toDoc(Blob.fromBytes(byteArrayOf(1)))
        val entity = doc.toEntity(ownerEmail = "me@example.com", imageUriOverride = "/cache/x.jpg")
        assertEquals("me@example.com", entity.ownerEmail)
        assertEquals("/cache/x.jpg", entity.imageUri)
        assertEquals(true, entity.isCard)
    }
```

> Note: the pre-existing tests call `.toDoc()`, `.toPublicReel("uid-abc")`, and
> `.toEntity()` with no new args — Step 3 gives every new parameter a default, so
> those keep compiling unchanged.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*GalleryMapperTest*"`
Expected: FAIL — `toDoc(blob)`, `toPublicReel(uid, blob)`, and the `imageUriOverride` params do not exist yet.

- [ ] **Step 3: Update the four mappers**

In `GalleryRepository.kt`, replace the four mapper functions at the bottom of the
file with these signatures. Add `import com.google.firebase.firestore.Blob` to the
imports.

```kotlin
internal fun MediaEntity.toDoc(blob: Blob? = null) = MediaDoc(
    id = id,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    imageBlob = blob,
    isCard = isCard,
    likes = likes,
    createdAtMs = createdAtMs
)

internal fun MediaEntity.toPublicReel(ownerUid: String, blob: Blob? = null) = PublicReelDoc(
    id = id,
    ownerUid = ownerUid,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    imageBlob = blob,
    isCard = isCard,
    likes = likes,
    createdAtMs = createdAtMs
)

// Remote reels are stored under a synthetic ownerEmail ("uid:<ownerUid>") so they
// never collide with my own email-keyed posts or leak into my "my posts" view.
// imageUriOverride is the local cache-file path the listener wrote the decoded
// image blob to (null → drawable fallback); the remote device's own imageUri is
// meaningless here and is dropped.
internal fun PublicReelDoc.toEntity(imageUriOverride: String? = null) = MediaEntity(
    id = id,
    ownerEmail = "uid:$ownerUid",
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUriOverride,
    likes = likes,
    createdAtMs = createdAtMs,
    isCard = isCard
)

internal fun MediaDoc.toEntity(ownerEmail: String, imageUriOverride: String? = null) = MediaEntity(
    id = id,
    ownerEmail = ownerEmail,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUriOverride,
    likes = likes,
    createdAtMs = createdAtMs,
    isCard = isCard
)
```

> This drops the meaningless remote `imageUri` (previously copied verbatim, which
> made cross-device photo posts render blank). `imageUri` now comes only from the
> locally-written cache path.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*GalleryMapperTest*"`
Expected: PASS (new + pre-existing tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt app/src/test/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryMapperTest.kt
git commit -m "P5: reel mappers carry image blob + isCard, drop stale remote imageUri"
```

---

## Task 5: Repository write path — `cacheDir`, `pushMediaToCloud`, cache helper

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/RunTrackApplication.kt`

- [ ] **Step 1: Add the `cacheDir` constructor param**

Change the `GalleryRepository` constructor:

```kotlin
class GalleryRepository(
    db: AppDatabase,
    private val cacheDir: File,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
```

Add the imports near the top:

```kotlin
import com.google.firebase.firestore.Blob
import java.io.File
import java.io.FileOutputStream
```

(`Blob` may already be imported from Task 4 — keep a single import.)

- [ ] **Step 2: Add `pushMediaToCloud` + the cache helper; refactor `createPost`**

Replace the body of `createPost` so it delegates the Firestore write to a new
`pushMediaToCloud`, and add the cache-writer. The new `createPost`:

```kotlin
    suspend fun createPost(
        email: String,
        caption: String,
        activity: String,
        distanceKm: String,
        imageUri: String?,
        imageRes: Int,
        isCard: Boolean = false,
        imageBytes: ByteArray? = null
    ) {
        if (email.isBlank()) return
        val user = userDao.findByEmail(email)
        val author = user?.runnerName ?: "You"
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = MediaEntity(
            id = id,
            ownerEmail = email,
            author = author,
            caption = caption.ifBlank { "New post" },
            activity = activity.ifBlank { "Run" },
            distanceKm = distanceKm.ifBlank { "0" },
            tint = 0xFF1E3A5F,
            imageRes = imageRes,
            imageUri = imageUri,
            likes = 0,
            createdAtMs = now,
            isCard = isCard
        )
        activityDao.insertMedia(entity)
        pushMediaToCloud(entity, imageBytes)
    }

    /**
     * Write a reel (already in Room) through to Firestore: the private copy under
     * users/{uid}/media and the public copy in publicReels, both carrying the
     * compressed image as a Blob. Best-effort — the local write already succeeded.
     */
    suspend fun pushMediaToCloud(entity: MediaEntity, imageBytes: ByteArray?) {
        val uid = userDao.findByEmail(entity.ownerEmail)?.firebaseUid ?: return
        val blob = imageBytes?.let { Blob.fromBytes(it) }
        runCatching {
            firestore.collection(USERS).document(uid).collection(MEDIA).document(entity.id)
                .set(entity.toDoc(blob)).await()
            firestore.collection(PUBLIC_REELS).document(entity.id)
                .set(entity.toPublicReel(uid, blob)).await()
        }
    }

    // Decode a reel image blob to a stable cache file and return its absolute path
    // (or null on failure). Idempotent on the reel id; Coil renders the file path.
    private fun writeReelCache(id: String, bytes: ByteArray): String? = runCatching {
        val dir = File(cacheDir, "remote_reels").apply { mkdirs() }
        val file = File(dir, "$id.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }.getOrNull()
```

- [ ] **Step 3: Wire `cacheDir` in `RunTrackApplication`**

In `RunTrackApplication.kt`, update the `galleryRepository` initializer:

```kotlin
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(AppDatabase.get(this), cacheDir) }
```

(`cacheDir` is a `Context` property available on the `Application`.)

- [ ] **Step 4: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt app/src/main/java/com/example/a211198_hasif_drnelson_Project2/RunTrackApplication.kt
git commit -m "P5: GalleryRepository.pushMediaToCloud writes image blob; cacheDir wired"
```

---

## Task 6: Repository receive path — decode blob to cache in both listeners

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt`

- [ ] **Step 1: Decode-to-cache in `startFeedSync`**

Replace the per-document body inside the `startFeedSync` snapshot loop with:

```kotlin
                        for (doc in snap.documents) {
                            val r = doc.toObject(PublicReelDoc::class.java) ?: continue
                            if (r.ownerUid.isBlank() || r.ownerUid == myUid) continue
                            val cachePath = r.imageBlob?.toBytes()?.let { writeReelCache(r.id, it) }
                            activityDao.insertMedia(r.toEntity(imageUriOverride = cachePath))
                        }
```

- [ ] **Step 2: Decode-to-cache + own-post guard in `startMineSync`**

Replace the per-document body inside the `startMineSync` snapshot loop with:

```kotlin
                        for (doc in snap.documents) {
                            val m = doc.toObject(MediaDoc::class.java) ?: continue
                            // Own-post guard: if this device already holds the post
                            // with a local image path, keep it (don't clobber the
                            // crisp local file with the re-encoded blob). On a fresh
                            // install the local row is absent, so decode the blob.
                            val existing = activityDao.getMediaById(m.id)
                            val cachePath = existing?.imageUri
                                ?: m.imageBlob?.toBytes()?.let { writeReelCache(m.id, it) }
                            activityDao.insertMedia(m.toEntity(ownerEmail = email, imageUriOverride = cachePath))
                        }
```

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the full unit suite (no regressions)**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all existing + Task-4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt
git commit -m "P5: reel listeners decode image blob to cache; own-post guard keeps local file"
```

---

## Task 7: Record path — encode the card and push to cloud

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

- [ ] **Step 1: Give `RecordViewModel` a repository ref + `imageBytes` param**

In `RecordViewModel.kt`, add the repository field next to the other deps (after
the `userDao` line, ~line 32):

```kotlin
    private val galleryRepository =
        (application as RunTrackApplication).galleryRepository
```

Change `saveActivity`'s signature to add `imageBytes`, and push to cloud after the
local insert. Replace the signature and the `viewModelScope.launch { ... }` block:

```kotlin
    fun saveActivity(
        type: String = "Run",
        caption: String = "New run",
        imageUri: String? = null,
        isCard: Boolean = false,
        imageBytes: ByteArray? = null,
    ) {
        if (!shouldSaveRun(elapsedSeconds, distanceKm, imageUri != null)) return
        val email = prefs.getString("activeEmail", null).orEmpty()
        if (email.isBlank()) return
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(now))
        val record = buildRunRecord(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            caption = caption,
            type = type,
            distanceKm = distanceKm,
            elapsedSeconds = elapsedSeconds,
            dateStr = dateStr,
        )
        viewModelScope.launch {
            val author = userDao.findByEmail(email)?.runnerName ?: "You"
            val media = buildRunMedia(
                id = UUID.randomUUID().toString(),
                ownerEmail = email,
                author = author,
                caption = caption,
                type = type,
                distanceKm = distanceKm,
                imageUri = imageUri,
                createdAtMs = now,
                isCard = isCard,
            )
            activityDao.insertActivity(record)
            activityDao.insertMedia(media)
            galleryRepository.pushMediaToCloud(media, imageBytes)
        }
    }
```

(`RunTrackApplication` is already imported in this file.)

- [ ] **Step 2: Encode the card bitmap in `RecordScreen.onPost`**

In `RecordScreen.kt`, in the `RunSummarySheet(onPost = { caption, cardBitmap -> ... })`
lambda (~line 321), encode the bitmap and pass the bytes. Replace the lambda body:

```kotlin
                onPost = { caption, cardBitmap ->
                    val runType = recordViewModel.activityType
                    val uri = cardBitmap?.let { saveBitmapToInternalStorage(context, it) }
                    val imageBytes = cardBitmap?.let { compressReelImage(it) }
                    recordViewModel.saveActivity(
                        type = runType,
                        caption = caption,
                        imageUri = uri,
                        // The post is a branded card only when we actually captured
                        // one; a stats-only run (no bitmap) falls back to the
                        // full-bleed drawable, which should stay Crop.
                        isCard = uri != null,
                        imageBytes = imageBytes,
                    )
                    snapshotter?.cancel()
                    snapshotter = null
                    showSummary = false
                    snapshot = null
                    recordViewModel.reset()
                },
```

(`compressReelImage` and `saveBitmapToInternalStorage` are in the same package, so
no import is needed.)

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P5: run-summary post syncs to cloud with its compressed card image"
```

---

## Task 8: CreatePostDialog path — encode the picked photo

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/GalleryViewModel.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/GalleryScreen.kt`

- [ ] **Step 1: Add `imageBytes` to `GalleryViewModel.createPost`**

In `GalleryViewModel.kt`, replace the `createPost` function:

```kotlin
    fun createPost(
        caption: String,
        activity: String,
        distanceKm: String,
        imageUri: String?,
        imageBytes: ByteArray? = null
    ) {
        val email = activeEmail.ifBlank { return }
        viewModelScope.launch {
            repository.createPost(
                email, caption, activity, distanceKm, imageUri,
                R.drawable.lakesidetrail, isCard = false, imageBytes = imageBytes
            )
        }
    }
```

- [ ] **Step 2: Decode + encode the picked URI in `GalleryScreen`**

In `GalleryScreen.kt`, find the call to `...createPost(` that passes the picked
image URI (the CreatePostDialog confirm handler). Immediately before it, decode the
picked `content://` URI to a bitmap and compress it. Add these imports to the file
if missing:

```kotlin
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
```

Where the dialog confirms with the selected `imageUri: String?` (call it
`selectedUri`), build the bytes and pass them. Example shape (adapt to the existing
variable names at the call site):

```kotlin
    val context = LocalContext.current
    // ... in the onConfirm/onPost handler that currently calls createPost(...):
    val imageBytes: ByteArray? = selectedUri?.let { uriString ->
        runCatching {
            context.contentResolver.openInputStream(android.net.Uri.parse(uriString)).use { input ->
                val bmp = BitmapFactory.decodeStream(input) ?: return@runCatching null
                compressReelImage(bmp)
            }
        }.getOrNull()
    }
    galleryViewModel.createPost(caption, activity, distanceKm, selectedUri, imageBytes)
```

> `compressReelImage` is in the same package (`view.screen`) as `GalleryScreen`, so
> no import is needed. If `LocalContext` / a `context` val already exists in this
> composable, reuse it instead of re-declaring.

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/GalleryViewModel.kt app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/GalleryScreen.kt
git commit -m "P5: CreatePostDialog photos sync cross-device via compressed blob"
```

---

## Task 9: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Compile + unit tests green**

Run: `./gradlew compileDebugKotlin testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all unit tests pass.

- [ ] **Step 2: Instrumented tests compile (and run if a device is attached)**

Run: `./gradlew compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL. With a device: `./gradlew connectedDebugAndroidTest --tests "*CompressReelImageTest*"` PASS.

- [ ] **Step 3: On-device cross-device checkpoint (Phase 5 §6)**

Manual, two devices/accounts (build per memory `maplibre-vulkan-pin` — debug APK is armeabi-v7a only):
1. Phone A: record a run → End → Post the run-summary card.
2. Phone B (different account, follows/searches A): open the cross-user feed → the
   reel shows the **branded card image** (not the generic drawable), stats footer
   readable (Fit). 
3. Repeat with a raw photo via CreatePostDialog → image appears on B.
4. A stats-only post (no image) still falls back cleanly to the drawable.
5. Fresh-install A's account on a third device → own reels restore **with** images.

- [ ] **Step 4: Update the roadmap**

In `plan.md`, mark the Phase 5 image-sync item done and note the Firestore-blob
approach (Storage was unavailable without Blaze). Update the §8 table / the P5
notes (the "Image limitation" caveat at `plan.md` §8 and the `GalleryRepository`
header comment about images not syncing).

```bash
git add plan.md app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt
git commit -m "P5: roadmap — cross-device reel images shipped via Firestore blob"
```

---

## Self-Review notes (for the executor)

- **Spec coverage:** Task 1 = schema; Task 2 = compression; Tasks 5–6 = write+receive; Tasks 7–8 = both post paths; Task 4 = mappers/tests; Task 3 = guard support; Task 9 = verification + roadmap. All spec sections map to a task.
- **Type consistency:** `compressReelImage(bitmap, maxBytes=900_000): ByteArray?`; `pushMediaToCloud(entity, imageBytes)`; `toDoc(blob)`, `toPublicReel(uid, blob)`, `PublicReelDoc.toEntity(imageUriOverride)`, `MediaDoc.toEntity(ownerEmail, imageUriOverride)`; `getMediaById(id): MediaEntity?`. These names are used identically across tasks.
- **Default args** keep every pre-existing caller (and the two existing mapper tests) compiling without edits.
