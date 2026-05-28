package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project2.model.GalleryActivity
import com.example.a211198_hasif_drnelson_Project2.model.sampleGalleryActivities
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel

// Gallery — Instagram/TikTok-style reels feed. One full-screen reel per page,
// vertical paging between them. Each reel shows the author, caption, a like
// button, and a Follow button so you can follow that user without leaving
// the feed.
@Composable
fun GalleryScreen(
    navController: NavController? = null,
    userViewModel: UserViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel()
) {
    val myName = userViewModel.userProfile.runnerName.ifBlank { "You" }
    val reels = sampleGalleryActivities(myName)
    val pagerState = rememberPagerState(pageCount = { reels.size })

    // Per-reel like state — survives page swipes within this screen.
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val likeBumps = remember { mutableStateMapOf<String, Int>() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            // Read follow state here so Compose tracks the read and this page
            // recomposes when toggleFollow mutates the underlying state map.
            val following = userViewModel.isFollowing(reel.author)
            ReelPage(
                reel = reel,
                isMine = reel.author == myName,
                isLiked = liked[reel.id] == true,
                likeBump = likeBumps[reel.id] ?: 0,
                isFollowing = following,
                onLike = {
                    val now = liked[reel.id] != true
                    liked[reel.id] = now
                    if (now) likeBumps[reel.id] = (likeBumps[reel.id] ?: 0) + 1
                    else likeBumps[reel.id] = (likeBumps[reel.id] ?: 0) - 1
                },
                onFollow = {
                    val wasFollowing = userViewModel.isFollowing(reel.author)
                    userViewModel.toggleFollow(reel.author)
                    // Mirror SearchScreen: following someone seeds a chat with
                    // them so they show up in Messages; unfollowing drops it.
                    if (!wasFollowing) {
                        messageViewModel.startConversationWith(reel.author)
                    } else {
                        messageViewModel.removeConversation(reel.author)
                    }
                }
            )
        }
    }
}

@Composable
private fun ReelPage(
    reel: GalleryActivity,
    isMine: Boolean,
    isLiked: Boolean,
    likeBump: Int,
    isFollowing: Boolean,
    onLike: () -> Unit,
    onFollow: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image — fills the page.
        AsyncImage(
            model = reel.imageRes,
            contentDescription = reel.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark gradient at the bottom so the overlay text stays legible
        // even against a bright image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )

        // Right-side action rail: like + comment + share. Instagram/TikTok pattern.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ReelActionButton(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                tint = if (isLiked) Color(0xFFFF3B5C) else Color.White,
                label = (reel.likes + likeBump).toString(),
                onClick = onLike
            )
            ReelActionButton(
                icon = Icons.Rounded.ChatBubbleOutline,
                tint = Color.White,
                label = "Comment",
                onClick = { /* TODO: comments */ }
            )
            ReelActionButton(
                icon = Icons.Rounded.Send,
                tint = Color.White,
                label = "Share",
                onClick = { /* TODO: share */ }
            )
        }

        // Bottom-left: author + caption + Follow button.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 96.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    reel.author,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Don't show "Follow" on your own reel.
                if (!isMine) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollow,
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Following", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = onFollow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                reel.caption,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${reel.activity} · ${reel.distanceKm} km",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(30.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun GalleryScreenPreview() {
    GalleryScreen(rememberNavController())
}
