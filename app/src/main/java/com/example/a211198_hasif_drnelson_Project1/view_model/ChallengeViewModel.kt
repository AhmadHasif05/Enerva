package com.example.a211198_hasif_drnelson_Project1.view_model

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.a211198_hasif_drnelson_Project1.model.Challenge
import com.example.a211198_hasif_drnelson_Project1.model.Club
import com.example.a211198_hasif_drnelson_Project1.model.sampleChallenges
import com.example.a211198_hasif_drnelson_Project1.model.sampleClubs

// Backs the Groups screen — challenges + clubs and the user's join state.
class ChallengeViewModel : ViewModel() {

    // Master list of challenges. Starts from the seed data in Challenges.kt.
    var challenges by mutableStateOf<List<Challenge>>(sampleChallenges)
        private set

    // Master list of clubs.
    var clubs by mutableStateOf<List<Club>>(sampleClubs)
        private set

    // Per-challenge progress in 0f..1f. Drives the LinearProgressIndicator
    // on the Active tab. Map so we only store entries for joined challenges.
    private val challengeProgress = mutableStateMapOf<String, Float>()

    // Filtered view: only the challenges the user has joined. Re-evaluated
    // whenever `challenges` changes thanks to derivedStateOf.
    val joinedChallenges: List<Challenge> by derivedStateOf {
        challenges.filter { it.isJoined }
    }

    // Same idea for clubs.
    val joinedClubs: List<Club> by derivedStateOf {
        clubs.filter { it.isJoined }
    }

    // Flip the join flag for one challenge and reset / seed its progress.
    fun toggleJoinChallenge(id: String) {
        challenges = challenges.map { c ->
            if (c.id == id) c.copy(isJoined = !c.isJoined) else c
        }
        if (challenges.none { it.id == id && it.isJoined }) {
            // Just left → forget the progress.
            challengeProgress.remove(id)
        } else if (challengeProgress[id] == null) {
            // Just joined → start at 0%.
            challengeProgress[id] = 0f
        }
    }

    fun toggleJoinClub(id: String) {
        clubs = clubs.map { c ->
            if (c.id == id) c.copy(isJoined = !c.isJoined) else c
        }
    }

    // Read current progress (0f if the challenge isn't joined).
    fun progressFor(challengeId: String): Float = challengeProgress[challengeId] ?: 0f

    // Demo helper for the "Log run" button — bumps progress by `delta` and
    // clamps to 1f so the bar never overshoots.
    fun bumpProgress(challengeId: String, delta: Float = 0.1f) {
        val current = challengeProgress[challengeId] ?: 0f
        challengeProgress[challengeId] = (current + delta).coerceIn(0f, 1f)
    }
}