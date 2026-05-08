// LoginScreen — collects an email, validates it via LoginViewModel, and
// hands the result back to MainActivity through onContinueClick.
package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a211198_hasif_drnelson_Project1.view_model.LoginViewModel

// Material 3 components like TopAppBar are still flagged as experimental,
// hence the opt-in annotation here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackClick: () -> Unit = {},                                  // Called when the back arrow is tapped
    onContinueClick: (String, String) -> Unit = { _, _ -> },       // (name, email) callback after the user taps Continue
    onGoogleSignIn: () -> Unit = {},                               // Hook for a real Google flow later
    onFacebookSignIn: () -> Unit = {},                             // Hook for a real Facebook flow later
    loginViewModel: LoginViewModel = viewModel()                   // Form state lives here so it survives rotation
) {
    // Read the current email and the validity flag from the ViewModel.
    val email = loginViewModel.email
    val isValid = loginViewModel.isValid

    Scaffold(
        topBar = {
            // Slim top bar — just the back button on the left.
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // Continue button — disabled until the email looks valid.
            Button(
                onClick = { onContinueClick("", email) }, // First arg unused on this screen
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

            // Two stub social-login buttons — wire onGoogleSignIn / onFacebookSignIn
            // to real auth providers when you have them.
            SocialLoginButton(text = "Continue with Google", onClick = onGoogleSignIn)

            Spacer(modifier = Modifier.height(16.dp))

            SocialLoginButton(text = "Continue with Facebook", onClick = onFacebookSignIn)

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