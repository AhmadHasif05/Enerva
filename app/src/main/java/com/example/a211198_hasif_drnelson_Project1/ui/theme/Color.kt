// All design-system colour tokens live in this file. Each Composable should
// pull colours from MaterialTheme.colorScheme.* — never hardcode `Color(0x..)`
// in screen code. That keeps the app consistent and themable.
package com.example.a211198_hasif_drnelson_Project1.ui.theme

import androidx.compose.ui.graphics.Color

// ----- Brand -----
// The signature Strava-style orange used for primary actions and highlights.
val StravaOrange = Color(0xFFFC4C02)
val StravaOrangeMuted = Color(0xFFFF7043) // Lighter orange for hover / accent variants

// ----- Light theme tokens -----
// `Primary` and `Secondary` map straight into MaterialTheme.colorScheme.* in Theme.kt.
val Primary = StravaOrange
val Secondary = Color(0xFF625b71)
val Tertiary = Color(0xFF7D5260)

// ----- Dark theme tokens -----
val DarkPrimary = StravaOrange
val DarkSecondary = Color(0xFFCCC2DC)
val DarkTertiary = Color(0xFFEFB8C8)

// ----- Dark surfaces -----
// Stacked from darkest (background) to lightest (surfaceVariant), used for
// the Strava-like layered dark UI.
val Background = Color(0xFF121212)
val Surface = Color(0xFF1E1E1E)
val SurfaceVariant = Color(0xFF2A2A2A)

// ----- Foreground (dark theme) -----
// `On*` colours are what sits ON TOP OF the matching surface — text/icon tints.
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnTertiary = Color.White
val OnBackground = Color.White
val OnSurface = Color.White
val OnSurfaceVariant = Color(0xFF9E9E9E) // Dim gray for secondary text
val OutlineDark = Color(0xFF424242)      // Borders
val OutlineVariantDark = Color(0xFF2E2E2E) // Faint dividers

// ----- Semantic (not theme-bound) -----
// Used for UI elements whose colour conveys meaning (success / difficulty)
// rather than brand. These don't change with the theme.
val SuccessGreen = Color(0xFF4CAF50)
val SuccessGreenLight = Color(0xFF81C784)