package com.example.a211198_hasif_drnelson_Project2.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Registered users. Email is the natural key — login and follow lookups use it.
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val runnerName: String,
    val location: String,
    val fitnessLevel: String,
    val personalGoal: String,
    val bio: String,
    val following: Int,
    val followers: Int,
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
@Entity(tableName = "conversations", primaryKeys = ["ownerEmail", "friendName"])
data class ConversationEntity(
    val ownerEmail: String,
    val friendName: String,
    val isGroup: Boolean,
    val membersCsv: String
)

// One chat message inside a conversation.
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val ownerEmail: String,
    val friendName: String,
    val fromMe: Boolean,
    val text: String,
    val timestampMs: Long
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
    val createdAtMs: Long
)
