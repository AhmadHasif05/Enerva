// LoginScreen — collects an email, validates it via LoginViewModel, and
// hands the result back to MainActivity through onContinueClick.
package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.LoginViewModel

// Material 3 components like TopAppBar are still flagged as experimental,
// hence the opt-in annotation here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onContinueClick: (String, String) -> Unit = { _, _ -> },       // (email, password) callback after the user taps Continue
    onGoogleSignIn: () -> Unit = {},                               // Logs the user in via Google
    onSignUpClick: () -> Unit = {},                                // Sends the user to the Sign Up screen
    onForgotPassword: (String, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(false, "Not wired") },
    loginViewModel: LoginViewModel = viewModel()                   // Form state lives here so it survives rotation
) {
    val email = loginViewModel.email                          // typed email (ViewModel-owned)
    val password = loginViewModel.password                    // typed password
    val isValid = loginViewModel.isValid                      // true → enables Continue
    var passwordVisible by remember { mutableStateOf(false) }  // eye-toggle for the password field
    var showForgotDialog by remember { mutableStateOf(false) } // whether the reset dialog is open

    if (showForgotDialog) {                                   // only compose the dialog when requested
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotDialog = false },
            onSend = onForgotPassword
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Page heading
            Text(
                text = "Log In to Enerva",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field — every keystroke flows through the ViewModel.
            OutlinedTextField(
                value = email,
                onValueChange = loginViewModel::onEmailChange,
                label = { Text("Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field — show/hide eye toggle, min 6 chars enforced by validator.
            OutlinedTextField(
                value = password,
                onValueChange = loginViewModel::onPasswordChange,
                label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility
                                          else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            // "Forgot password?" link — aligned to the end, opens the reset dialog.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showForgotDialog = true }) {
                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Continue button — disabled until email + password are valid.
            Button(
                onClick = { onContinueClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = isValid
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "or" divider between primary action and the social options.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "or",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Google sign-in — the only social option now.
            SocialLoginButton(text = "Continue with Google", onClick = onGoogleSignIn)

            Spacer(modifier = Modifier.height(24.dp))

            // Sign-up prompt — a plain text link, deliberately styled differently
            // from the Google button above.
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't join us yet? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Join us now",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onSignUpClick)
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // Push legal text to the bottom

            // Inline-styled "ToS / Privacy" line — `buildAnnotatedString` lets us
            // underline parts of the same Text without splitting it into multiple
            // composables.
            val annotatedString = buildAnnotatedString {
                append("By continuing, you are agreeing to our ")
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("Terms of Service")
                }
                append(" and ")
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("Privacy Policy")
                }
                append(".")
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

// Forgot password dialog — collects an email and calls onSend, which delegates
// to AuthRepository.sendPasswordReset via the ViewModel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSend: (String, (Boolean, String?) -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current // for Toasts
    var dialogEmail by remember { mutableStateOf(initialEmail) }     // editable email, seeded from the login field
    var sending by remember { mutableStateOf(false) }               // true while the reset request is in flight

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Reset password") },
        text = {
            Column {
                Text(
                    "Enter your account email. We'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dialogEmail,
                    onValueChange = { dialogEmail = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending && dialogEmail.contains('@'), // need a plausible email and not already sending
                onClick = {
                    sending = true                            // show the "Sending..." label, block re-taps
                    onSend(dialogEmail.trim()) { success, error -> // callback fires when the reset request returns
                        sending = false
                        if (success) {                        // toast + close on success
                            android.widget.Toast.makeText(
                                context,
                                "Reset email sent. Check your inbox.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                error ?: "Could not send reset email",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) { Text(if (sending) "Sending..." else "Send reset link") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancel") }
        }
    )
}

// Reusable outlined button used for the social login options.
@Composable
fun SocialLoginButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

// Lets us inspect the screen in the IDE preview without running the app.
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen()
    }
}