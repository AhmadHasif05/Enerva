package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Bottom-sheet summary shown when the user taps End. Shows the route picture (or
// a spinner while it renders), the run stats, a caption field, an "include
// photo" toggle, and Post / Discard actions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummarySheet(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    onPost: (caption: String, includePhoto: Boolean) -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Run complete", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.onSurface)
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    snapshotLoading -> CircularProgressIndicator()
                    snapshot != null -> Image(
                        bitmap = snapshot.asImageBitmap(),
                        contentDescription = "Route map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Text("No route image", color = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryStat(timeText, "Time")
                SummaryStat(distanceText, "Distance (km)")
                SummaryStat(paceText, "Pace (/km)")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                    onClick = { onPost(caption, includePhoto && snapshot != null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Post")
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp)
    }
}
