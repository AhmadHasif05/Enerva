// Package — UI "screen" layer.
package com.example.a211198_hasif_drnelson_Project2.view.screen

// ---- Compose & Android imports ----
import androidx.compose.foundation.background                       // background colour fill
import androidx.compose.foundation.clickable                        // makes the avatar / "change photo" tappable
import androidx.compose.foundation.layout.*                         // Column, Row, Box, Spacer, padding, size…
import androidx.compose.foundation.rememberScrollState              // remembers scroll position
import androidx.compose.foundation.shape.CircleShape                // round avatar
import androidx.compose.foundation.shape.RoundedCornerShape         // rounded fields/buttons
import androidx.compose.foundation.text.KeyboardOptions             // configures the soft keyboard (e.g. email type)
import androidx.compose.foundation.verticalScroll                   // makes the form scrollable
import androidx.compose.material.icons.Icons                        // Material icon set
import androidx.compose.material.icons.automirrored.rounded.ArrowBack // RTL-aware back arrow
import androidx.compose.material.icons.rounded.Check                // check icon (available for the save action)
import androidx.compose.material3.*                                 // Scaffold, TopAppBar, OutlinedTextField, Button…
import androidx.compose.runtime.*                                   // remember, mutableStateOf, Composable, by…
import androidx.compose.ui.Alignment                               // alignment constants
import androidx.compose.ui.Modifier                                // modifier chain
import androidx.compose.ui.draw.clip                               // clip avatar to a circle
import androidx.compose.ui.layout.ContentScale                     // crop the avatar image to fill
import androidx.compose.ui.text.font.FontWeight                    // bold/semibold text
import androidx.compose.ui.text.input.KeyboardType                 // keyboard variants (Text, Email…)
import androidx.compose.ui.tooling.preview.Preview                 // Studio design preview
import androidx.compose.ui.unit.dp                                 // dp sizes
import androidx.compose.ui.unit.sp                                 // sp font sizes
import androidx.lifecycle.viewmodel.compose.viewModel              // obtain the UserViewModel
import androidx.navigation.NavController                           // go back after saving
import androidx.navigation.compose.rememberNavController           // fake controller for @Preview
import coil.compose.AsyncImage                                     // loads the avatar from a URI/drawable
import com.example.a211198_hasif_drnelson_Project2.R              // resource ids (default avatar drawable)
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel // holds + saves the profile

