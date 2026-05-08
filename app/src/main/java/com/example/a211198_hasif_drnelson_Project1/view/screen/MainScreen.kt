// MainScreen — the welcome / onboarding screen the user sees first.
// Shows the ENERVA logo, an inset card with sport icons, a tagline, pagination
// dots, and the Join / Log In buttons.
package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.rounded.DownhillSkiing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a211198_hasif_drnelson_Project1.R

@Composable
fun MainScreen(
    onNavigateToSignup: () -> Unit = {}, // Triggered by the "Join for free" button
    onNavigateToLogin: () -> Unit = {}   // Triggered by the "Log In" text button
) {
    // Cache theme tokens once so the rest of the function stays readable.
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // The whole layout is a Box — it lets us stack a background image, a dark
    // overlay, and the foreground UI on top of one another.
    Box(modifier = Modifier.fillMaxSize()) {
        // Hero background image (a runner). Crops to fill the screen.
        Image(
            painter = painterResource(id = R.drawable.bgmainscreen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay so the white text stays readable on top of the image.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        // All the actual content — title, card, buttons.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ENERVA logo / brand mark
            Text(
                text = "ENERVA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = primary,
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Central card showcasing sport selection — purely visual on this
            // welcome screen. The real onboarding flow happens on Signup.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Card top bar — fake "Close / Run / Settings" header for visual demo.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Close", color = onSurfaceVariant, fontSize = 14.sp)
                        Text("Run", fontWeight = FontWeight.Bold, color = onSurface)
                        Icon(Icons.Default.Settings, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }

                    // Hero photo of a running scene.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFC8E6C9)) // Fallback colour while the image loads
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.running),
                            contentDescription = "Running preview",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight()
                                .padding(10.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Choose a Sport",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row of selectable sport icons — Run is highlighted.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SportIconOnboarding("Ride", Icons.AutoMirrored.Rounded.DirectionsBike, false)
                        SportIconOnboarding("Run", Icons.AutoMirrored.Filled.DirectionsRun, true)
                        SportIconOnboarding("Hike", Icons.Default.Hiking, false)
                        SportIconOnboarding("Ski", Icons.Rounded.DownhillSkiing, false)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Water Sports",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontSize = 14.sp
                    )

                    // Inline row showing two extra sports the user could pick.
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Pool, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Swim", color = onSurface, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(24.dp))
                        Icon(Icons.Default.Surfing, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Surf", color = onSurface, fontSize = 14.sp)
                    }
                }
            }

            // Push the bottom content down with a flexible spacer.
            Spacer(modifier = Modifier.weight(1f))

            // Tagline
            Text(
                text = "Track your active life in one place.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pagination dots — purely decorative; mimics an onboarding carousel.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> primary // First page indicator is highlighted
                                    1 -> onSurfaceVariant.copy(alpha = 0.3f)
                                    2 -> onSurfaceVariant.copy(alpha = 0.5f)
                                    else -> onSurfaceVariant.copy(alpha = 0.2f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Primary CTA — go to signup.
            Button(
                onClick = onNavigateToSignup,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = background),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Join for free", color = onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Secondary CTA — go to login (existing user).
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text("Log In", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// One sport selector circle inside the welcome card.
//   - When `isSelected` is true we paint the orange-tinted background and
//     give the label / icon a primary tint.
//   - `iconPainter` is `Any` so callers can pass either ImageVector or Painter.
@Composable
fun SportIconOnboarding(
    label: String,
    iconPainter: Any,
    isSelected: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = if (isSelected) primary.copy(alpha = 0.1f) else Color.Transparent,
            border = if (!isSelected) BorderStroke(1.dp, onSurfaceVariant) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Branch on the runtime type so we can render either a vector icon
                // or a bitmap painter inside the same circle.
                when (iconPainter) {
                    is androidx.compose.ui.graphics.vector.ImageVector -> {
                        Icon(
                            imageVector = iconPainter,
                            contentDescription = null,
                            tint = if (isSelected) primary else onSurfaceVariant
                        )
                    }
                    is androidx.compose.ui.graphics.painter.Painter -> {
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            colorFilter = if (isSelected)
                                androidx.compose.ui.graphics.ColorFilter.tint(primary)
                            else
                                androidx.compose.ui.graphics.ColorFilter.tint(onSurfaceVariant)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = if (isSelected) primary else onSurfaceVariant, fontSize = 13.sp)
    }
}

// IDE preview entry point.
@Preview
@Composable
fun MainScreenPreview() {
    MainScreen()
}