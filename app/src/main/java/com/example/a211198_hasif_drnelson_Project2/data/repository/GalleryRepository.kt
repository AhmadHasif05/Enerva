package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.MEDIA
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.PUBLIC_REELS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.USERS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.MediaDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.PublicReelDoc
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owns gallery (reel) persistence.
 *
 * Scope of cloud sync (plan.md §5.2 + §5.5):
 *  - A user's **own** reels sync write-through to `users/{uid}/media/{id}`, and a
 *    listener pulls them back into Room — so a fresh install restores *your* reels.
 *  - The **cross-user feed** (`observeFeed`) stays Room-backed with locally-seeded
 *    demo authors. The owner-scoped security rules (only the owner can read their
 *    own `users/{uid}` subtree) deliberately prevent reading other users' media, so
 *    a real cross-user social feed would need a public top-level collection or a
 *    follow-based fan-out — out of scope for Phase 3.
 *
 * Image note: `imageRes` (drawable id) and `imageUri` (content:// uri) are
 * device-local, so synced reels carry their text/stats across devices but not the
 * picture. Real cloud images arrive with Firebase Storage in Phase 5.
 */
class GalleryRepository(
    db: AppDatabase,
    private val cacheDir: File,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val activityDao = db.activityDao()
    private val userDao = db.userDao()

    private var mediaListener: ListenerRegistration? = null
    private var feedListener: ListenerRegistration? = null

    // ---- reads (Room is the source of truth) ----

    fun observeFeed(): Flow<List<MediaEntity>> = activityDao.observeAllMedia()
    fun observeMine(email: String): Flow<List<MediaEntity>> = activityDao.observeMedia(email)
    fun observeByAuthor(authorName: String): Flow<List<MediaEntity>> =
        activityDao.observeMediaByAuthor(authorName)

    // ---- local seeding passthrough (demo scaffolding owned by the VM) ----

    suspend fun countMedia(email: String): Int = activityDao.countMedia(email)
    suspend fun insertAllMedia(media: List<MediaEntity>) = activityDao.insertAllMedia(media)

    // ---- writes (write-through) ----

    /**
     * Create a reel for the active user. Written to Room and (if the user has a
     * cloud identity) to `users/{uid}/media/{id}` under the *same* id, so the
     * own-media listener echo is idempotent.
     */
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

    // ---- own-media sync ----

    /** Listen to my own `users/{uid}/media` and fold changes into Room. */
    fun startMineSync(scope: CoroutineScope, email: String) {
        stopSync()
        scope.launch {
            val uid = userDao.findByEmail(email)?.firebaseUid ?: return@launch
            mediaListener = firestore.collection(USERS).document(uid).collection(MEDIA)
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
        }
    }

    /**
     * Listen to the public reels feed and fold *other users'* reels into Room so
     * the cross-user Gallery shows posts created on other devices. My own reels are
     * skipped here — they already arrive via [startMineSync] under my real email.
     */
    fun startFeedSync(scope: CoroutineScope, email: String) {
        feedListener?.remove()
        scope.launch {
            val myUid = userDao.findByEmail(email)?.firebaseUid
            feedListener = firestore.collection(PUBLIC_REELS)
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
        }
    }

    fun stopSync() {
        mediaListener?.remove()
        mediaListener = null
        feedListener?.remove()
        feedListener = null
    }
}

// ---- mappers ----

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
