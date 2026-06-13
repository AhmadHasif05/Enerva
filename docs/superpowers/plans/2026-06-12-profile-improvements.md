# Profile Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-select delete to the user's own gallery, let users open other users' profiles (Follow-only) by tapping a reel's name/avatar bar, and show real, cross-device profile photos in reels.

**Architecture:** Avatars reuse the project's existing "compress to JPEG → Firestore `Blob` → decode to cache file" pattern (already used for reel images), stored once on `publicProfiles/{uid}` and joined by display name in the UI. `ProfileScreen` becomes dual-mode (self vs. another user) via an optional `authorName` argument. Deletes are write-through (Room + Firestore), and the existing sync listeners gain `DocumentChange.REMOVED` handling so deletes propagate.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Firebase Firestore, Coil.

**Testing note:** This module has no unit/instrumentation test harness, and the changes are Compose UI + Firestore sync. Each task is verified by a clean Gradle compile (`./gradlew assembleDebug`); Task 7 is a manual on-device verification checklist. This matches the project's established verification practice.

**Build/verify command** (git-bash, from project root — see `memory/build-env.md`):
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"   # adjust to your Android Studio JBR
./gradlew assembleDebug
```
Expected on success: `BUILD SUCCESSFUL`.

---

## File Structure

**Data / model layer**
- `data/cloud/FirestoreSchema.kt` — add `photoBlob` to `PublicProfileDoc`.
- `data/dao/ActivityDao.kt` — add `deleteMediaByIds`.
- `data/repository/GalleryRepository.kt` — `deletePosts`; `REMOVED` handling in both sync listeners.
- `data/repository/UserRepository.kt` — `cacheDir`; avatar blob upload (merge) + decode-on-receive.
- `RunTrackApplication.kt` — pass `cacheDir` into `UserRepository`.

**ViewModels**
- `view_model/GalleryViewModel.kt` — `deletePosts(ids)`.
- `view_model/UserViewModel.kt` — `updatePhotoUri(uri, bytes)`, `photoForAuthor(name)`.

**UI**
- `view/screen/RouteSnapshot.kt` — add `compressAvatarImage` next to `compressReelImage`.
- `view/screen/EditProfile.kt` — compress picked avatar → bytes → `updatePhotoUri`.
- `view/Navigation.kt` — `Screen.UserProfile` + `userProfileRoute`.
- `view/MainActivity.kt` — wire `messageViewModel` into Profile; add UserProfile route.
- `view/screen/GalleryScreen.kt` — real reel avatar + clickable author bar.
- `view/screen/ProfileScreen.kt` — dual mode (self/other) + multi-select delete.

---

## Task 1: Delete plumbing (data layer)

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/dao/ActivityDao.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/GalleryViewModel.kt`

- [x] **Step 1: Add the delete query to `ActivityDao`**

In `ActivityDao.kt`, add this method inside the `interface ActivityDao` (e.g. after `renameAuthor`):

```kotlin
    // Delete reels by id (used by the profile multi-select delete).
    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<String>)
```

- [x] **Step 2: Add `deletePosts` + `REMOVED` handling to `GalleryRepository`**

In `GalleryRepository.kt`, add the import near the other firestore imports:

```kotlin
import com.google.firebase.firestore.DocumentChange
```

Add this method in the `// ---- writes (write-through) ----` section (e.g. right after `createPost`):

```kotlin
    /**
     * Delete the given reels for the active user: Room first (so the grid updates
     * immediately), then the cloud copies (private + public) and the cached image
     * file. Cloud deletes are best-effort.
     */
    suspend fun deletePosts(email: String, ids: Collection<String>) {
        if (ids.isEmpty()) return
        activityDao.deleteMediaByIds(ids.toList())
        ids.forEach { id -> File(File(cacheDir, "remote_reels"), "$id.jpg").delete() }
        val uid = userDao.findByEmail(email)?.firebaseUid ?: return
        runCatching {
            for (id in ids) {
                firestore.collection(USERS).document(uid).collection(MEDIA).document(id).delete().await()
                firestore.collection(PUBLIC_REELS).document(id).delete().await()
            }
        }
    }
```

In `startMineSync`, replace the snapshot body that iterates `snap.documents` with one that iterates `snap.documentChanges` and handles removals. Replace this block:

