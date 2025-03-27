package com.example.money.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Navigation routes for the app
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val BUDGET = "budget"
    const val SETTINGS = "settings"
}

/**
 * Navigation item data class for bottom navigation
 */
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * List of bottom navigation items
 */
val bottomNavItems = listOf(
    NavItem("Dashboard", Icons.Default.Dashboard, Routes.DASHBOARD),
    NavItem("Transactions", Icons.AutoMirrored.Filled.List, Routes.TRANSACTIONS),
    NavItem("Budget", Icons.Default.AccountBalance, Routes.BUDGET),
    NavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
)

/**
 * Extension function to navigate with saving state
 */
fun NavController.navigateWithSaveState(route: String) {
    this.navigate(route) {
        popUpTo(this@navigateWithSaveState.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}