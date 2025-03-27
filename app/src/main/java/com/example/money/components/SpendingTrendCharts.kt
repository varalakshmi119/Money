package com.example.money.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.money.models.Transaction
import com.example.money.utils.FinancialUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

/**
 * Enum class to represent different time periods for trend analysis
 */
enum class TrendPeriod {
    WEEKLY, MONTHLY, YEARLY
}

/**
 * Component for displaying spending trend charts with a dropdown to select the time period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingTrendChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.MONTHLY) }
    val animatedProgress = remember { Animatable(initialValue = 0f) }
    val coroutineScope = rememberCoroutineScope()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("INR")
        }
    }

    val trendData = when (selectedPeriod) {
        TrendPeriod.WEEKLY -> FinancialUtils.getWeeklyTrends(transactions)
        TrendPeriod.MONTHLY -> FinancialUtils.getMonthlyTrends(transactions)
        TrendPeriod.YEARLY -> FinancialUtils.getYearlyTrends(transactions)
    }

    val maxValue = trendData.values.maxOfOrNull { it } ?: 0.0

    LaunchedEffect(trendData) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending Trends",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Improved time period selector with better styling - more compact
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(2.dp)
                ) {
                    TrendPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.01f)
                                )
                                .clickable {
                                    if (!isSelected) {
                                        selectedPeriod = period
                                        coroutineScope.launch {
                                            animatedProgress.snapTo(0f)
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(period) {
                                    TrendPeriod.WEEKLY -> "W"
                                    TrendPeriod.MONTHLY -> "M"
                                    TrendPeriod.YEARLY -> "Y"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trendData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Not enough data to display trends",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            val axisColor = MaterialTheme.colorScheme.outline
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f).toArgb()
            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryTransparentColor = primaryColor.copy(alpha = 0.7f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Ensure we don't divide by zero
                    val dataSize = maxOf(trendData.size, 1)
                    val barWidth = canvasWidth / (dataSize + 1)
                    val padding = barWidth * 0.2f

                    // Draw axes
                    drawLine(
                        start = Offset(40f, 0f),
                        end = Offset(40f, canvasHeight - 30f),
                        color = axisColor.copy(alpha = 0.2f),
                        strokeWidth = 2f
                    )

                    drawLine(
                        start = Offset(40f, canvasHeight - 30f),
                        end = Offset(canvasWidth, canvasHeight - 30f),
                        color = axisColor.copy(alpha = 0.2f),
                        strokeWidth = 2f
                    )

                    // Only draw labels and bars if we have data
                    if (maxValue > 0) {
                        val yLabelValues = listOf(0.25f, 0.5f, 0.75f, 1f)
                        yLabelValues.forEach { fraction ->
                            val yPos = (canvasHeight - 30f) * (1 - fraction)
                            drawContext.canvas.nativeCanvas.apply {
                                drawText(
                                    currencyFormatter.format(maxValue * fraction),
                                    8f,
                                    yPos + 8,
                                    android.graphics.Paint().apply {
                                        color = onSurfaceColor
                                        textSize = 12f
                                    }
                                )
                            }
                            drawLine(
                                start = Offset(40f, yPos),
                                end = Offset(45f, yPos),
                                color = axisColor.copy(alpha = 0.2f),
                                strokeWidth = 1f
                            )
                        }

                        // Draw bars
                        trendData.entries.forEachIndexed { index, (period, value) ->
                            val barHeight = ((value / maxValue) * (canvasHeight - 30f) * animatedProgress.value).toFloat()
                            val barX = (index + 1) * barWidth - barWidth + padding + 40f
                            val barY = canvasHeight - 30f - barHeight
                            val barWidthFinal = barWidth - (padding * 2)
                            
                            // Draw bar with rounded corners and gradient
                            drawRoundRect(
                                color = primaryTransparentColor,
                                topLeft = Offset(barX, barY),
                                size = Size(barWidthFinal, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            
                            // Draw outline with rounded corners
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(barX, barY),
                                size = Size(barWidthFinal, barHeight),
                                cornerRadius = CornerRadius(4f, 4f),
                                style = Stroke(width = 1f)
                            )
                            
                            // Draw value on top of the bar if it's tall enough
                            if (barHeight > 40f) {
                                drawContext.canvas.nativeCanvas.apply {
                                    drawText(
                                        currencyFormatter.format(value),
                                        barX + (barWidthFinal / 2),
                                        barY - 8f,
                                        android.graphics.Paint().apply {
                                            color = primaryColor.copy(alpha = 0.9f).toArgb()
                                            textSize = 10f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (trendData.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(top = 8.dp, start = 40.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        trendData.keys.forEach { period ->
                            Text(
                                text = period,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalSpending = trendData.values.sum()
            val averageSpending = if (trendData.isNotEmpty()) totalSpending / trendData.size else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    title = "Total",
                    value = currencyFormatter.format(totalSpending),
                    modifier = Modifier.weight(1f)
                )

                SummaryItem(
                    title = "Average",
                    value = currencyFormatter.format(averageSpending),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}