package com.example.money.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.money.components.AddBudgetDialog
import com.example.money.components.BudgetAlertBanner
import com.example.money.components.BudgetAnalysisCard
import com.example.money.components.BudgetItem
import com.example.money.components.BudgetOptimizationCard
import com.example.money.components.BudgetSummaryCard
import com.example.money.components.EmptyBudgetCard
import com.example.money.database.BudgetEntity
import com.example.money.utils.BudgetUtils
import com.example.money.utils.Constants
import com.example.money.viewmodel.BudgetSortOrder
import com.example.money.viewmodel.FinancialViewModel
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Enhanced Budget management screen with modern design and improved functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: FinancialViewModel) {
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val budgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTimeRange by rememberSaveable { mutableStateOf(BudgetTimeRange.CURRENT_MONTH) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val sortOrder by viewModel.sortOrder
    val filterOverBudget by viewModel.filterOverBudget
    
    // Date calculations
    val (startDate, endDate) = remember(selectedTimeRange) {
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        when (selectedTimeRange) {
            BudgetTimeRange.CURRENT_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = calendar.time
                start to end
            }
            BudgetTimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = calendar.time
                start to end
            }
            BudgetTimeRange.THREE_MONTHS -> {
                calendar.add(Calendar.MONTH, -2)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.time
                calendar.add(Calendar.MONTH, 2)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = calendar.time
                start to end
            }
            BudgetTimeRange.CUSTOM -> {
                // For future implementation of custom date range
                val start = calendar.time
                val end = calendar.time
                start to end
            }
        }
    }

    // Budget status calculations
    val budgetStatuses = remember(transactions, budgets, startDate, endDate) {
        budgets.map { budget ->
            BudgetUtils.calculateCategoryBudgetStatus(
                transactions = transactions.filter { it.category == budget.category },
                category = budget.category,
                budgetAmount = budget.budgetAmount,
                startDate = startDate,
                endDate = endDate,
                rolloverUnused = budget.rolloverUnused,
                alertThreshold = budget.alertThreshold
            )
        }
    }

    // Budget optimization suggestions
    val budgetOptimizations = remember(transactions, budgets, startDate, endDate) {
        BudgetUtils.analyzeBudgetEfficiency(
            budgets = budgets,
            transactions = transactions,
            startDate = startDate,
            endDate = endDate
        )
    }

    // Savings goal status
    val savingsGoalStatus = remember(transactions) {
        viewModel.checkSavingsGoalProgress(transactions)
    }

    // Filter and sort budgets
    val sortedAndFilteredBudgetStatuses by remember(budgetStatuses, sortOrder, filterOverBudget) {
        derivedStateOf {
            var result = budgetStatuses
            
            // Apply filter
            if (filterOverBudget) {
                result = result.filter { it.isOverBudget || it.willExceedBudget }
            }
            
            // Apply sort
            result = when (sortOrder) {
                BudgetSortOrder.AMOUNT_DESC -> result.sortedByDescending { it.budgetAmount }
                BudgetSortOrder.CATEGORY_ASC -> result.sortedBy { it.category }
                BudgetSortOrder.PERCENTAGE_USED -> result.sortedByDescending { it.percentUsed }
                else -> result // Keep default order
            }
            
            result
        }
    }
    
    // Count of over-budget categories
    val overBudgetCount = budgetStatuses.count { it.isOverBudget }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Budget Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track and optimize your spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                actions = {
                    // Sort button with dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Budgets",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default") },
                                onClick = {
                                    viewModel.setSortOrder(BudgetSortOrder.DEFAULT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By Amount") },
                                onClick = {
                                    viewModel.setSortOrder(BudgetSortOrder.AMOUNT_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By Category") },
                                onClick = {
                                    viewModel.setSortOrder(BudgetSortOrder.CATEGORY_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By % Used") },
                                onClick = {
                                    viewModel.setSortOrder(BudgetSortOrder.PERCENTAGE_USED)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                    
                    // Filter button with badge for over-budget items
                    Box {
                        BadgedBox(
                            badge = {
                                if (overBudgetCount > 0) {
                                    Badge { Text(overBudgetCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { showFilterMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter Budgets",
                                    tint = if (filterOverBudget) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (filterOverBudget) "Show All" else "Show Over Budget") },
                                onClick = {
                                    viewModel.toggleFilterOverBudget()
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBudgetDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Budget"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Time range selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedTimeRange == BudgetTimeRange.CURRENT_MONTH,
                        onClick = { selectedTimeRange = BudgetTimeRange.CURRENT_MONTH },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) {
                        Text("This Month")
                    }
                    SegmentedButton(
                        selected = selectedTimeRange == BudgetTimeRange.LAST_MONTH,
                        onClick = { selectedTimeRange = BudgetTimeRange.LAST_MONTH },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) {
                        Text("Last Month")
                    }
                    SegmentedButton(
                        selected = selectedTimeRange == BudgetTimeRange.THREE_MONTHS,
                        onClick = { selectedTimeRange = BudgetTimeRange.THREE_MONTHS },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) {
                        Text("3 Months")
                    }
                }
            }

            // Main budget content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show alert banner for over-budget categories
                if (budgetStatuses.any { it.isOverBudget }) {
                    item {
                        BudgetAlertBanner(
                            overBudgetStatuses = budgetStatuses.filter { it.isOverBudget }
                        )
                    }
                }
                
                // Budget Summary Card
                item {
                    BudgetSummaryCard(
                        budgetStatuses = budgetStatuses,
                        selectedTimeRange = selectedTimeRange
                    )
                }
                
                // Budget Analysis Card
                if (budgetStatuses.isNotEmpty()) {
                    item {
                        BudgetAnalysisCard(
                            budgetStatuses = budgetStatuses,
                            totalBudget = budgetStatuses.sumOf { it.budgetAmount },
                            totalSpent = budgetStatuses.sumOf { it.spent }
                        )
                    }
                    
                    // Budget Optimization Card
                    item {
                        BudgetOptimizationCard(
                            optimizations = budgetOptimizations,
                            onApplyOptimization = { category, amount ->
                                val budget = budgets.find { it.category == category }
                                budget?.let {
                                    viewModel.updateBudget(it.copy(budgetAmount = amount))
                                }
                            }
                        )
                    }
                    
                    // Savings Goal Card
                    item {
                        SavingsGoalCard(
                            savingsGoalStatus = savingsGoalStatus,
                            currencyFormatter = java.text.NumberFormat.getCurrencyInstance().apply {
                                currency = java.util.Currency.getInstance("INR")
                            },
                            onEditGoal = { viewModel.showEditSavingsGoalDialog() }
                        )
                    }
                }
                
                // Individual budget items or empty state
                if (sortedAndFilteredBudgetStatuses.isNotEmpty()) {
                    // Section header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Budgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "${sortedAndFilteredBudgetStatuses.size} budgets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Budget items
                    items(
                        items = sortedAndFilteredBudgetStatuses,
                        key = { it.category }
                    ) { status ->
                        BudgetItem(
                            budgetStatus = status,
                            onEdit = {
                                selectedCategory = status.category
                                showAddBudgetDialog = true
                            },
                            onDelete = {
                                viewModel.deleteBudgetByCategory(status.category)
                            }
                        )
                    }
                } else if (budgets.isEmpty()) {
                    // Empty state when no budgets are set
                    item {
                        EmptyBudgetCard(
                            onAddBudgetClick = { showAddBudgetDialog = true }
                        )
                    }
                } else {
                    // Empty state when filter returned no results
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "No matching budgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Try changing your filters to see more budgets.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.toggleFilterOverBudget() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text("Show All Budgets")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit Budget Dialog
        if (showAddBudgetDialog) {
            val initialCategory = selectedCategory
            val initialBudget = budgets.find { it.category == initialCategory }

            AddBudgetDialog(
                initialCategory = initialCategory,
                initialAmount = initialBudget?.budgetAmount ?: 0.0,
                onDismiss = {
                    showAddBudgetDialog = false
                    selectedCategory = null
                },
                onConfirm = { category, amount ->
                    val budget = BudgetEntity(
                        category = category,
                        budgetAmount = amount,
                        lastUpdated = Date()
                    )
                    viewModel.insertBudget(budget)
                    showAddBudgetDialog = false
                    selectedCategory = null
                },
                categories = Constants.TRANSACTION_CATEGORIES
            )
        }
    }
}

/**
 * Enum representing time ranges for budget analysis
 */
enum class BudgetTimeRange(val displayName: String) {
    CURRENT_MONTH("Current Month"),
    LAST_MONTH("Last Month"),
    THREE_MONTHS("Last 3 Months"),
    CUSTOM("Custom Range")
}

