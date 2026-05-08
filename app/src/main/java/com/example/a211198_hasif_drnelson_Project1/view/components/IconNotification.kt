// Small reusable square badge that says "ENERVA" — the app's mark used as the
// avatar on each row in NotifyScreen. iconColor is passed in so the parent
// can theme each notification individually.
package com.example.a211198_hasif_drnelson_Project1.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IconNotification(
    iconColor: Color = MaterialTheme.colorScheme.primary, // Background colour of the badge
    modifier: Modifier = Modifier,
    text: String = "ENERVA"                                // Text rendered inside the badge
) {
    Box(
        modifier = modifier
            .size(48.dp)                                                       // Fixed-size square
            .background(iconColor, RoundedCornerShape(8.dp)),                  // Coloured rounded background
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,                       // Auto-paired contrast colour
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}