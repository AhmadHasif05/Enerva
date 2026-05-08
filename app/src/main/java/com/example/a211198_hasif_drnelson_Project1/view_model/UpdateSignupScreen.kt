package com.example.a211198_hasif_drnelson_Project1.view_model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Form state for SignupScreen — runner name + email plus a tiny validity check.
class SignupViewModel : ViewModel() {

    // Both fields use `private set` so updates can only happen via the
    // explicit on*Change methods below — keeps mutation paths obvious.
    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    fun onNameChange(value: String) {
        name = value
    }

    fun onEmailChange(value: String) {
        email = value
    }

    // The "Sign Up" button stays disabled until both fields are filled in
    // and the email at least contains an "@".
    val isValid: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && email.contains("@")

    // Clear the form (e.g. after the user successfully signs up).
    fun reset() {
        name = ""
        email = ""
    }
}