package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The branded run-summary card: ENERVA header + pace-coloured route map + stats
// footer. Used both on screen (in RunSummarySheet) and, via `captureLayer`,
// rendered to the bitmap posted to the gallery. Pure presentation — no logic.
@Composable
fun RunSummaryCard(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
) {
    val colors = MaterialTheme.colorScheme

    // When a capture layer is supplied, record this card's drawing into it so the
    // sheet can export the exact on-screen card as a bitmap.
    val captureModifier = if (captureLayer != null) {
        Modifier.drawWithContent {
            captureLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureLayer)
        }
    } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .then(captureModifier)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "ENERVA",
                color = colors.onPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = 3.sp
            )
        }

        // Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
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
            if (snapshot != null) {
                PaceLegend(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }

        // Stats footer
        Row(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            SummaryStatTile(Icons.Filled.Timer, "DURATION", timeText, Modifier.weight(1f))
            FooterDivider()
            SummaryStatTile(Icons.Filled.Place, "DISTANCE", distanceText, Modifier.weight(1f))
            FooterDivider()
            SummaryStatTile(Icons.Filled.Speed, "PACE", paceText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = colors.onSurfaceVariant, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun PaceLegend(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.background.copy(alpha = 0.7f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("FAST", color = colors.onSurfaceVariant, fontSize = 8.sp)
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    // Mirrors the pace ramp stops in PaceColors.kt (fast→slow).
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFC4C02))
                    )
                )
        )
        Text("SLOW", color = colors.onSurfaceVariant, fontSize = 8.sp)
    }
}
