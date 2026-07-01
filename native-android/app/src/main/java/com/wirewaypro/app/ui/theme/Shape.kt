package com.wirewaypro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Soft, pill-and-panel shapes matching the mockups: generous corner radii on cards
// and buttons for a friendly, modern feel.
val WirewayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),  // default card radius
    large = RoundedCornerShape(26.dp),   // hero panels
    extraLarge = RoundedCornerShape(34.dp),
)
