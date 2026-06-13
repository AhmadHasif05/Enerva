package com.example.a211198_hasif_drnelson_Project2.data.cloud

import com.google.firebase.firestore.Blob

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

    // Top-level, readable by any signed-in user (cross-device discovery):
    //  - publicProfiles/{uid} → the public slice of a user's profile (directory)
    //  - publicReels/{mediaId} → reels surfaced in the cross-user Gallery feed
    const val PUBLIC_PROFILES = "publicProfiles"
    const val PUBLIC_REELS = "publicReels"
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
    val imageRes: Int = 0,
    val imageUrl: String? = null
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
    val imageBlob: Blob? = null,
    val isCard: Boolean = false,
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
