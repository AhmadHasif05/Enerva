package com.example.a211198_hasif_drnelson_Project2.view_model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Holds the form state for LoginScreen so it survives rotation, and
// keeps the validation rules in one place.
class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun onEmailChange(value: String) {
        email = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    // Firebase Auth requires passwords of at least 6 characters.
    val isValid: Boolean
        get() = email.isNotBlank() && email.contains("@") && password.length >= 6

    fun reset() {
        email = ""
        password = ""
    }
}
