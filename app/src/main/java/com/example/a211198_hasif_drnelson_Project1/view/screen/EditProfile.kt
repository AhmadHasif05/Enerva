package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project1.R
import com.example.a211198_hasif_drnelson_Project1.view_model.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val current = userViewModel.userProfile

    var name by remember { mutableStateOf(current.runnerName) }
    var email by remember { mutableStateOf(current.email) }
    var location by remember { mutableStateOf(current.location) }
    var fitnessLevel by remember { mutableStateOf(current.fitnessLevel) }
    var personalGoal by remember { mutableStateOf(current.personalGoal) }
    var levelMenuOpen by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            userViewModel.updateProfile(
                                name = name,
                                email = email,
                                location = location,
                                fitnessLevel = fitnessLevel,
                                personalGoal = personalGoal
                            )
                            navController.popBackStack()
                        },
                        enabled = isValid) {}
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar with change-photo affordance
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable { /* TODO: pick image */ }
            ) {
                AsyncImage(
                    model = R.drawable.hasif_profile,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                "Change photo",
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clickable { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            EditField(
                label = "Runner Name",
                value = name,
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditField(
                label = "Location",
                value = location,
                onValueChange = { location = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fitness level dropdown
            Text(
                "Fitness Level",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = levelMenuOpen,
                onExpandedChange = { levelMenuOpen = !levelMenuOpen }
            ) {
                OutlinedTextField(
                    value = fitnessLevel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelMenuOpen) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onBackground,
                        unfocusedTextColor = colors.onBackground,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = levelMenuOpen,
                    onDismissRequest = { levelMenuOpen = false }
                ) {
                    listOf("Beginner", "Intermediate", "Advanced", "Elite").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                fitnessLevel = option
                                levelMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EditField(
                label = "Personal Goal",
                value = personalGoal,
                onValueChange = { personalGoal = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    userViewModel.updateProfile(
                        name = name,
                        email = email,
                        location = location,
                        fitnessLevel = fitnessLevel,
                        personalGoal = personalGoal
                    )
                    navController.popBackStack()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f),
                    disabledContentColor = colors.onSurfaceVariant
                )
            ) {
                Text(
                    "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            label,
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline,
                cursorColor = colors.primary
            )
        )
    }
}

@Preview
@Composable
fun EditProfileScreenPreview() {
    EditProfileScreen(rememberNavController())
}