```kotlin
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    scope.launch {
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
                    }
                }
```

with:

```kotlin
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    scope.launch {
                        for (change in snap.documentChanges) {
                            val m = change.document.toObject(MediaDoc::class.java)
                            if (change.type == DocumentChange.Type.REMOVED) {
                                activityDao.deleteMediaByIds(listOf(m.id))
                                File(File(cacheDir, "remote_reels"), "${m.id}.jpg").delete()
                                continue
                            }
                            // Own-post guard: if this device already holds the post
                            // with a local image path, keep it (don't clobber the
                            // crisp local file with the re-encoded blob). On a fresh
                            // install the local row is absent, so decode the blob.
                            val existing = activityDao.getMediaById(m.id)
                            val cachePath = existing?.imageUri
                                ?: m.imageBlob?.toBytes()?.let { writeReelCache(m.id, it) }
                            activityDao.insertMedia(m.toEntity(ownerEmail = email, imageUriOverride = cachePath))
                        }
                    }
                }
```

In `startFeedSync`, replace this block:

```kotlin
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    scope.launch {
                        for (doc in snap.documents) {
                            val r = doc.toObject(PublicReelDoc::class.java) ?: continue
                            if (r.ownerUid.isBlank() || r.ownerUid == myUid) continue
                            val cachePath = r.imageBlob?.toBytes()?.let { writeReelCache(r.id, it) }
                            activityDao.insertMedia(r.toEntity(imageUriOverride = cachePath))
                        }
                    }
                }
```

with:

```kotlin
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    scope.launch {
                        for (change in snap.documentChanges) {
                            val r = change.document.toObject(PublicReelDoc::class.java)
                            if (r.ownerUid.isBlank() || r.ownerUid == myUid) continue
                            if (change.type == DocumentChange.Type.REMOVED) {
                                activityDao.deleteMediaByIds(listOf(r.id))
                                File(File(cacheDir, "remote_reels"), "${r.id}.jpg").delete()
                                continue
                            }
                            val cachePath = r.imageBlob?.toBytes()?.let { writeReelCache(r.id, it) }
                            activityDao.insertMedia(r.toEntity(imageUriOverride = cachePath))
                        }
                    }
                }
```

- [x] **Step 3: Add `deletePosts` to `GalleryViewModel`**

In `GalleryViewModel.kt`, add after `createPost(...)`:

```kotlin
    /** Delete the given posts (own gallery multi-select). */
    fun deletePosts(ids: Set<String>) {
        val email = activeEmail.ifBlank { return }
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.deletePosts(email, ids) }
    }
```

- [x] **Step 4: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/dao/ActivityDao.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/GalleryRepository.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/GalleryViewModel.kt
git commit -m "feat: delete plumbing for reels (Room + Firestore + REMOVED sync)"
```

---

## Task 2: Cross-device avatar foundation (data layer)

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/cloud/FirestoreSchema.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/UserRepository.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/RunTrackApplication.kt`

- [x] **Step 1: Add `photoBlob` to `PublicProfileDoc`**

In `FirestoreSchema.kt`, update `PublicProfileDoc`:

```kotlin
/** publicProfiles/{uid} — public slice of a profile, readable by any signed-in user. */
data class PublicProfileDoc(
    val uid: String = "",
    val runnerName: String = "",
    val location: String = "",
    val fitnessLevel: String = "",
    val photoUri: String? = null,
    // Compressed avatar JPEG so the photo travels cross-device (photoUri is a
    // device-local content:// path that doesn't resolve elsewhere). Mirrors the
    // reel-image blob approach. Decoded to a cache file on receive.
    val photoBlob: Blob? = null
)
```

(`Blob` is already imported at the top of this file.)

- [x] **Step 2: Inject `cacheDir` into `UserRepository` and add avatar upload + decode**

In `UserRepository.kt`, add imports near the others:

```kotlin
import com.google.firebase.firestore.Blob
import java.io.File
import java.io.FileOutputStream
```

Change the constructor:

```kotlin
class UserRepository(
    db: AppDatabase,
    private val cacheDir: File,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
```

Add this private helper near the bottom of the class (e.g. just before `stopListeners` or after `pushProfile`):

