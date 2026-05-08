package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project1.model.Notification
import com.example.a211198_hasif_drnelson_Project1.view.components.IconNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyScreen(navController: NavController) {
    val colors = MaterialTheme.colorScheme

    val notifications = listOf(
        Notification(
            title = "Visit popular spots for your next activity",
            description = "Find popular segments in your area to plan your next activity.",
            timestamp = "14 days ago"
        ),
        Notification(
            title = "Welcome to Enerva Challenges",
            description = "Join a challenge to get motivated and get after it.",
            timestamp = "16 days ago"
        ),
        Notification(
            title = "Find a local club",
            description = "Check out local clubs to join and meet workout partners at club events.",
            timestamp = "18 days ago"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(notifications) { notification ->
                NotificationItem(notification, colors.primary)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NotificationItem(data: Notification, iconColor: Color) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        IconNotification(iconColor = iconColor)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    lineHeight = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.timestamp,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.onSurfaceVariant
                )
            )
        }
    }
}

@Preview
@Composable
fun NotifyScreenPreview() {
    NotifyScreen(rememberNavController())
}