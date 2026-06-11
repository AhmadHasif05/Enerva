package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.Manifest
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.example.a211198_hasif_drnelson_Project2.view_model.RecordViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.RecordViewModelFactory
import com.example.a211198_hasif_drnelson_Project2.view_model.formatElapsed
import com.example.a211198_hasif_drnelson_Project2.view_model.formatPace
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.ramani.compose.CameraPosition
import org.ramani.compose.CenterState
import org.ramani.compose.Circle
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.Polyline
import org.ramani.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    navController: NavController? = null,
    recordViewModel: RecordViewModel = viewModel(factory = RecordViewModelFactory)
) {
    val colors = MaterialTheme.colorScheme

    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    // --- Map view-state (no ViewModel involvement) ---
    // The map's built-in location layer reports the latest fix here; we forward it to
    // the ViewModel (which computes distance/pace and grows the breadcrumb path).
    val userLocation = remember { mutableStateOf(Location(null)) }
    val cameraMode = remember { mutableIntStateOf(CameraMode.NONE) }
    val cameraPositionState = rememberCameraPositionState(CameraPosition(zoom = 16.0))
    val startCenter = rememberSaveable(saver = CenterState.Saver) { CenterState(LatLng(0.0, 0.0)) }
    var tilted by remember { mutableStateOf(false) }

    // Available map styles for the Layers button. OpenFreeMap needs no key (so the map
    // always renders); a MapTiler key (local.properties) unlocks the extra styles.
    val styleUrls = remember {
        buildList {
            add("https://tiles.openfreemap.org/styles/liberty")
            add("https://tiles.openfreemap.org/styles/bright")
            val key = BuildConfig.MAPTILER_API_KEY
            if (key.isNotBlank()) {
                add("https://api.maptiler.com/maps/streets-v2/style.json?key=$key")
                add("https://api.maptiler.com/maps/satellite/style.json?key=$key")
                add("https://api.maptiler.com/maps/outdoor-v2/style.json?key=$key")
            }
        }
    }
    var styleIndex by remember { mutableIntStateOf(0) }

    // Polyline colour as a MapLibre hex string (#RRGGBB) from the theme primary.
    val trailColorHex = remember(colors.primary) {
        String.format("#%06X", 0xFFFFFF and colors.primary.toArgb())
    }

    // Forward each valid fix to the ViewModel while recording.
    LaunchedEffect(userLocation.value) {
        val loc = userLocation.value
        if (recordViewModel.isRecording && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
            val accuracy: Float? = if (loc.hasAccuracy()) loc.accuracy else null
            recordViewModel.onLocation(loc.latitude, loc.longitude, accuracy, loc.time)
        }
    }

    // Pin the start marker to the first recorded point.
    LaunchedEffect(recordViewModel.path.firstOrNull()) {
        recordViewModel.path.firstOrNull()?.let { startCenter.center = LatLng(it.lat, it.lng) }
    }

    // Follow the user while recording; release the camera when paused/stopped.
    LaunchedEffect(recordViewModel.isRecording) {
        cameraMode.intValue = if (recordViewModel.isRecording) CameraMode.TRACKING else CameraMode.NONE
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // --- Real vector map (replaces the Canvas breadcrumb backdrop) ---
        MapLibre(
            modifier = Modifier.fillMaxSize(),
            style = MapStyle.Uri(styleUrls[styleIndex]),
            cameraPositionState = cameraPositionState,
            locationRequestProperties = LocationRequestProperties(),
            locationStyling = LocationStyling(enablePulse = true),
            userLocation = userLocation,
            renderMode = RenderMode.COMPASS,
            cameraMode = cameraMode,
        ) {
            val trail = recordViewModel.path.map { LatLng(it.lat, it.lng) }
            if (trail.size >= 2) {
                Polyline(points = trail, color = trailColorHex, lineWidth = 8f)
            }
            if (trail.isNotEmpty()) {
                Circle(
                    centerState = startCenter,
                    radius = 8f,
                    color = "#4CAF50",
                    borderColor = "White",
                    borderWidth = 2f,
                )
            }
        }

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

        // --- Top-left back / Activities button (search + filter bar removed in Phase 4) ---
        IconButton(
            onClick = { navController?.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .background(colors.surface, CircleShape)
                .size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Activities", tint = colors.primary)
        }

        // --- Right-side map action buttons (now functional) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Layers: cycle through the available map styles.
            RecordMapActionButton(Icons.Outlined.Layers) {
                styleIndex = (styleIndex + 1) % styleUrls.size
            }
            // 3D: toggle camera tilt.
            RecordMapActionButton(Icons.Default.ViewInAr) {
                tilted = !tilted
                cameraPositionState.position =
                    cameraPositionState.position.copy(tilt = if (tilted) 45.0 else 0.0)
            }
            // Recenter on the user's current position.
            RecordMapActionButton(Icons.Default.MyLocation) {
                val loc = userLocation.value
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    cameraPositionState.position = cameraPositionState.position.copy(
                        target = LatLng(loc.latitude, loc.longitude),
                        zoom = 16.0
                    )
                }
            }
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
                    RecordStatItem(
                        formatPace(recordViewModel.elapsedSeconds, recordViewModel.distanceKm),
                        "Pace (/km)"
                    )
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

@Composable
fun RecordStatItem(value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecordMapActionButton(icon: ImageVector, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        modifier = Modifier.size(44.dp),
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview
@Composable
fun RecordScreenPreview() {
    RecordScreen(rememberNavController())
}
