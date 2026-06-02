package com.example.a211198_hasif_drnelson_Project2.data.cloud

// Single source of truth for Cloud Firestore collection/sub-collection names and
// the document shapes (DTOs) we read/write. Keeping these here means the
// repositories never hard-code a path string and the schema is documented in one
// place (mirrors plan.md §5.2).
//
// Every DTO gives *all* fields a default value on purpose: Firestore's automatic
// POJO (`toObject`) mapping needs a no-arg constructor, and Kotlin only generates
// one when every constructor parameter has a default.

object FirestoreCollections {
    const val USERS = "users"

    // Sub-collections under users/{uid}
    const val FOLLOWS = "follows"
    const val SAVED_ROUTES = "savedRoutes"
    const val ACTIVITIES = "activities"
    const val MEDIA = "media"

    // Top-level, shared between participants
    const val CONVERSATIONS = "conversations"
    const val MESSAGES = "messages"
}

/** users/{uid} */
data class UserDoc(
    val uid: String = "",
    val email: String = "",
    val runnerName: String = "",
    val location: String = "",
    val fitnessLevel: String = "",
    val personalGoal: String = "",
    val bio: String = "",
    val following: Int = 0,
    val followers: Int = 0,
    val photoUri: String? = null, // device-local content:// uri; real cloud photos arrive in Phase 5 (Storage)
    val createdAt: Long = 0L
)

/** users/{uid}/follows/{friendKey} */
data class FollowDoc(
    val friendName: String = "",
    val friendUid: String? = null,
    val createdAt: Long = 0L
)

/** users/{uid}/savedRoutes/{title} */
data class SavedRouteDoc(
    val title: String = "",
    val distance: String = "",
    val time: String = "",
    val elevation: String = "",
    val difficulty: String = "",
    val imageRes: Int = 0
)

/** users/{uid}/activities/{activityId} */
data class ActivityDoc(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val date: String = "",
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val elevationM: Int = 0,
    val avgPace: String = ""
)

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
    val likes: Int = 0,
    val createdAtMs: Long = 0L
)

/** conversations/{conversationId} (top-level, shared) */
data class ConversationDoc(
    val conversationId: String = "",
    val participants: List<String> = emptyList(), // Firebase uids
    // uid -> display name, denormalised so a participant can label the chat
    // without reading the other user's (owner-only) profile doc.
    val participantNames: Map<String, String> = emptyMap(),
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val lastMessageAt: Long = 0L
)

/** conversations/{conversationId}/messages/{messageId} */
data class MessageDoc(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val timestampMs: Long = 0L
)
