package com.example.money.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.money.navigation.Routes
import com.example.money.navigation.bottomNavItems
import com.example.money.navigation.navigateWithSaveState
import com.example.money.ui.theme.MoneyTheme
import com.example.money.viewmodel.FinancialViewModel

/**
 * Main activity for the Money app
 * Serves as the entry point and sets up the navigation structure
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Initialize the ViewModel
            val viewModel: FinancialViewModel = viewModel()
            
            // Apply theme settings from ViewModel
            MoneyTheme(
                darkTheme = viewModel.isDarkTheme.value,
                dynamicColor = viewModel.isDynamicTheme.value
            ) {
                MainApp(viewModel)
            }
        }
    }
}

/**
 * Main composable that sets up the app's navigation and scaffold structure
 */
@Composable
fun MainApp(viewModel: FinancialViewModel) {
    // Create a NavController to handle navigation between screens
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                // Create navigation items from our defined routes
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Use our extension function to navigate while preserving state
                            navController.navigateWithSaveState(screen.route)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Set up the navigation host with our routes
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(viewModel)
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(viewModel)
            }
            composable(Routes.BUDGET) {
                BudgetScreen(viewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel)
            }
        }
    }
}

