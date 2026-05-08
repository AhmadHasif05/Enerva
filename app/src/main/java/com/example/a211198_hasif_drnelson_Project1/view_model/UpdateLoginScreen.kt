package com.example.a211198_hasif_drnelson_Project1.view_model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Holds the form state for LoginScreen so it survives rotation, and
// keeps the validation rule in one place.
class LoginViewModel : ViewModel() {

    // Email typed into the form. private set → only onEmailChange can mutate it.
    var email by mutableStateOf("")
        private set

    // Called from the OutlinedTextField's onValueChange callback.
    fun onEmailChange(value: String) {
        email = value
    }

    // Computed property — the "Continue" button is enabled when this is true.
    // Trivial check (non-blank + contains "@") is enough for this demo.
    val isValid: Boolean
        get() = email.isNotBlank() && email.contains("@")

    // Wipe the field, e.g. after a successful login.
    fun reset() {
        email = ""
    }
}