package com.example.a211198_hasif_drnelson_Project2.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Wraps Credential Manager's Google ID flow — the current recommended path now
// that the legacy GoogleSignIn API is deprecated. Returns a Google ID token that
// AuthRepository.signInWithGoogle exchanges for a Firebase session.
//
// Credential Manager needs an Activity context (it shows the system account
// picker), so getIdToken takes the context at call time rather than caching it.
class GoogleSignInHelper(private val webClientId: String) {

    val isConfigured: Boolean get() = webClientId.isNotBlank()

    /**
     * Launches the system account picker and returns the selected account's
     * Google ID token. Throws [NotConfiguredException] if the Web client ID is
     * missing, or propagates Credential Manager exceptions (cancellation,
     * no-credential, etc.) for the caller to map to user-facing messages.
     */
    suspend fun getIdToken(context: Context): String {
        if (!isConfigured) throw NotConfiguredException()

        // GetSignInWithGoogleOption is the button-driven flow: it always shows the
        // full account picker for every Google account on the device. (GetGoogleIdOption
        // is the seamless/bottom-sheet flow and throws NoCredentialException for an
        // explicit button press before any account is authorized — which is the bug
        // we were hitting.)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}")
    }

    class NotConfiguredException : Exception("Google Sign-In not configured")
}
