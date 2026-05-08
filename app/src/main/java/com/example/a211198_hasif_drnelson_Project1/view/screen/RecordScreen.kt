package com.example.a211198_hasif_drnelson_Project1.view.screen

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project1.view_model.RecordViewModel
import com.example.a211198_hasif_drnelson_Project1.view_model.TrackPoint
import com.example.a211198_hasif_drnelson_Project1.view_model.formatElapsed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordScreen(
    navController: NavController? = null,
    recordViewModel: RecordViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    // Wire FusedLocationProviderClient to the ViewModel while recording.
    LocationUpdatesEffect(
        enabled = recordViewModel.isRecording && permission.status.isGranted,
        onLocation = { lat, lng, speed, time ->
            recordViewModel.onLocation(lat, lng, speed, time)
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // --- Canvas-drawn breadcrumb trail (replaces the static map placeholder) ---
        TrailCanvas(
            points = recordViewModel.path,
            modifier = Modifier.fillMaxSize()
        )

        if (recordViewModel.path.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = colors.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (!permission.status.isGranted) "Location permission required"
                    else if (!recordViewModel.isRecording) "Press play to start tracking"
                    else "Waiting for GPS fix...",
                    color = colors.onSurfaceVariant
                )
            }
        }

        // --- Top bar buttons ---
        IconButton(
            onClick = { navController?.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .background(colors.surface, CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = colors.onSurface)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecordMapActionButton(Icons.Outlined.Layers)
            RecordMapActionButton(Icons.Default.ViewInAr)
            RecordMapActionButton(Icons.Default.MyLocation)
        }

        IconButton(
            onClick = { recordViewModel.reset() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 280.dp)
                .background(colors.surface, CircleShape)
                .size(36.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }

        // --- Live stats card ---
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 170.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Text("Walk", color = colors.onSurface, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.OpenInFull, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RecordStatItem(formatElapsed(recordViewModel.elapsedSeconds), "Time")
                    RecordStatItem("%.2f".format(recordViewModel.distanceKm), "Distance (km)")
                    RecordStatItem("%.1f".format(recordViewModel.currentSpeedKmh), "Speed (km/h)")
                }
            }
        }

        // --- Bottom control bar ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp),
            color = colors.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.outline)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = colors.primary)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                        }
                        Text("Walk", color = colors.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }

                    // Start / Pause toggle
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.size(80.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (!permission.status.isGranted) {
                                    permission.launchPermissionRequest()
                                } else if (recordViewModel.isRecording) {
                                    recordViewModel.pause()
                                } else {
                                    recordViewModel.start()
                                }
                            }
                        ) {
                            Icon(
                                if (recordViewModel.isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (recordViewModel.isRecording) "Pause" else "Start",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(onClick = { recordViewModel.reset() }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = colors.onSurface)
                            }
                        }
                        Text("Stop", color = colors.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun LocationUpdatesEffect(
    enabled: Boolean,
    onLocation: (lat: Double, lng: Double, speed: Float?, time: Long) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val speed: Float? = if (location.hasSpeed()) location.speed else null
                    onLocation(location.latitude, location.longitude, speed, location.time)
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        onDispose {
            client.removeLocationUpdates(callback)
        }
    }
}

@Composable
private fun TrailCanvas(points: List<TrackPoint>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        // Faint grid lines as a stand-in for the map background.
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

        if (points.size < 2) return@Canvas

        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }

        val latRange = (maxLat - minLat).coerceAtLeast(1e-6)
        val lngRange = (maxLng - minLng).coerceAtLeast(1e-6)
        val padding = 64f

        fun project(p: TrackPoint): Offset {
            val nx = ((p.lng - minLng) / lngRange).toFloat()
            val ny = 1f - ((p.lat - minLat) / latRange).toFloat()
            return Offset(
                x = padding + nx * (size.width - 2 * padding),
                y = padding + ny * (size.height - 2 * padding)
            )
        }

        val path = Path().apply {
            val first = project(points.first())
            moveTo(first.x, first.y)
            for (i in 1 until points.size) {
                val o = project(points[i])
                lineTo(o.x, o.y)
            }
        }
        drawPath(path = path, color = primary, style = Stroke(width = 8f))

        // Start marker
        drawCircle(color = Color(0xFF4CAF50), radius = 12f, center = project(points.first()))
        // Current marker
        drawCircle(color = primary, radius = 14f, center = project(points.last()))
    }
}

@Composable
fun RecordStatItem(value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecordMapActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

@Preview
@Composable
fun RecordScreenPreview() {
    RecordScreen(rememberNavController())
}
