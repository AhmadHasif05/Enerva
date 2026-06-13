package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.FOLLOWS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.PUBLIC_PROFILES
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.SAVED_ROUTES
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.USERS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FollowDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.PublicProfileDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.SavedRouteDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.UserDoc
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserDirectoryEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity
import com.example.a211198_hasif_drnelson_Project2.model.UserData
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Owns all persistence for the user domain — profile, follows, saved routes.
 *
 * Offline-first contract (plan.md §5.3):
 *  - **Reads** come from Room (instant, offline). The exposed `observe*` flows are
 *    straight Room queries; the UI never blocks on the network.
 *  - **Writes** are write-through: Room first (so the UI updates immediately), then
 *    Firestore. The Firestore SDK queues writes while offline and replays them.
 *  - **Cloud → local** sync runs through snapshot listeners that fold incoming
 *    Firestore changes back into Room, which the read flows already observe.
 *
 * Firestore documents key on the Firebase `uid`; the local Room cache keys on
 * `email`. `firebaseUid` on the user row bridges the two.
 */
class UserRepository(
    db: AppDatabase,
    private val cacheDir: File,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val userDao = db.userDao()
    private val activityDao = db.activityDao()
    private val messageDao = db.messageDao()

    // Active cloud listeners; torn down on logout so a stale collector can't keep
    // writing the previous user's data into Room.
    private val listeners = mutableListOf<ListenerRegistration>()

    // ---- reads (Room is the source of truth) ----

    fun observeProfile(email: String): Flow<UserData?> =
        userDao.observeByEmail(email).map { it?.toModel() }

    /**
     * Everyone *other* than me, for Search. Merges two sources:
     *  - the local `users` table (accounts that signed in on this device, incl.
     *    the seeded demo runners), and
     *  - the public directory mirrored from Firestore (accounts created on *other*
     *    devices). Directory entries whose uid already exists as a local row are
     *    dropped so a user who has signed in here isn't listed twice.
     */
    fun observeOtherUsers(email: String, myUid: String?): Flow<List<UserData>> =
        combine(
            userDao.observeAllExcept(email),
            userDao.observeDirectoryExcept(myUid.orEmpty())
        ) { locals, directory ->
            val localUsers = locals.map { it.toModel() }
            val localUids = localUsers.mapNotNull { it.firebaseUid }.toSet()
            val directoryUsers = directory
                .filter { it.uid !in localUids }
                .map { it.toModel() }
            (localUsers + directoryUsers).distinctBy { it.runnerName }
        }

    fun observeFollowing(email: String): Flow<List<String>> =
        userDao.observeFollowing(email)

    fun observeSavedRoutes(email: String): Flow<List<SavedRouteEntity>> =
        userDao.observeSavedRoutes(email)

    suspend fun findByEmail(email: String): UserData? = userDao.findByEmail(email)?.toModel()

    suspend fun findByName(name: String): UserData? = userDao.findByName(name)?.toModel()

    // ---- session hydration ----

    /**
     * Called right after a successful sign-up. Seeds the local row + the Firestore
     * doc from the chosen display name, then starts listeners.
     */
    suspend fun onSignUp(authUser: AuthRepository.AuthUser, name: String): UserData {
        val profile = UserData(
            runnerName = name.ifBlank { authUser.email.substringBefore('@') },
            email = authUser.email,
            firebaseUid = authUser.uid
        )
        userDao.upsertUser(profile.toEntity())
        pushProfile(profile, create = true)
        return profile
    }

    /**
     * Called after a successful sign-in (email/password or Google). Reconciles the
     * local cache with the cloud:
     *  - If a Firestore `users/{uid}` doc exists → it wins (fresh-install pull).
     *  - Else if a local row exists → keep it and stamp the uid + create the cloud doc.
     *  - Else → seed a minimal profile from the auth account.
     *
     * Always backfills `firebaseUid` on the local row so future queries can map it.
     */
    suspend fun onSignIn(authUser: AuthRepository.AuthUser): UserData {
        val email = authUser.email
        val local = userDao.findByEmail(email)
        val cloudSnap = runCatching {
            firestore.collection(USERS).document(authUser.uid).get().await()
        }.getOrNull()

        val resolved: UserData = when {
            cloudSnap != null && cloudSnap.exists() ->
                cloudSnap.toObject(UserDoc::class.java)!!.toModel(authUser.uid)
            local != null -> local.toModel().copy(firebaseUid = authUser.uid)
            else -> UserData(
                runnerName = authUser.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@'),
                email = email,
                photoUri = authUser.photoUrl,
                firebaseUid = authUser.uid
            )
        }

        userDao.upsertUser(resolved.toEntity())
        // Always (re)publish the profile on sign-in: creates the cloud doc the first
        // time, and guarantees a publicProfiles directory entry exists for users who
        // signed up before discovery shipped. Merge keeps it idempotent.
        pushProfile(resolved, create = cloudSnap == null || !cloudSnap.exists())
        return resolved
    }

    /**
     * Starts real-time Firestore listeners for the signed-in user and folds every
     * change back into Room. [scope] is the caller's ViewModel scope so Room
     * suspend writes outlive the snapshot callback.
     */
    fun startListeners(scope: CoroutineScope, uid: String, email: String) {
        stopListeners()

        // users/{uid} — my profile.
        listeners += firestore.collection(USERS).document(uid)
            .addSnapshotListener { snap, _ ->
                val doc = snap?.takeIf { it.exists() }?.toObject(UserDoc::class.java) ?: return@addSnapshotListener
                scope.launch { userDao.upsertUser(doc.toModel(uid).toEntity()) }
            }

        // users/{uid}/follows/* — my follow list. Reconcile Room to match the cloud.
        listeners += firestore.collection(USERS).document(uid).collection(FOLLOWS)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                val cloudNames = snap.documents.mapNotNull {
                    it.toObject(FollowDoc::class.java)?.friendName?.takeIf { n -> n.isNotBlank() }
                }.toSet()
                scope.launch {
                    val localNames = userDao.observeFollowing(email).first().toSet()
                    (cloudNames - localNames).forEach { userDao.addFollow(FollowEntity(email, it)) }
                    (localNames - cloudNames).forEach { userDao.removeFollow(email, it) }
                }
            }

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
    }

    // Decode an avatar blob to a stable cache file; return its absolute path (or
    // null on failure). Idempotent on uid; Coil renders the file path.
    private fun writeAvatarCache(uid: String, bytes: ByteArray): String? = runCatching {
        val dir = File(cacheDir, "remote_avatars").apply { mkdirs() }
        val file = File(dir, "$uid.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }.getOrNull()

    fun stopListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    // ---- profile writes (write-through) ----

    suspend fun saveProfile(user: UserData, avatarBytes: ByteArray? = null) {
        userDao.upsertUser(user.toEntity())
        pushProfile(user, create = false, avatarBytes = avatarBytes)
    }

    /**
     * Persist a renamed profile and fan the new display name out to every place the
     * old name was copied — reels, and the rows where OTHER users reference this
     * person by name (their conversations, messages, follow entries).
     */
    suspend fun saveProfileWithRename(user: UserData, oldName: String) {
        saveProfile(user)
        propagateRename(oldName, user.runnerName)
    }

    private suspend fun propagateRename(oldName: String, newName: String) {
        if (oldName.isBlank() || oldName == newName) return
        activityDao.renameAuthor(oldName, newName)
        messageDao.renameConversationFriend(oldName, newName)
        messageDao.renameMessageFriend(oldName, newName)
        userDao.renameFollowFriend(oldName, newName)
    }

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

    // ---- follows (write-through, with follower-count bookkeeping) ----

    /**
     * Follow / unfollow [friendName] for the owner identified by [ownerEmail] +
     * [ownerUid]. Updates the owner's following count and the followee's followers
     * count locally, and mirrors the follow edge to Firestore.
     */
    suspend fun toggleFollow(ownerEmail: String, ownerUid: String?, friendName: String) {
        if (ownerEmail.isBlank()) return
        val currentlyFollowing = userDao.observeFollowing(ownerEmail).first().contains(friendName)

        if (currentlyFollowing) {
            userDao.removeFollow(ownerEmail, friendName)
        } else {
            userDao.addFollow(FollowEntity(ownerEmail, friendName))
        }

        // Owner's following count.
        val followingCount = userDao.observeFollowing(ownerEmail).first().size
        userDao.findByEmail(ownerEmail)?.let { me ->
            userDao.upsertUser(me.copy(following = followingCount))
        }

        // Followee's followers count (if they're a registered user on this device).
        userDao.findByName(friendName)?.let { followee ->
            val followers = userDao.countFollowersOf(friendName)
            userDao.upsertUser(followee.copy(followers = followers))
        }

        // Mirror the edge to Firestore under the owner's uid.
        if (ownerUid != null) {
            val followsRef = firestore.collection(USERS).document(ownerUid).collection(FOLLOWS)
            val docId = friendName.toFollowDocId()
            runCatching {
                if (currentlyFollowing) {
                    followsRef.document(docId).delete().await()
                } else {
                    val friendUid = userDao.findByName(friendName)?.firebaseUid
                    followsRef.document(docId)
                        .set(FollowDoc(friendName, friendUid, System.currentTimeMillis()))
                        .await()
                }
            }
        }
    }

    // ---- saved routes (write-through) ----

    suspend fun saveRoute(ownerEmail: String, ownerUid: String?, route: SavedRouteEntity) {
        userDao.saveRoute(route)
        if (ownerUid != null) {
            runCatching {
                firestore.collection(USERS).document(ownerUid)
                    .collection(SAVED_ROUTES).document(route.title.toFollowDocId())
                    .set(route.toDoc()).await()
            }
        }
    }

    suspend fun unsaveRoute(ownerEmail: String, ownerUid: String?, title: String) {
        userDao.unsaveRoute(ownerEmail, title)
        if (ownerUid != null) {
            runCatching {
                firestore.collection(USERS).document(ownerUid)
                    .collection(SAVED_ROUTES).document(title.toFollowDocId()).delete().await()
            }
        }
    }
}

// ---- mappers ----

private fun UserData.toEntity() = UserEntity(
    email = email,
    runnerName = runnerName,
    location = location,
    fitnessLevel = fitnessLevel,
    personalGoal = personalGoal,
    bio = bio,
    following = following,
    followers = followers,
    photoUri = photoUri,
    firebaseUid = firebaseUid
)

private fun UserEntity.toModel() = UserData(
    runnerName = runnerName,
    email = email,
    location = location,
    fitnessLevel = fitnessLevel,
    personalGoal = personalGoal,
    bio = bio,
    following = following,
    followers = followers,
    photoUri = photoUri,
    firebaseUid = firebaseUid
)

// Directory entries only carry public fields; private fields fall back to model
// defaults and email is unknown (stays blank — Search uses runnerName/location).
private fun UserDirectoryEntity.toModel() = UserData(
    runnerName = runnerName,
    location = location,
    fitnessLevel = fitnessLevel,
    photoUri = photoUri,
    firebaseUid = uid
)

private fun UserDoc.toModel(uid: String) = UserData(
    runnerName = runnerName,
    email = email,
    location = location,
    fitnessLevel = fitnessLevel,
    personalGoal = personalGoal,
    bio = bio,
    following = following,
    followers = followers,
    photoUri = photoUri,
    firebaseUid = uid
)

private fun SavedRouteEntity.toDoc() = SavedRouteDoc(
    title = title,
    distance = distance,
    time = time,
    elevation = elevation,
    difficulty = difficulty,
    imageRes = imageRes
)

// Firestore document ids can't contain '/'. Display names / route titles are
// otherwise fine as ids (owner-scoped sub-collection, so collisions only within
// one user's own follows/routes).
internal fun String.toFollowDocId(): String = replace('/', '_').ifBlank { "_" }