// EditProfileScreen — a scrollable form to edit the signed-in user's profile:
// avatar, name, email, location, fitness level, goal and bio. Local edit state is
// held here and only pushed to the ViewModel (Room + Firestore) on Save.
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar / ExposedDropdownMenuBox are experimental
@Composable
fun EditProfileScreen(
    navController: NavController,                              // to pop back after saving / cancelling
    userViewModel: UserViewModel = viewModel()                // source of the current profile + save methods
) {
    val colors = MaterialTheme.colorScheme                    // theme palette
    val current = userViewModel.userProfile                   // the profile as it currently stands

    // Each field is local editable state, seeded from the current profile. Editing
    // these doesn't touch storage until Save is pressed.
    var name by remember { mutableStateOf(current.runnerName) }
    var email by remember { mutableStateOf(current.email) }
    var location by remember { mutableStateOf(current.location) }
    var fitnessLevel by remember { mutableStateOf(current.fitnessLevel) }
    var personalGoal by remember { mutableStateOf(current.personalGoal) }
    var bio by remember { mutableStateOf(current.bio) }
    var levelMenuOpen by remember { mutableStateOf(false) }   // whether the fitness-level dropdown is open
    var photoUri by remember { mutableStateOf(current.photoUri) } // currently selected avatar URI

    val context = androidx.compose.ui.platform.LocalContext.current // needed for the content resolver
    // System photo picker — returns the chosen image's URI (or null if cancelled).
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Persist read access so AsyncImage can load this URI across process restarts.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* non-persistable provider; still usable this session */ }
            photoUri = uri.toString()                         // show the picked photo immediately
            // Compress to a small JPEG so the avatar syncs cross-device as a blob.
            val avatarBytes = runCatching {
                context.contentResolver.openInputStream(uri).use { input ->
                    val bmp = android.graphics.BitmapFactory.decodeStream(input) // decode the picked image
                        ?: return@runCatching null            // undecodable → no blob (URI still shown locally)
                    compressAvatarImage(bmp)                  // downscale + JPEG-compress under the Firestore cap
                }
            }.getOrNull()
            userViewModel.updatePhotoUri(photoUri, avatarBytes) // persist URI locally + upload the blob
        }
    }
    // Convenience lambda to launch the picker, restricted to images only.
    val launchPicker: () -> Unit = {
        pickPhoto.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    val isValid = name.isNotBlank()                           // Save is only allowed with a non-empty name

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
                navigationIcon = {                            // back button
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                actions = {                                   // top-bar save shortcut (mirrors the bottom button)
                    TextButton(
                        onClick = {
                            userViewModel.updateProfile(      // push all edited fields to the ViewModel
                                name = name,
                                email = email,
                                location = location,
                                fitnessLevel = fitnessLevel,
                                personalGoal = personalGoal,
                                bio = bio
                            )
                            navController.popBackStack()      // return to the profile
                        },
                        enabled = isValid) {}                 // empty label — the visible Save is the bottom button
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
                .verticalScroll(rememberScrollState())        // the form scrolls if it's taller than the screen
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
                    .clickable { launchPicker() }             // tap the avatar to pick a new photo
            ) {
                AsyncImage(
                    model = photoUri ?: R.drawable.hasif_profile, // picked photo, or the default avatar
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop          // crop to fill the circle
                )
            }
            Text(
                "Change photo",                               // text affordance under the avatar
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clickable { launchPicker() }             // also opens the picker
            )

            Spacer(modifier = Modifier.height(24.dp))

            EditField(                                        // reusable labelled text field (below)
                label = "Runner Name",
                value = name,
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                keyboardType = KeyboardType.Email             // email-optimised keyboard
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
            ExposedDropdownMenuBox(                           // a read-only field that opens a menu of options
                expanded = levelMenuOpen,
                onExpandedChange = { levelMenuOpen = !levelMenuOpen } // toggle open/closed on tap
            ) {
                OutlinedTextField(
                    value = fitnessLevel,                     // shows the current selection
                    onValueChange = {},                       // read-only; selection happens via the menu
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelMenuOpen) }, // ▼/▲ chevron
                    modifier = Modifier
                        .menuAnchor()                         // anchors the popup menu to this field
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
                    onDismissRequest = { levelMenuOpen = false } // tap outside closes it
                ) {
                    listOf("Beginner", "Intermediate", "Advanced", "Elite").forEach { option -> // the fixed options
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                fitnessLevel = option         // select it
                                levelMenuOpen = false         // and close the menu
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

            Spacer(modifier = Modifier.height(12.dp))

            EditField(
                label = "Bio",
                value = bio,
                onValueChange = { bio = it },
                singleLine = false                            // bio is multi-line
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(                                           // the primary Save Changes button
                onClick = {
                    userViewModel.updateProfile(              // persist all fields (Room + Firestore + rename fan-out)
                        name = name,
                        email = email,
                        location = location,
                        fitnessLevel = fitnessLevel,
                        personalGoal = personalGoal,
                        bio = bio
                    )
                    navController.popBackStack()              // back to the profile
                },
                enabled = isValid,                            // disabled while the name is blank
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),            // pill button
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f), // faded when disabled
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

// Reusable labelled text field used for every editable profile attribute.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditField(
    label: String,                                           // the field's caption above the input
    value: String,                                           // current text value
    onValueChange: (String) -> Unit,                         // called on each keystroke
    keyboardType: KeyboardType = KeyboardType.Text,          // soft-keyboard variant (default plain text)
    singleLine: Boolean = true                               // false → multi-line (used for Bio)
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
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,             // give multi-line fields more initial height
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

// Design-time preview for Android Studio.
@Preview
@Composable
fun EditProfileScreenPreview() {
    EditProfileScreen(rememberNavController())
}