```kotlin
    // Decode an avatar blob to a stable cache file; return its absolute path (or
    // null on failure). Idempotent on uid; Coil renders the file path.
    private fun writeAvatarCache(uid: String, bytes: ByteArray): String? = runCatching {
        val dir = File(cacheDir, "remote_avatars").apply { mkdirs() }
        val file = File(dir, "$uid.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }.getOrNull()
```

Replace the `publicProfiles/*` listener block inside `startListeners` (the one that builds `UserDirectoryEntity`) with one that decodes the blob:

```kotlin
        // publicProfiles/* — the cross-device user directory. Mirror every other
        // user into Room so Search/profiles can show accounts created elsewhere.
        listeners += firestore.collection(PUBLIC_PROFILES)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                val docs = snap.documents.mapNotNull { it.toObject(PublicProfileDoc::class.java) }
                    .filter { it.uid.isNotBlank() && it.uid != uid }
                if (docs.isEmpty()) return@addSnapshotListener
                scope.launch {
                    val entries = docs.map { d ->
                        // Prefer the decoded blob (loads cross-device); fall back to
                        // the doc's photoUri for profiles that predate avatar blobs.
                        val avatarPath = d.photoBlob?.toBytes()?.let { writeAvatarCache(d.uid, it) }
                        UserDirectoryEntity(
                            uid = d.uid,
                            runnerName = d.runnerName,
                            location = d.location,
                            fitnessLevel = d.fitnessLevel,
                            photoUri = avatarPath ?: d.photoUri
                        )
                    }
                    userDao.upsertDirectoryUsers(entries)
                }
            }
```

Change `saveProfile` to accept avatar bytes and forward them:

```kotlin
    suspend fun saveProfile(user: UserData, avatarBytes: ByteArray? = null) {
        userDao.upsertUser(user.toEntity())
        pushProfile(user, create = false, avatarBytes = avatarBytes)
    }
```

Update `saveProfileWithRename` to keep compiling (it calls `saveProfile`):

```kotlin
    suspend fun saveProfileWithRename(user: UserData, oldName: String) {
        saveProfile(user)
        propagateRename(oldName, user.runnerName)
    }
```
(No change needed if it already reads this way — the new `avatarBytes` parameter defaults to null.)

Replace `pushProfile` with a version that takes `avatarBytes` and writes `publicProfiles` via **merge** (so a name/bio edit never wipes the stored blob), including the blob only when present:

```kotlin
    private suspend fun pushProfile(user: UserData, create: Boolean, avatarBytes: ByteArray? = null) {
        val uid = user.firebaseUid ?: return // no cloud identity yet → local-only
        val ref = firestore.collection(USERS).document(uid)
        val fields = mutableMapOf<String, Any?>(
            "uid" to uid,
            "email" to user.email,
            "runnerName" to user.runnerName,
            "location" to user.location,
            "fitnessLevel" to user.fitnessLevel,
            "personalGoal" to user.personalGoal,
            "bio" to user.bio,
            "following" to user.following,
            "followers" to user.followers,
            "photoUri" to user.photoUri
        )
        if (create) fields["createdAt"] = System.currentTimeMillis()
        // merge so a profile edit never wipes createdAt or fields we didn't send.
        runCatching { ref.set(fields, SetOptions.merge()).await() }

        // Mirror the public slice into the discovery directory. Merge (not a full
        // set) so a later name/bio edit can't drop the avatar blob; include the
        // blob only when the user just picked a new photo.
        val publicFields = mutableMapOf<String, Any?>(
            "uid" to uid,
            "runnerName" to user.runnerName,
            "location" to user.location,
            "fitnessLevel" to user.fitnessLevel,
            "photoUri" to user.photoUri
        )
        if (avatarBytes != null) publicFields["photoBlob"] = Blob.fromBytes(avatarBytes)
        runCatching {
            firestore.collection(PUBLIC_PROFILES).document(uid)
                .set(publicFields, SetOptions.merge()).await()
        }
    }
```

- [x] **Step 3: Pass `cacheDir` in `RunTrackApplication`**

In `RunTrackApplication.kt`, change the `userRepository` line:

```kotlin
    val userRepository: UserRepository by lazy { UserRepository(AppDatabase.get(this), cacheDir) }
```

