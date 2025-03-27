package com.example.money.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    // Small components like chips, small buttons
    small = RoundedCornerShape(4.dp),
    // Medium components like cards, dialogs
    medium = RoundedCornerShape(12.dp),
    // Large components like bottom sheets
    large = RoundedCornerShape(16.dp),
    // Custom shapes for specific components
    extraSmall = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(24.dp)
) 