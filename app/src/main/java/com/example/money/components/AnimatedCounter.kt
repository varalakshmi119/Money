package com.example.money.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * A composable that animates a numeric value with a rolling animation effect.
 * 
 * @param targetValue The target value to animate to
 * @param formatValue A function to format the animated value as a string
 * @param style The text style to apply to the displayed value
 * @param fontWeight The font weight to apply to the displayed value
 * @param color The color to apply to the displayed value
 * @param durationMillis The duration of the animation in milliseconds
 */
@Composable
fun AnimatedCounter(
    targetValue: Double,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
    durationMillis: Int = 1000
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "counterAnimation"
    )

    Text(
        text = formatValue(animatedValue.toDouble()),
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}