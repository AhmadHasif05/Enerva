package com.example.a211198_hasif_drnelson_Project2.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Registered users. Email stays the local primary key (every query + the offline
// cache is keyed on it), but Firestore keys on the Firebase uid — so we carry
// `firebaseUid` as an indexed column to map each local row to its cloud doc.
// Null until the user first signs in against Firebase on this device (backfilled
// by the repository layer in 5.3).
@Entity(tableName = "users", indices = [Index("firebaseUid")])
data class UserEntity(
    @PrimaryKey val email: String,
    val runnerName: String,
    val location: String,
    val fitnessLevel: String,
    val personalGoal: String,
    val bio: String,
    val following: Int,
    val followers: Int,
    val photoUri: String? = null,
    val firebaseUid: String? = null
)

// Public, cross-device directory of other users (mirror of Firestore
// `publicProfiles/{uid}`). Keyed by Firebase uid (we never see other users'
// emails — those stay private). Holds only the public fields needed to discover
// + display someone in Search and on their profile. Populated by the
// publicProfiles snapshot listener; `users` stays the private, email-keyed cache
// of accounts that have actually signed in on THIS device.
@Entity(tableName = "user_directory")
data class UserDirectoryEntity(
    @PrimaryKey val uid: String,
    val runnerName: String,
    val location: String,
    val fitnessLevel: String,
    val photoUri: String? = null
)

// A weekend route the user bookmarked. Composite key = ownerEmail+title.
@Entity(tableName = "saved_routes", primaryKeys = ["ownerEmail", "title"])
data class SavedRouteEntity(
    val ownerEmail: String,
    val title: String,
    val distance: String,
    val time: String,
    val elevation: String,
    val difficulty: String,
    val imageRes: Int
)

// Who the user is following. Composite key = ownerEmail+friendName.
@Entity(tableName = "follows", primaryKeys = ["ownerEmail", "friendName"])
data class FollowEntity(
    val ownerEmail: String,
    val friendName: String
)

// A completed run/walk/ride. id is a UUID string from the model.
@Entity(tableName = "activities")
data class ActivityRecordEntity(
    @PrimaryKey val id: String,
    val ownerEmail: String,
    val type: String,
    val title: String,
    val date: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val elevationM: Int,
    val avgPace: String
)

// A 1:1 or group conversation. friendName doubles as the group name when isGroup.
// members stored as CSV — small enough that a join table is overkill.
// `conversationId` is the shared, top-level Firestore conversation id that both
// participants reference (uid-based). Null until reconciled by the repository
// layer in 5.3; the local (ownerEmail, friendName) key still drives all queries.
@Entity(tableName = "conversations", primaryKeys = ["ownerEmail", "friendName"])
data class ConversationEntity(
    val ownerEmail: String,
    val friendName: String,
    val isGroup: Boolean,
    val membersCsv: String,
    val conversationId: String? = null
)

// One chat message inside a conversation. `conversationId` mirrors the parent
// ConversationEntity's shared Firestore id (null until reconciled in 5.3).
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val ownerEmail: String,
    val friendName: String,
    val fromMe: Boolean,
    val text: String,
    val timestampMs: Long,
    val conversationId: String? = null
)

// One gallery reel — captured activity or seeded sample. author is the display
// name (matches GalleryActivity.author). tint stored as ARGB long.
@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: String,
    val ownerEmail: String,
    val author: String,
    val caption: String,
    val activity: String,
    val distanceKm: String,
    val tint: Long,
    val imageRes: Int,
    val imageUri: String?, // null for seeded reels using imageRes; set for user posts
    val likes: Int,
    val createdAtMs: Long,
    // True when imageUri is a branded run-summary card (≈square) the reel renderer
    // should Fit rather than Crop. Defaults false: seeded/demo/remote reels and
    // raw photo posts are full-bleed. Device-local (not synced to Firestore — the
    // card image itself doesn't sync, so remote copies stay drawable-backed Crop).
    val isCard: Boolean = false
)
