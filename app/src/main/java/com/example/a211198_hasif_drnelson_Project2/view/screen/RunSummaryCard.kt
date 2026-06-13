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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The branded run-summary card: a 4:5 hero image (the user's photo if taken, else
// the pace-coloured route map) with overlays — ENERVA wordmark top-left, a stats
// info chip bottom-right, and (only when the route map is the hero) the pace legend
// bottom-left. Used on screen in RunSummarySheet and, via `captureLayer`, exported
// to the 4:5 bitmap posted to the gallery. Pure presentation — no logic.
@Composable
fun RunSummaryCard(
    photo: Bitmap?,
    routeSnapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
) {
    val colors = MaterialTheme.colorScheme

    val heroIsPhoto = photo != null
    val hero = photo ?: routeSnapshot
    // Show the route thumbnail in the chip only when a photo took the hero slot, so
    // the route stays visible. When the route map is already the hero, the thumbnail
    // would be redundant.
    val showRouteThumb = photo != null && routeSnapshot != null
    // The pace legend is meaningful only over the route map, never over a photo.
    val showPaceLegend = !heroIsPhoto && routeSnapshot != null

    // When a capture layer is supplied, record this card's drawing into it so the
    // sheet can export the exact on-screen card as a bitmap.
    val captureModifier = if (captureLayer != null) {
        Modifier.drawWithContent {
            captureLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureLayer)
        }
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .then(captureModifier)
    ) {
        // Hero image (or loading / empty state).
        when {
            snapshotLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            hero != null -> Image(
                bitmap = hero.asImageBitmap(),
                contentDescription = if (heroIsPhoto) "Run photo" else "Route map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No route image", color = colors.onSurfaceVariant)
            }
        }

        if (hero != null && !snapshotLoading) {
            // Top + bottom scrim so the white wordmark and the chip stay legible over
            // any image.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.35f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f)
                        )
                    )
            )
        }

        // ENERVA wordmark, top-left.
        Text(
            "ENERVA",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 12.dp)
        )

        // Pace legend, bottom-left — only when the route map is the hero.
        if (showPaceLegend) {
            PaceLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        }

        // Info chip, bottom-right: ENERVA + (route thumbnail when hero is a photo) +
        // the three stats.
        InfoChip(
            timeText = timeText,
            distanceText = distanceText,
            paceText = paceText,
            routeThumb = if (showRouteThumb) routeSnapshot else null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun InfoChip(
    timeText: String,
    distanceText: String,
    paceText: String,
    routeThumb: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (routeThumb != null) {
                Image(
                    bitmap = routeThumb.asImageBitmap(),
                    contentDescription = "Route map",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "ENERVA",
                color = Color(0xFFFFB38A),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ChipStat(Icons.Filled.Timer, "DURATION", timeText, Modifier.weight(1f))
            ChipStat(Icons.Filled.Place, "DISTANCE", distanceText, Modifier.weight(1f))
            ChipStat(Icons.Filled.Speed, "PACE", paceText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChipStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 7.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
                        listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF7043), Color(0xFFFC4C02))
                    )
                )
        )
        Text("SLOW", color = colors.onSurfaceVariant, fontSize = 8.sp)
    }
}
