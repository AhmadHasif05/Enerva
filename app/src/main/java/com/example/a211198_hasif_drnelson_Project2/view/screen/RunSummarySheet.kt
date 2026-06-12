package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.launch

// Bottom-sheet summary shown when the user taps End. Shows the branded
// RunSummaryCard (the same composition that gets posted), a caption field, an
// "include photo" toggle, and Post / Discard. On Post, the card is captured to a
// Bitmap and handed back so the gallery reel shows the branded card.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummarySheet(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    onPost: (caption: String, cardBitmap: Bitmap?) -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val captureLayer: GraphicsLayer = rememberGraphicsLayer()
    var caption by remember { mutableStateOf("New run") }
    var includePhoto by remember { mutableStateOf(true) }

    // A user-taken camera photo. When present it overrides the route map as the
    // card image (stats overlay stays). pendingPhotoFile is the file the camera is
    // currently writing into, read back once TakePicture reports success.
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingPhotoFile?.let { photo = decodeSampledBitmap(it) }
        }
        pendingPhotoFile = null
    }

    fun launchCamera() {
        val (file, uri: Uri) = createCameraImageUri(context)
        pendingPhotoFile = file
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPhotoFile = null
            Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    // What the card shows: a captured photo wins over the route snapshot.
    val cardImage = photo ?: snapshot
    val hasImage = cardImage != null

    ModalBottomSheet(
        onDismissRequest = onDiscard,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RunSummaryCard(
                snapshot = cardImage,
                snapshotLoading = snapshotLoading && photo == null,
                timeText = timeText,
                distanceText = distanceText,
                paceText = paceText,
                modifier = Modifier.fillMaxWidth(),
                captureLayer = captureLayer,
                isPhoto = photo != null,
            )

            Spacer(Modifier.height(12.dp))

            // Take / retake a real photo, and revert to the route map if wanted.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (photo == null) "Take photo" else "Retake")
                }
                if (photo != null && snapshot != null) {
                    TextButton(onClick = { photo = null }, modifier = Modifier.weight(1f)) {
                        Text("Use route map")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Include photo", color = colors.onSurface)
                Switch(
                    checked = includePhoto,
                    onCheckedChange = { includePhoto = it },
                    enabled = hasImage
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        val wantPhoto = includePhoto && hasImage
                        if (wantPhoto) {
                            scope.launch {
                                val bmp = captureLayer.toImageBitmap().asAndroidBitmap()
                                onPost(caption, bmp)
                            }
                        } else {
                            onPost(caption, null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Post")
                }
            }
        }
    }
}
