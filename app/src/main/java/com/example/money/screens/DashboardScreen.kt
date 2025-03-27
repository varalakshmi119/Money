package com.example.money.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.money.components.AnimatedCounter
import com.example.money.components.EditSavingsGoalDialog
import com.example.money.components.InsightItem
import com.example.money.components.SpendingTrendChart
import com.example.money.ui.theme.CategoryColors
import com.example.money.utils.BudgetStatus
import com.example.money.utils.BudgetUtils
import com.example.money.utils.SavingsGoalStatus
import com.example.money.viewmodel.FinancialViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency

/**
 * Enhanced Dashboard screen showing essential financial overview
 * Focuses on key information with improved visual hierarchy and modern UI elements
 */
@Composable
fun DashboardScreen(viewModel: FinancialViewModel) {
    // Collect data from the ViewModel
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val totalDebits by viewModel.totalDebits.collectAsState(initial = 0.0)
    val totalCredits by viewModel.totalCredits.collectAsState(initial = 0.0)
    val categoryTotals by viewModel.categoryTotals.collectAsState(initial = emptyList())
    val budgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    val showEditSavingsGoalDialog by viewModel.showEditSavingsGoalDialog.collectAsState(initial = false)

    // Calculate current balance
    val balance = (totalCredits ?: 0.0) - (totalDebits ?: 0.0)

    // Get savings goal progress
    val savingsGoalProgress = remember(transactions) {
        viewModel.checkSavingsGoalProgress(transactions)
    }

    // Get monthly analysis for insights
    val monthlyAnalysis = remember(transactions) {
        viewModel.getMonthlyAnalysis(transactions)
    }

    // Format currency values
    val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("INR")
    }

    // Calculate budget statuses
    val calendar = Calendar.getInstance()
    val startDate = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.time
    val endDate = calendar.apply {
        set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    }.time

    val budgetStatuses = remember(budgets, transactions, startDate, endDate) {
        budgets.map { budget ->
            BudgetUtils.calculateCategoryBudgetStatus(
                transactions = transactions.filter { it.category == budget.category },
                category = budget.category,
                budgetAmount = budget.budgetAmount,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    // Show EditSavingsGoalDialog if flag is set
    if (showEditSavingsGoalDialog) {
        EditSavingsGoalDialog(
            visible = true,
            currentGoal = savingsGoalProgress.savingsGoal,
            onDismiss = { viewModel.hideEditSavingsGoalDialog() },
            onSave = { newGoal ->
                viewModel.updateSavingsGoal(newGoal)
                viewModel.hideEditSavingsGoalDialog()
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Dashboard Header
            DashboardHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Account Balance Card with animation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated counter for balance
                    AnimatedCounter(
                        targetValue = balance,
                        formatValue = { currencyFormatter.format(it) },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            AnimatedCounter(
                                targetValue = totalCredits ?: 0.0,
                                formatValue = { currencyFormatter.format(it) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            AnimatedCounter(
                                targetValue = totalDebits ?: 0.0,
                                formatValue = { currencyFormatter.format(it) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Spending Trends Section (Increased Size)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // Spending Trend Chart with increased height
                SpendingTrendChart(
                    transactions = transactions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Spending Categories Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Spending Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    if (categoryTotals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No spending data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Show all categories instead of just top ones
                        val allCategories = categoryTotals.sortedByDescending { it.total }
                        
                        allCategories.forEach { category ->
                            val percentage = if (totalDebits!! > 0) {
                                (category.total / totalDebits!!) * 100
                            } else 0.0

                            CategoryProgressItem(
                                category = category.category,
                                amount = category.total,
                                percentage = percentage,
                                currencyFormatter = currencyFormatter
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Budget Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    if (budgetStatuses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No budgets set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Calculate overall budget metrics
                        val totalBudget = budgetStatuses.sumOf { it.budgetAmount }
                        val totalSpent = budgetStatuses.sumOf { it.spent }
                        val overallPercentUsed = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0

                        // Overall progress
                        Text(
                            text = "Overall: ${currencyFormatter.format(totalSpent)} of ${currencyFormatter.format(totalBudget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (overallPercentUsed / 100).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                overallPercentUsed >= 100 -> MaterialTheme.colorScheme.error
                                overallPercentUsed >= 80 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Show all category budgets instead of just top ones
                        budgetStatuses.sortedBy { it.category }.forEach { status ->
                            // Find if there's a matching category in spending
                            val categorySpending = categoryTotals.find { it.category == status.category }
                            
                            // Create a combined view of budget and actual spending
                            BudgetWithSpendingItem(
                                status = status,
                                actualSpending = categorySpending?.total ?: 0.0,
                                currencyFormatter = currencyFormatter
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Savings Goal Card
            SavingsGoalCard(
                savingsGoalStatus = savingsGoalProgress,
                currencyFormatter = currencyFormatter,
                onEditGoal = { viewModel.showEditSavingsGoalDialog() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Financial Insights Section with improved visual design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Financial Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Spacer(modifier = Modifier.height(16.dp))

                    // Get the most recent month's analysis
                    val latestMonth = monthlyAnalysis.entries.maxByOrNull { it.key }?.value

                    if (latestMonth == null) {
                        Text(
                            text = "Add more transactions to see personalized insights",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Most expensive category
                        InsightItem(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "Top Spending Category",
                            description = "${latestMonth.mostExpensiveCategory} accounts for ${String.format("%.1f", (latestMonth.categoryBreakdown[latestMonth.mostExpensiveCategory] ?: 0.0) / latestMonth.totalDebits * 100)}% of your spending"
                        )

                        // Balance trend
                        InsightItem(
                            icon = Icons.Default.AccountBalance,
                            title = "Balance Trend",
                            description = if (latestMonth.balance >= 0) {
                                "You're saving money this month with a positive balance"
                            } else {
                                "You're spending more than your income this month"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Dashboard header with greeting based on time of day
 */
@Composable
fun DashboardHeader() {
    val calendar = Calendar.getInstance()
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

    val greeting = when {
        hourOfDay < 12 -> "Good Morning!"
        hourOfDay < 17 -> "Good Afternoon!"
        else -> "Good Evening!"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Here's your financial overview",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/**
 * Category Progress Item for displaying spending by category 
 */
@Composable
fun CategoryProgressItem(
    category: String,
    amount: Double,
    percentage: Double,
    currencyFormatter: NumberFormat
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = currencyFormatter.format(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (percentage / 100).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CategoryColors.getColorForCategory(category),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Percentage
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * New component that combines budget status with actual spending
 */
@Composable
fun BudgetWithSpendingItem(
    status: BudgetStatus,
    actualSpending: Double,
    currencyFormatter: NumberFormat
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category color indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CategoryColors.getColorForCategory(status.category))
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = status.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${currencyFormatter.format(status.spent)} / ${currencyFormatter.format(status.budgetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar with budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (status.percentUsed / 100).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    status.percentUsed >= 100 -> MaterialTheme.colorScheme.error
                    status.percentUsed >= 80 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Percentage
            Text(
                text = "${status.percentUsed.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    status.percentUsed >= 100 -> MaterialTheme.colorScheme.error
                    status.percentUsed >= 80 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        }
        
        // Additional status info
        if (status.daysUntilDepleted < 30 && status.daysUntilDepleted != Int.MAX_VALUE) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "At current rate, depleted in ${status.daysUntilDepleted} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (status.willExceedBudget && !status.isOverBudget) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Forecasted to exceed budget this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * Extension function for SavingsGoalCard to accept SavingsGoalStatus 
 */
@Composable
fun SavingsGoalCard(
    savingsGoalStatus: SavingsGoalStatus,
    currencyFormatter: NumberFormat,
    onEditGoal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Savings Goal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // Edit button
                IconButton(onClick = onEditGoal) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Savings Goal",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )

                    // Use AnimatedCounter for rolling animation
                    AnimatedCounter(
                        targetValue = savingsGoalStatus.savingsGoal,
                        formatValue = { currencyFormatter.format(it) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )

                    // Use AnimatedCounter for rolling animation
                    AnimatedCounter(
                        targetValue = savingsGoalStatus.currentSavings,
                        formatValue = { currencyFormatter.format(it) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { (savingsGoalStatus.percentOfGoal / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = if (savingsGoalStatus.isOnTrack)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status text
            Text(
                text = if (savingsGoalStatus.isOnTrack)
                    "You're on track to meet your savings goal!"
                else
                    "You're behind on your savings goal. Try to save more!",
                style = MaterialTheme.typography.bodyMedium,
                color = if (savingsGoalStatus.isOnTrack)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.tertiary
            )
        }
    }
}