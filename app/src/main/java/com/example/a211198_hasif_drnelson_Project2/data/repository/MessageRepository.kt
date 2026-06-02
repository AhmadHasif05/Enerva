package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.cloud.ConversationDoc
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.CONVERSATIONS
import com.example.a211198_hasif_drnelson_Project2.data.cloud.FirestoreCollections.MESSAGES
import com.example.a211198_hasif_drnelson_Project2.data.cloud.MessageDoc
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Owns conversation + message persistence.
 *
 * Local model: Room rows keyed by (ownerEmail, friendName) — friendName is the
 * other party's display name for 1:1, or the group name for groups. This is what
 * every screen reads.
 *
 * Cloud model (plan.md §5.2): top-level `conversations/{conversationId}` shared by
 * both participants, with a `messages` sub-collection. Participants are referenced
 * by Firebase uid.
 *
 * Bridge:
 *  - **1:1** → `conversationId` is derived deterministically from the two sorted
 *    uids, so both devices compute the same id without a lookup.
 *  - **Group** → `conversationId` is a generated UUID stored on the local row.
 *  - A message is written to Room and Firestore under the **same id**, so when the
 *    listener pulls it back the Room upsert is idempotent (no duplicate). That also
 *    removes the old single-device hack of mirroring a copy into the recipient's
 *    rows — real cross-user delivery now flows through Firestore.
 *
 * Users without a `firebaseUid` (seeded demo runners) have no cloud identity, so
 * conversations with them stay local-only — the app still works offline / in demos.
 */
