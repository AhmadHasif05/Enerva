package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project1.R
import com.example.a211198_hasif_drnelson_Project1.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen() {
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {

        // --- 1. Canvas trail backdrop (replaces static placeholder) ---
        MapTrailBackdrop(modifier = Modifier.fillMaxSize())

        // --- 2. Top UI Layer ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = colors.primary)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("Search locations", color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp), color = colors.outline)
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = colors.onSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saved", color = colors.onSurface, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filters = listOf("Routes", "Length", "Difficulty", "Elevation")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = filter == "Routes",
                        onClick = { },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(filter)
                                if (filter == "Routes") {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.surface,
                            labelColor = colors.onSurface,
                            selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.outline,
                            selectedBorderColor = colors.primary,
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = filter == "Routes"
                        )
                    )
                }
            }
        }

        // --- 3. Floating Action Buttons (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapActionButton(Icons.Outlined.Layers)
            MapActionButton(Icons.Default.ViewInAr)
            MapActionButton(Icons.Default.MyLocation)
            MapActionButton(Icons.Default.Edit)
        }

        // --- 4. Bottom Route Card ---
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(colors.surfaceVariant)
                ) {
                    AsyncImage(
                        model = R.drawable.lingkunganilmu,
                        contentDescription = "Lingkungan Ilmu route",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .weight(1f)
                ) {
                    Text(
                        "Lingkungan Ilmu-Persiaran U...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SuccessGreen,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Easy",
                                color = colors.onPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            " • 6.5 km • 84.2 m • 0h 48m",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("From your location", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        "Made for you",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        IconButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 170.dp)
                .background(colors.surface, CircleShape)
                .size(36.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

// Renders a faint grid plus a couple of pre-baked sample routes so the map area looks alive.
@Composable
private fun MapTrailBackdrop(modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        // Grid
        val gridStep = 64f
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = outline.copy(alpha = 0.15f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = outline.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        // Sample route A — Teratai (orange, primary)
        val routeA = Path().apply {
            val cx = size.width * 0.30f
            val cy = size.height * 0.55f
            moveTo(cx - 120f, cy + 80f)
            cubicTo(cx - 60f, cy - 40f, cx + 40f, cy - 60f, cx + 100f, cy + 20f)
            cubicTo(cx + 160f, cy + 100f, cx + 60f, cy + 140f, cx - 40f, cy + 100f)
            close()
        }
        drawPath(routeA, color = primary, style = Stroke(width = 7f))
        drawCircle(SuccessGreen, radius = 11f, center = Offset(size.width * 0.30f - 120f, size.height * 0.55f + 80f))

        // Sample route B — Lakeside Trail (cyan-ish blue)
        val routeB = Path().apply {
            val sx = size.width * 0.55f
            val sy = size.height * 0.30f
            moveTo(sx, sy)
            cubicTo(sx + 90f, sy + 40f, sx + 40f, sy + 120f, sx + 130f, sy + 180f)
            cubicTo(sx + 200f, sy + 230f, sx + 240f, sy + 320f, sx + 160f, sy + 380f)
        }
        drawPath(routeB, color = Color(0xFF4FC3F7), style = Stroke(width = 6f))
        drawCircle(SuccessGreen, radius = 10f, center = Offset(size.width * 0.55f, size.height * 0.30f))

        // Sample route C — Lingkungan Ilmu (yellow accent)
        val routeC = Path().apply {
            val ax = size.width * 0.20f
            val ay = size.height * 0.20f
            moveTo(ax, ay)
            lineTo(ax + 80f, ay + 30f)
            lineTo(ax + 60f, ay + 110f)
            lineTo(ax + 160f, ay + 140f)
            lineTo(ax + 200f, ay + 80f)
        }
        drawPath(routeC, color = Color(0xFFFFD54F), style = Stroke(width = 5f))
        drawCircle(SuccessGreen, radius = 9f, center = Offset(size.width * 0.20f, size.height * 0.20f))
    }
}

@Composable
fun MapActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        modifier = Modifier.size(44.dp),
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = { }) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}