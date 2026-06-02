package com.example.a211198_hasif_drnelson_Project2.view_model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Form state for SignupScreen — runner name, email, password, confirm.
class SignupViewModel : ViewModel() {

    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    fun onNameChange(value: String) { name = value }
    fun onEmailChange(value: String) { email = value }
    fun onPasswordChange(value: String) { password = value }
    fun onConfirmPasswordChange(value: String) { confirmPassword = value }

    val passwordsMatch: Boolean
        get() = password == confirmPassword

    val isValid: Boolean
        get() = name.isNotBlank()
            && email.isNotBlank()
            && email.contains("@")
            && password.length >= 6
            && passwordsMatch

    fun reset() {
        name = ""
        email = ""
        password = ""
        confirmPassword = ""
    }
}