- [x] **Step 4: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/cloud/FirestoreSchema.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/UserRepository.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/RunTrackApplication.kt
git commit -m "feat: cross-device avatar blob sync on publicProfiles"
```

---

## Task 3: Avatar compression + Edit Profile upload + ViewModel helpers

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/UserViewModel.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/EditProfile.kt`

- [x] **Step 1: Add `compressAvatarImage` next to `compressReelImage`**

In `RouteSnapshot.kt`, add this top-level function directly after `compressReelImage` (around line 212). It pre-scales to a small square-ish bound, then reuses the existing compressor:

```kotlin
// Compress a profile avatar: downscale the longest side to `maxDim` px first
// (avatars render small), then reuse compressReelImage's quality ladder under a
// tight byte cap so the blob stays well within the Firestore doc limit.
fun compressAvatarImage(bitmap: Bitmap, maxDim: Int = 256): ByteArray? {
    val longest = maxOf(bitmap.width, bitmap.height)
    val scaled = if (longest > maxDim) {
        val ratio = maxDim.toFloat() / longest
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    } else bitmap
    return compressReelImage(scaled, maxBytes = 120_000)
}
```

- [x] **Step 2: Update `UserViewModel.updatePhotoUri` + add `photoForAuthor`**

In `UserViewModel.kt`, replace `updatePhotoUri`:

```kotlin
    fun updatePhotoUri(photoUri: String?, avatarBytes: ByteArray? = null) {
        val updated = userProfile.copy(photoUri = photoUri)
        userProfile = updated
        viewModelScope.launch { userRepository.saveProfile(updated, avatarBytes) }
    }
```

Add this read helper in the `// ---- follows ----` area (anywhere public in the class):

```kotlin
    /**
     * Resolve a reel/profile author's avatar by display name: my own photo if it's
     * me, else the directory entry (cross-device), else null (icon fallback).
     */
    fun photoForAuthor(name: String): String? =
        if (name == userProfile.runnerName) userProfile.photoUri
        else otherUsers.firstOrNull { it.runnerName == name }?.photoUri
```

- [x] **Step 3: Encode the avatar in Edit Profile and pass the bytes**

In `EditProfile.kt`, replace the `pickPhoto` result callback body so it compresses the chosen image and forwards the bytes:

```kotlin
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Persist read access so AsyncImage can load this URI across process restarts.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* non-persistable provider; still usable this session */ }
            photoUri = uri.toString()
            // Compress to a small JPEG so the avatar syncs cross-device as a blob.
            val avatarBytes = runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        ?: return@runCatching null
                    compressAvatarImage(bmp)
                }
            }.getOrNull()
            userViewModel.updatePhotoUri(photoUri, avatarBytes)
        }
    }
```

`compressAvatarImage` is a top-level function in the same `view.screen` package, so no import is needed.

- [x] **Step 4: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/UserViewModel.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/EditProfile.kt
git commit -m "feat: compress + upload profile avatar; photoForAuthor helper"
```

---

## Task 4: User profile route + reel avatar/clickable author bar

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/Navigation.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/GalleryScreen.kt`

- [x] **Step 1: Add the UserProfile destination + route builder**

In `Navigation.kt`, add inside the `sealed class Screen` (after `UserGallery`):

```kotlin
    // Viewing another user's profile. Use userProfileRoute(name) to build it.
    object UserProfile : Screen("profile/user/{authorName}", "User Profile", Icons.Rounded.Person)
```

Add the builder next to `userGalleryRoute`:

```kotlin
// Build a route to view a specific user's profile (Follow-only, read-only gallery).
fun userProfileRoute(authorName: String): String =
    "profile/user/${java.net.URLEncoder.encode(authorName, "UTF-8")}"
```

- [x] **Step 2: Show real reel avatars and make the author bar tappable**

