package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.MEDIA
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.PUBLIC_REELS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.USERS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.MediaDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.PublicReelDoc
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
        imageRes: Int
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
            createdAtMs = now
        )
        activityDao.insertMedia(entity)

        user?.firebaseUid?.let { uid ->
            runCatching {
                // Private copy under the owner.
                firestore.collection(USERS).document(uid).collection(MEDIA).document(id)
                    .set(entity.toDoc()).await()
                // Public copy for the cross-user Gallery feed.
                firestore.collection(PUBLIC_REELS).document(id)
                    .set(entity.toPublicReel(uid)).await()
            }
        }
    }

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
                            activityDao.insertMedia(m.toEntity(ownerEmail = email))
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
                            activityDao.insertMedia(r.toEntity())
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

internal fun MediaEntity.toDoc() = MediaDoc(
    id = id,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    likes = likes,
    createdAtMs = createdAtMs
)

internal fun MediaEntity.toPublicReel(ownerUid: String) = PublicReelDoc(
    id = id,
    ownerUid = ownerUid,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    likes = likes,
    createdAtMs = createdAtMs
)

// Remote reels are stored under a synthetic ownerEmail ("uid:<ownerUid>") so they
// never collide with my own email-keyed posts or leak into my "my posts" view.
internal fun PublicReelDoc.toEntity() = MediaEntity(
    id = id,
    ownerEmail = "uid:$ownerUid",
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    likes = likes,
    createdAtMs = createdAtMs
)

internal fun MediaDoc.toEntity(ownerEmail: String) = MediaEntity(
    id = id,
    ownerEmail = ownerEmail,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    likes = likes,
    createdAtMs = createdAtMs
)
