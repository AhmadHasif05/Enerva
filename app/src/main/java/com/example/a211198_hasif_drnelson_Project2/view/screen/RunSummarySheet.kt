package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val captureLayer: GraphicsLayer = rememberGraphicsLayer()
    var caption by remember { mutableStateOf("New run") }
    var includePhoto by remember { mutableStateOf(true) }

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
                snapshot = snapshot,
                snapshotLoading = snapshotLoading,
                timeText = timeText,
                distanceText = distanceText,
                paceText = paceText,
                modifier = Modifier.fillMaxWidth(),
                captureLayer = captureLayer,
            )

            Spacer(Modifier.height(16.dp))

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
                Text("Include route photo", color = colors.onSurface)
                Switch(
                    checked = includePhoto,
                    onCheckedChange = { includePhoto = it },
                    enabled = snapshot != null
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
                        val wantPhoto = includePhoto && snapshot != null
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