In `GalleryScreen.kt`, add imports:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Person
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.userProfileRoute
```

In the `VerticalPager` page body, compute the author photo and pass new params to `ReelPage`. Replace the `ReelPage(...)` call with:

```kotlin
            val following = userViewModel.isFollowing(reel.author)
            val authorPhoto = userViewModel.photoForAuthor(reel.author)
            ReelPage(
                reel = reel,
                isMine = reel.author == myName,
                authorPhoto = authorPhoto,
                isLiked = liked[reel.id] == true,
                likeBump = likeBumps[reel.id] ?: 0,
                isFollowing = following,
                onAuthorClick = {
                    if (reel.author == myName) navController?.navigate(Screen.Profile.route)
                    else navController?.navigate(userProfileRoute(reel.author))
                },
                onLike = {
                    val now = liked[reel.id] != true
                    liked[reel.id] = now
                    if (now) likeBumps[reel.id] = (likeBumps[reel.id] ?: 0) + 1
                    else likeBumps[reel.id] = (likeBumps[reel.id] ?: 0) - 1
                },
                onFollow = {
                    val wasFollowing = userViewModel.isFollowing(reel.author)
                    userViewModel.toggleFollow(reel.author)
                    if (!wasFollowing) {
                        messageViewModel.startConversationWith(reel.author)
                    } else {
                        messageViewModel.removeConversation(reel.author)
                    }
                }
            )
