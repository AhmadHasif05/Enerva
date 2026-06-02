package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

// Thin wrapper around FirebaseAuth. Returns Result<...> so ViewModels can
// branch on success/failure without touching Firebase exception types.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    data class AuthUser(
        val uid: String,
        val email: String,
        val displayName: String? = null,
        val photoUrl: String? = null
    )

    val currentUser: AuthUser?
        get() = auth.currentUser?.let {
            AuthUser(it.uid, it.email.orEmpty(), it.displayName, it.photoUrl?.toString())
        }

    suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val fbUser = result.user ?: error("Sign-up succeeded but user is null")
        AuthUser(fbUser.uid, fbUser.email.orEmpty())
    }.mapError(::mapAuthError)

    suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val fbUser = result.user ?: error("Sign-in succeeded but user is null")
        AuthUser(fbUser.uid, fbUser.email.orEmpty())
    }.mapError(::mapAuthError)

    /**
     * Exchanges a Google ID token (from Credential Manager) for a Firebase
     * session. The display name comes from the Google account.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val fbUser = result.user ?: error("Google sign-in succeeded but user is null")
        AuthUser(fbUser.uid, fbUser.email.orEmpty(), fbUser.displayName, fbUser.photoUrl?.toString())
    }.mapError(::mapAuthError)

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }.mapError(::mapAuthError)

    private fun mapAuthError(t: Throwable): Throwable = when (t) {
        is FirebaseAuthWeakPasswordException ->
            IllegalArgumentException("Password is too weak (minimum 6 characters)")
        is FirebaseAuthInvalidCredentialsException ->
            IllegalArgumentException("Invalid email or password")
        is FirebaseAuthInvalidUserException ->
            IllegalArgumentException("No account found for that email")
        is FirebaseAuthUserCollisionException ->
            IllegalArgumentException("That email is already registered")
        else -> t
    }

    private inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
}