class MessageRepository(
    db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val messageDao = db.messageDao()
    private val userDao = db.userDao()

    // Top-level "my conversations" listener.
    private var conversationsListener: ListenerRegistration? = null
    // Per-conversation message listeners, keyed by conversationId.
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    // ---- reads (Room is the source of truth) ----

    fun observeConversations(email: String): Flow<List<ConversationEntity>> =
        messageDao.observeConversations(email)

    fun observeAllMessages(email: String): Flow<List<MessageEntity>> =
        messageDao.observeAllMessages(email)

    // ---- writes (write-through: Room first, then Firestore) ----

    /** Opens a 1:1 conversation with a friendly opening line (local flavour). */
    suspend fun startConversationWith(ownerEmail: String, friendName: String) {
        if (ownerEmail.isBlank()) return
        if (messageDao.findConversation(ownerEmail, friendName) != null) return

        val cid = resolveConversationId(ownerEmail, friendName, isGroup = false)
        messageDao.upsertConversation(
            ConversationEntity(ownerEmail, friendName, isGroup = false, membersCsv = "", conversationId = cid)
        )
        // Opening line is local-only flavour — fabricated "incoming", not sent by us.
        messageDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                ownerEmail = ownerEmail,
                friendName = friendName,
                fromMe = false,
                text = "Hey! Thanks for the follow 👋 Let's go for a run sometime.",
                timestampMs = System.currentTimeMillis(),
                conversationId = cid
            )
        )
    }

    suspend fun sendMessage(ownerEmail: String, friendName: String, text: String) {
        if (ownerEmail.isBlank() || text.isBlank()) return
        val body = text.trim()
        val now = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()

        val existing = messageDao.findConversation(ownerEmail, friendName)
        val isGroup = existing?.isGroup == true
        val cid = existing?.conversationId ?: resolveConversationId(ownerEmail, friendName, isGroup)

        if (existing == null) {
            messageDao.upsertConversation(
                ConversationEntity(ownerEmail, friendName, isGroup = false, membersCsv = "", conversationId = cid)
            )
        }
        // Local copy — same id we'll use in Firestore, so the listener echo is idempotent.
        messageDao.insertMessage(
            MessageEntity(msgId, ownerEmail, friendName, fromMe = true, text = body, timestampMs = now, conversationId = cid)
        )

        // Cloud write — only if we and the conversation have a cloud identity.
        val myUid = userDao.findByEmail(ownerEmail)?.firebaseUid ?: return
        val resolved = resolveParticipants(ownerEmail, friendName, isGroup, existing?.membersCsv.orEmpty(), myUid)
            ?: return // no resolvable cloud counterpart → stay local-only (e.g. demo user)
        val (participants, participantNames) = resolved

        val convoRef = firestore.collection(CONVERSATIONS).document(cid)
        runCatching {
            convoRef.set(
                mapOf(
                    "conversationId" to cid,
                    "participants" to participants,
                    "participantNames" to participantNames,
                    "isGroup" to isGroup,
                    "groupName" to if (isGroup) friendName else null,
                    "lastMessageAt" to now
                ),
                SetOptions.merge()
            ).await()
            convoRef.collection(MESSAGES).document(msgId)
                .set(MessageDoc(msgId, myUid, body, now)).await()
        }
    }

    suspend fun removeConversation(ownerEmail: String, friendName: String) {
        if (ownerEmail.isBlank()) return
        messageDao.deleteMessagesFor(ownerEmail, friendName)
        messageDao.deleteConversation(ownerEmail, friendName)
        // Note: we only drop the local view. The shared Firestore conversation is
        // left intact so the other participant keeps their history.
    }

    suspend fun createGroup(ownerEmail: String, groupName: String, members: List<String>) {
        if (ownerEmail.isBlank()) return
        val name = groupName.trim()
        if (name.isBlank() || members.isEmpty()) return
        if (messageDao.findConversation(ownerEmail, name) != null) return

        val cid = UUID.randomUUID().toString()
        val membersCsv = members.joinToString(",")
        val now = System.currentTimeMillis()
        val openingId = UUID.randomUUID().toString()

        messageDao.upsertConversation(
            ConversationEntity(ownerEmail, name, isGroup = true, membersCsv = membersCsv, conversationId = cid)
        )
        messageDao.insertMessage(
            MessageEntity(
                id = openingId,
                ownerEmail = ownerEmail,
                friendName = name,
                fromMe = false,
                text = "Group \"$name\" created with ${members.joinToString(", ")} 🎉",
                timestampMs = now,
                conversationId = cid
            )
        )

        // Cloud: create the shared conversation with every member that has a cloud id.
        val myUid = userDao.findByEmail(ownerEmail)?.firebaseUid ?: return
        val myName = userDao.findByEmail(ownerEmail)?.runnerName ?: ownerEmail.substringBefore('@')
        val names = linkedMapOf(myUid to myName)
        members.forEach { member ->
            uidForName(member)?.let { names[it] = member }
        }
        runCatching {
            firestore.collection(CONVERSATIONS).document(cid).set(
                ConversationDoc(
                    conversationId = cid,
                    participants = names.keys.toList(),
                    participantNames = names,
                    isGroup = true,
                    groupName = name,
                    lastMessageAt = now
                )
            ).await()
        }
    }

    // ---- real-time sync ----

    /**
     * Starts listening to every conversation this user participates in, and folds
     * incoming conversations + messages into Room. [scope] is the caller's
     * ViewModel scope so Room writes outlive the snapshot callbacks.
     */
    fun startSync(scope: CoroutineScope, ownerEmail: String) {
        stopSync()
        scope.launch {
            val myUid = userDao.findByEmail(ownerEmail)?.firebaseUid ?: return@launch
            conversationsListener = firestore.collection(CONVERSATIONS)
                .whereArrayContains("participants", myUid)
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    for (doc in snap.documents) {
                        val convo = doc.toObject(ConversationDoc::class.java) ?: continue
                        val cid = doc.id
                        scope.launch { reconcileConversation(ownerEmail, myUid, cid, convo) }
                        attachMessageListener(scope, ownerEmail, myUid, cid)
                    }
                }
        }
    }

    fun stopSync() {
        conversationsListener?.remove()
        conversationsListener = null
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    private fun attachMessageListener(scope: CoroutineScope, ownerEmail: String, myUid: String, cid: String) {
        if (messageListeners.containsKey(cid)) return
        messageListeners[cid] = firestore.collection(CONVERSATIONS).document(cid)
            .collection(MESSAGES)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                scope.launch {
                    val convo = messageDao.findConversationByCloudId(cid) ?: return@launch
                    for (mDoc in snap.documents) {
                        val m = mDoc.toObject(MessageDoc::class.java) ?: continue
                        messageDao.insertMessage(
                            MessageEntity(
                                id = mDoc.id,
                                ownerEmail = ownerEmail,
                                friendName = convo.friendName,
                                fromMe = m.senderUid == myUid,
                                text = m.text,
                                timestampMs = m.timestampMs,
                                conversationId = cid
                            )
                        )
                    }
                }
            }
    }

    /** Ensure a local ConversationEntity exists for a cloud conversation. */
    private suspend fun reconcileConversation(ownerEmail: String, myUid: String, cid: String, convo: ConversationDoc) {
        val friendName: String
        val membersCsv: String
        if (convo.isGroup) {
            friendName = convo.groupName?.takeIf { it.isNotBlank() } ?: "Group"
            membersCsv = convo.participants.filter { it != myUid }
                .mapNotNull { nameForParticipant(convo, it) }
                .joinToString(",")
        } else {
            val otherUid = convo.participants.firstOrNull { it != myUid }
            friendName = otherUid?.let { nameForParticipant(convo, it) } ?: "Runner"
            membersCsv = ""
        }
        // Preserve an existing local row's friendName if one already maps to this cid
        // (the sender created it under the name they typed).
        val existing = messageDao.findConversationByCloudId(cid)
        if (existing == null) {
            messageDao.upsertConversation(
                ConversationEntity(ownerEmail, friendName, convo.isGroup, membersCsv, cid)
            )
        }
    }

    /**
     * Display name for a participant uid. Uses the names denormalised on the
     * conversation doc first (the security rules forbid reading another user's
     * profile), then falls back to the local cache.
     */
    private suspend fun nameForParticipant(convo: ConversationDoc, uid: String): String? =
        convo.participantNames[uid]?.takeIf { it.isNotBlank() }
            ?: userDao.findByFirebaseUid(uid)?.runnerName

    // ---- helpers ----

    /** A user's Firebase uid by display name — local account first, then the directory. */
    private suspend fun uidForName(name: String): String? =
        userDao.findByName(name)?.firebaseUid ?: userDao.findDirectoryByName(name)?.uid

    /** Deterministic id for a 1:1 (sorted uids); UUID for a group / unresolved peer. */
    private suspend fun resolveConversationId(ownerEmail: String, friendName: String, isGroup: Boolean): String {
        if (isGroup) return UUID.randomUUID().toString()
        val myUid = userDao.findByEmail(ownerEmail)?.firebaseUid
        val friendUid = uidForName(friendName)
        return if (myUid != null && friendUid != null) {
            listOf(myUid, friendUid).sorted().joinToString("_")
        } else {
            UUID.randomUUID().toString()
        }
    }

    /**
     * Participants (uids) + their display names for a Firestore conversation, or
     * null if there's no resolvable cloud counterpart (e.g. a demo runner with no
     * `firebaseUid`) → the caller keeps the conversation local-only.
     */
    private suspend fun resolveParticipants(
        ownerEmail: String,
        friendName: String,
        isGroup: Boolean,
        membersCsv: String,
        myUid: String
    ): Pair<List<String>, Map<String, String>>? {
        val myName = userDao.findByEmail(ownerEmail)?.runnerName ?: ownerEmail.substringBefore('@')
        val names = linkedMapOf(myUid to myName)
        return if (isGroup) {
            membersCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { member ->
                uidForName(member)?.let { names[it] = member }
            }
            Pair(names.keys.toList(), names)
        } else {
            val friendUid = uidForName(friendName) ?: return null
            names[friendUid] = friendName
            Pair(listOf(myUid, friendUid).sorted(), names)
        }
    }
}
