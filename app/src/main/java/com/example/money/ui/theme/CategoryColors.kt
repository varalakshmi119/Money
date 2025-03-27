package com.example.money.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Utility class to manage category colors
 */
object CategoryColors {
    private val colorMap = mutableMapOf<String, Color>()
    
    // Predefined colors for categories
    private val predefinedColors = listOf(
        Color(0xFF1E88E5), // Blue - Primary
        Color(0xFF2E7D32), // Green - Success
        Color(0xFFF57C00), // Orange - Warning
        Color(0xFFD32F2F), // Red - Error
        Color(0xFF7B1FA2), // Purple - Premium
        Color(0xFF1976D2), // Deep Blue - Investment
        Color(0xFF388E3C), // Forest Green - Savings
        Color(0xFFE64A19), // Deep Orange - Expenses
        Color(0xFF455A64), // Blue Grey - Utilities
        Color(0xFF5D4037), // Brown - Food
        Color(0xFF0288D1), // Light Blue - Entertainment
        Color(0xFF689F38), // Light Green - Shopping
        Color(0xFFFBC02D), // Amber - Travel
        Color(0xFF8E24AA), // Deep Purple - Healthcare
        Color(0xFF0097A7)  // Cyan - Education
    )
    
    /**
     * Get a color for a category. If the category doesn't have a color assigned yet,
     * it will be assigned a color from the predefined list.
     */
    fun getColorForCategory(category: String): Color {
        if (!colorMap.containsKey(category)) {
            // Assign a color based on the hash of the category name
            val index = Math.abs(category.hashCode()) % predefinedColors.size
            colorMap[category] = predefinedColors[index]
        }
        
        return colorMap[category]!!
    }
    
    /**
     * Set a specific color for a category
     */
    fun setColorForCategory(category: String, color: Color) {
        colorMap[category] = color
    }
    
    /**
     * Get all category colors
     */
    fun getAllCategoryColors(): Map<String, Color> {
        return colorMap.toMap()
    }
    
    /**
     * Clear all category color assignments
     */
    fun clearAllColors() {
        colorMap.clear()
    }
}
