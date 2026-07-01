package com.wirewaypro.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Wireway brand palette — "Powered by Precision".
//
// One identity, two skins. The accent hues (electric blue → purple) are shared
// across LIGHT and DARK; only the surfaces flip, so the app reads as a single
// brand in two moods. There is no gold anywhere — the accent is always blue/
// purple. A subtle blue→purple gradient ([BrandGradient]) carries hero/CTA
// surfaces.
// ─────────────────────────────────────────────────────────────────────────────

// ── Core brand hues (shared by both skins) ──────────────────────────────────
/** Electric blue — the primary brand accent. */
val BrandBlue = Color(0xFF3D63FF)
/** Vivid violet — the secondary brand accent. */
val BrandPurple = Color(0xFF8A5BFF)

/** Gradient endpoints for hero surfaces / primary CTAs (blue → purple). */
val GradientBlue = Color(0xFF2E6BFF)
val GradientPurple = Color(0xFF8A4FFF)

/** Shared semantic accents. */
val BrandGreen = Color(0xFF22C55E) // success / positive money (used app-wide)
val BrandAmber = Color(0xFFF5A524) // warning / pending

// ── LIGHT skin ──────────────────────────────────────────────────────────────
// Blue/purple accents on white & very-light blue-tinted surfaces (blue mockup).
val LightPrimary = Color(0xFF3A57E8)        // electric blue, tuned for white bg
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE3E8FF)
val LightOnPrimaryContainer = Color(0xFF0E1A52)
val LightSecondary = Color(0xFF7B4DF0)       // purple
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFEDE4FF)
val LightOnSecondaryContainer = Color(0xFF2A0F66)
val LightBackground = Color(0xFFF6F8FE)      // very light blue-gray ground
val LightOnBackground = Color(0xFF0F1426)
val LightSurface = Color(0xFFFFFFFF)         // cards
val LightOnSurface = Color(0xFF131A2E)
val LightSurfaceVariant = Color(0xFFEDF1FB)  // raised / tinted cards
val LightOnSurfaceVariant = Color(0xFF5A6585)
val LightOutline = Color(0xFFD3DAEC)
val LightOutlineVariant = Color(0x14101A52)  // hairline (blue, ~8% alpha)

// ── DARK skin ─────────────────────────────────────────────────────────────────
// Same accents, brightened a touch for contrast, on near-black blue-tinted
// surfaces (dark mockup — but blue, never gold).
val DarkPrimary = Color(0xFF6B8AFF)          // electric blue, brightened for dark
val DarkOnPrimary = Color(0xFF071034)
val DarkPrimaryContainer = Color(0xFF22305F)
val DarkOnPrimaryContainer = Color(0xFFDBE3FF)
val DarkSecondary = Color(0xFFAE8BFF)         // purple
val DarkOnSecondary = Color(0xFF1E0C4D)
val DarkSecondaryContainer = Color(0xFF362159)
val DarkOnSecondaryContainer = Color(0xFFE9DEFF)
val DarkBackground = Color(0xFF0A0B14)        // near-black, slight blue/purple cast
val DarkOnBackground = Color(0xFFECEFF9)
val DarkSurface = Color(0xFF111425)           // cards
val DarkOnSurface = Color(0xFFECEFF9)
val DarkSurfaceVariant = Color(0xFF1A1E33)    // raised cards
val DarkOnSurfaceVariant = Color(0xFF9AA3C4)
val DarkOutline = Color(0xFF2C3354)
val DarkOutlineVariant = Color(0x22708CFF)    // hairline (blue glow, ~13% alpha)

// ── Shared error ─────────────────────────────────────────────────────────────
val ErrorRed = Color(0xFFE5484D)