```

Update the `ReelPage` signature to add `authorPhoto` and `onAuthorClick`:

```kotlin
@Composable
private fun ReelPage(
    reel: GalleryActivity,
    isMine: Boolean,
    authorPhoto: String?,
    isLiked: Boolean,
    likeBump: Int,
    isFollowing: Boolean,
    onAuthorClick: () -> Unit,
    onLike: () -> Unit,
    onFollow: () -> Unit
) {
```

In `ReelPage`, replace the author `Row` (the one starting `Row(verticalAlignment = Alignment.CenterVertically)` that holds the gray avatar `Box`, the name `Text`, and the Follow button) with this version — the avatar+name are grouped in an inner clickable `Row`, and the gray circle becomes a real photo or person icon:

```kotlin
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        if (authorPhoto != null) {
                            AsyncImage(
                                model = authorPhoto,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        reel.author,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Don't show "Follow" on your own reel.
                if (!isMine) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollow,
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Following", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = onFollow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
```

- [x] **Step 3: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/Navigation.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/GalleryScreen.kt
git commit -m "feat: real reel avatars + tap author bar to open profile"
```

---

## Task 5: ProfileScreen dual mode (self vs. other) + wire route

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/MainActivity.kt`

This task makes `ProfileScreen` render either the current user (self) or another user (Follow-only, no Edit/Logout/Saved). The multi-select delete grid is added in Task 6 on top of this.

- [x] **Step 1: Replace `ProfileScreen.kt` with the dual-mode version**

Replace the entire contents of `ProfileScreen.kt` with:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.model.UserData
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.userGalleryRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.GalleryViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel

// ProfileScreen — dual mode.
//  - authorName == null  → the signed-in user's own profile (Edit Profile, Logout,
//    Saved section, and the multi-select gallery grid).
//  - authorName != null  → another user's profile: avatar + name + stats + a single
//    Follow/Following button, a read-only gallery, and no Edit/Logout/Saved.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    navController: NavController,
    messageViewModel: MessageViewModel = viewModel(factory = MessageViewModel.Factory),
    onLogout: () -> Unit = {},
    authorName: String? = null,
    galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val isSelf = authorName == null
    val myProfile = userViewModel.userProfile

    // For another user's profile, resolve their data from the in-memory directory
    // list first, then fall back to a name lookup.
    var otherProfile by remember(authorName) { mutableStateOf<UserData?>(null) }
    if (!isSelf) {
        val fromList = userViewModel.otherUsers.firstOrNull { it.runnerName == authorName }
        LaunchedEffect(authorName, fromList) {
            otherProfile = fromList ?: userViewModel.findUserByName(authorName!!)
        }
    }
    val userData = if (isSelf) myProfile else (otherProfile ?: UserData(runnerName = authorName ?: ""))

    val primaryColor = MaterialTheme.colorScheme.primary
    val isFollowing = userViewModel.isFollowing(userData.runnerName)

    // Pick the gallery mode: my own posts vs. the visited user's posts.
    LaunchedEffect(isSelf, userData.email, userData.runnerName) {
        if (isSelf) {
            if (userData.email.isNotBlank()) {
                galleryViewModel.showMyPosts(userData.email, userData.runnerName.ifBlank { "You" })
            }
        } else if (userData.runnerName.isNotBlank()) {
            galleryViewModel.showAuthorGallery(userData.runnerName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Gallery + Logout actions only belong on your own profile.
                    if (isSelf) {
                        IconButton(onClick = { navController.navigate(Screen.Gallery.route) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { onLogout() }) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Image and Name
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = userData.photoUri ?: R.drawable.hasif_profile,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = userData.runnerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Email is private to other users — only show it on your own profile.
                    if (isSelf && userData.email.isNotBlank()) {
                        Text(
                            text = userData.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = userData.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    if (userData.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = userData.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Stats row
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    Text("Following", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${userData.following}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column {
                    Text("Followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${userData.followers}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isSelf) {
                // Own profile: Edit Profile only (no self-follow).
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.EditProfile.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Another user's profile: Follow / Following only.
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = {
                                userViewModel.toggleFollow(userData.runnerName)
                                messageViewModel.removeConversation(userData.runnerName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            border = BorderStroke(1.dp, primaryColor),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Following", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                userViewModel.toggleFollow(userData.runnerName)
                                messageViewModel.startConversationWith(userData.runnerName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Follow", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Gallery section — Instagram-style 3-column image grid. Latest at the top.
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    "Gallery",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val gallery = galleryViewModel.reels
                if (gallery.isEmpty()) {
                    Text(
                        if (isSelf) "No posts yet. Tap + on Gallery to share your first run."
                        else "${userData.runnerName} hasn't posted any reels yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        gallery.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable {
                                                navController.navigate(userGalleryRoute(item.author))
                                            }
                                    ) {
                                        AsyncImage(
                                            model = item.imageUri ?: item.imageRes,
                                            contentDescription = item.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Saved section — own profile only.
            if (isSelf) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        "Saved",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val savedRoutes = userViewModel.savedRoutes
                    if (savedRoutes.isEmpty()) {
                        Text(
                            "No saved places yet. Tap the bookmark on a route to save it here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedRoutes) { route ->
                                RouteCard(
                                    route = route,
                                    saved = true,
                                    onSaveToggle = { userViewModel.toggleRouteSave(route) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Note: `RouteCard` is defined elsewhere in the project and is already used by the current `ProfileScreen`; this rewrite keeps that call unchanged.

- [x] **Step 2: Wire `messageViewModel` + the UserProfile route in `MainActivity`**

In `MainActivity.kt`, update the existing `Screen.Profile` composable to pass the shared `messageViewModel`:

```kotlin
            composable(Screen.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    messageViewModel = messageViewModel,
                    onLogout = {
                        userViewModel.logout()
                        messageViewModel.clearActiveUser()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
```

Add a new composable for the UserProfile route (place it next to the `Screen.UserGallery` composable):

```kotlin
            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(androidx.navigation.navArgument("authorName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val raw = backStackEntry.arguments?.getString("authorName") ?: ""
                val authorName = java.net.URLDecoder.decode(raw, "UTF-8")
                ProfileScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    messageViewModel = messageViewModel,
                    authorName = authorName
                )
            }
```

- [x] **Step 3: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/MainActivity.kt
git commit -m "feat: view other users' profiles (Follow-only) from reels"
```

---

## Task 6: Multi-select delete in the own gallery grid

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt`

This adds long-press multi-select + a contextual bar + a confirm dialog to the **self** gallery grid only. The other-user grid stays tap-to-open (no selection).

- [x] **Step 1: Add imports**

In `ProfileScreen.kt`, add these imports:

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
```

Add the opt-in to the function annotation (combinedClickable is experimental). Change:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
```

to:

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
```

- [x] **Step 2: Add selection state**

Inside `ProfileScreen`, just after the line `val isFollowing = userViewModel.isFollowing(userData.runnerName)`, add:

```kotlin
    // Multi-select state for the own gallery grid (self mode only).
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selectedCount = selectedIds.count { it.value }
```

- [x] **Step 3: Replace the Gallery section header + grid with the selection-aware version**

Replace the whole `// Gallery section ...` `Column(modifier = Modifier.padding(vertical = 16.dp)) { ... }` block from Task 5 with this version. It swaps the header for a contextual action bar while selecting, and makes self-mode tiles long-pressable with a check overlay:

```kotlin
            // Gallery section — Instagram-style 3-column image grid. Latest at the top.
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // Header doubles as a selection action bar when selecting (self only).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelf && selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel selection", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Text(
                            "$selectedCount selected",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { if (selectedCount > 0) showDeleteConfirm = true },
                            enabled = selectedCount > 0
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "Gallery",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val gallery = galleryViewModel.reels
                if (gallery.isEmpty()) {
                    Text(
                        if (isSelf) "No posts yet. Tap + on Gallery to share your first run."
                        else "${userData.runnerName} hasn't posted any reels yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        gallery.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { item ->
                                    val isChecked = selectedIds[item.id] == true
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelf && selectionMode) {
                                                        if (isChecked) selectedIds.remove(item.id)
                                                        else selectedIds[item.id] = true
                                                        // Leaving selection mode when nothing is left selected.
                                                        if (selectedIds.none { it.value }) selectionMode = false
                                                    } else {
                                                        navController.navigate(userGalleryRoute(item.author))
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isSelf) {
                                                        selectionMode = true
                                                        selectedIds[item.id] = true
                                                    }
                                                }
                                            )
                                    ) {
                                        AsyncImage(
                                            model = item.imageUri ?: item.imageRes,
                                            contentDescription = item.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelf && selectionMode) {
                                            // Dim + check overlay on every tile while selecting.
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = if (isChecked) 0.35f else 0.0f))
                                            )
                                            Icon(
                                                if (isChecked) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Confirm dialog for deleting the selected posts.
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete ${selectedCount} post(s)?") },
                    text = { Text("This removes them from your gallery on all your devices. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val ids = selectedIds.filter { it.value }.keys.toSet()
                            galleryViewModel.deletePosts(ids)
                            selectedIds.clear()
                            selectionMode = false
                            showDeleteConfirm = false
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                    }
                )
            }
```

- [x] **Step 4: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt
git commit -m "feat: long-press multi-select delete in own gallery grid"
```

---

## Task 7: Manual on-device verification

No code changes — install the debug build on a device/emulator (ideally two accounts/devices for cross-device checks) and confirm each behaviour.

- [ ] **Step 1: Install and launch**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew installDebug
```
Expected: `BUILD SUCCESSFUL`, app installs.

- [ ] **Step 2: Avatar upload + own display**

In Edit Profile, change the photo and Save. Confirm the new avatar shows on your own Profile header and on your own reels' name bar in the Gallery feed.

- [ ] **Step 3: Cross-device avatar (two accounts)**

Sign in as user B on a second device/emulator and set a photo. As user A, open the Gallery feed: user B's reel shows B's real photo (not the person-icon placeholder). If you only have one device, verify the placeholder icon shows for a demo author (Sarah/Daniel/Aisha) instead.

- [ ] **Step 4: Open another user's profile**

In the Gallery feed, tap a reel's name/avatar bar for a user that isn't you. Confirm: their profile opens showing avatar + name + location + stats, a single **Follow/Following** button, **no** Edit Profile, **no** Logout/Gallery top-bar actions, **no** Saved section. Tap a gallery tile → their reels open read-only with no delete/selection. Tapping your **own** reel's name bar opens your own Profile (with Edit/Logout).

- [ ] **Step 5: Multi-select delete (own profile)**

On your own Profile, long-press a gallery tile → selection mode starts with that tile checked and the header becomes "1 selected" with a trash + X. Select a couple more, tap trash → confirm dialog → Delete. Confirm the tiles disappear from the grid and the reel(s) are gone from the Gallery feed. Tapping X cancels selection with nothing deleted.

- [ ] **Step 6: Delete propagation (optional, two devices)**

After deleting a post on device A, confirm it also disappears on device A's other sessions / from device B's feed (driven by the `REMOVED` listener handling). 

- [ ] **Step 7: Final commit (if any verification fixes were needed)**

If Steps 2–6 surfaced fixes, commit them with a clear message. Otherwise nothing to commit.

---

## Self-review notes (for the executor)

- Avatars are joined by **display name**; this assumes names are unique (the directory join already de-dupes by `runnerName`). Two real users sharing a name would collide — acceptable for this scope.
- Avatar blobs re-upload **only when a new photo is picked**, not on plain sign-in (a user who set a photo before this feature shipped must re-pick it once to publish the blob).
- `RouteCard` and `compressReelImage` are pre-existing top-level/Composable symbols reused unchanged.
