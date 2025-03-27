// BudgetUtils.kt
package com.example.money.utils

import com.example.money.database.BudgetEntity
import com.example.money.models.Transaction
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for budget management and financial planning features.
 * Provides functionality for setting budgets, tracking spending against budgets,
 * and generating budget-related insights.
 */
object BudgetUtils {

    /**
     * Calculate spending against budget for a specific category
     * @param transactions List of transactions to analyze
     * @param category The category to calculate budget for
     * @param budgetAmount The budget amount set for the category
     * @param startDate The start date of the budget period
     * @param endDate The end date of the budget period
     * @param rolloverUnused Whether to include leftover budget from previous periods
     * @param alertThreshold Percent of budget at which to trigger alert (0.0-1.0)
     * @return BudgetStatus object containing budget metrics
     */
    fun calculateCategoryBudgetStatus(
        transactions: List<Transaction>,
        category: String,
        budgetAmount: Double,
        startDate: Date,
        endDate: Date,
        rolloverUnused: Boolean = false,
        alertThreshold: Double = 0.8
    ): BudgetStatus {
        val filteredTransactions = transactions.filter {
            it.type == "Debit" &&
                    it.category == category &&
                    it.timestamp >= startDate &&
                    it.timestamp <= endDate
        }

        // Calculate total spending for the category
        val totalSpent = filteredTransactions.sumOf { it.amount.toDouble() }

        // Handle rollover from previous period if enabled
        var adjustedBudgetAmount = budgetAmount
        if (rolloverUnused) {
            // Calculate previous period dates
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.MONTH, -1)
            val prevStartDate = calendar.time
            
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val prevEndDate = calendar.time
            
            // Filter transactions from previous period
            val prevPeriodTransactions = transactions.filter {
                it.type == "Debit" &&
                        it.category == category &&
                        it.timestamp >= prevStartDate &&
                        it.timestamp <= prevEndDate
            }
            
            // Calculate previous period spending
            val prevPeriodSpent = prevPeriodTransactions.sumOf { it.amount.toDouble() }
            
            // If we spent less than budget in previous period, add the difference to current budget
            if (prevPeriodSpent < budgetAmount) {
                adjustedBudgetAmount += (budgetAmount - prevPeriodSpent)
            }
        }

        // Calculate remaining budget
        val remaining = adjustedBudgetAmount - totalSpent

        // Calculate percentage of budget used
        val percentUsed = if (adjustedBudgetAmount > 0) (totalSpent / adjustedBudgetAmount) * 100 else 0.0

        // Calculate daily spending rate
        val daysInPeriod = ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)) + 1
        val dailySpendingRate = if (daysInPeriod > 0) totalSpent / daysInPeriod else 0.0

        // Calculate daily budget allowance
        val dailyBudget = if (daysInPeriod > 0) adjustedBudgetAmount / daysInPeriod else 0.0

        // Calculate days until budget depleted
        val daysUntilDepleted = if (dailySpendingRate > 0) remaining / dailySpendingRate else Double.POSITIVE_INFINITY

        // Determine budget status with more nuanced thresholds
        val status = when {
            percentUsed >= 100 -> BudgetStatus.Status.EXCEEDED
            percentUsed >= 90 -> BudgetStatus.Status.CRITICAL
            percentUsed >= 75 -> BudgetStatus.Status.WARNING
            percentUsed >= 50 -> BudgetStatus.Status.ATTENTION
            else -> BudgetStatus.Status.GOOD
        }

        // Calculate trend (are we spending faster or slower than budgeted?)
        val trend = when {
            dailySpendingRate > dailyBudget * 1.1 -> BudgetStatus.Trend.INCREASING_RAPIDLY
            dailySpendingRate > dailyBudget -> BudgetStatus.Trend.INCREASING
            dailySpendingRate < dailyBudget * 0.9 -> BudgetStatus.Trend.DECREASING
            else -> BudgetStatus.Trend.STABLE
        }
        
        // Calculate forecast for end of period
        val currentDay = ((System.currentTimeMillis() - startDate.time) / (1000 * 60 * 60 * 24)) + 1
        val remainingDays = daysInPeriod - currentDay
        val forecastedAdditionalSpending = dailySpendingRate * remainingDays
        val forecastedTotalSpending = totalSpent + forecastedAdditionalSpending
        val forecastedPercentUsed = if (adjustedBudgetAmount > 0) (forecastedTotalSpending / adjustedBudgetAmount) * 100 else 0.0
        
        // Forecast status at end of period
        val forecastStatus = when {
            forecastedPercentUsed >= 100 -> BudgetStatus.Status.EXCEEDED
            forecastedPercentUsed >= 90 -> BudgetStatus.Status.CRITICAL
            forecastedPercentUsed >= 75 -> BudgetStatus.Status.WARNING
            forecastedPercentUsed >= 50 -> BudgetStatus.Status.ATTENTION
            else -> BudgetStatus.Status.GOOD
        }
        
        // Daily spending breakdown
        val dailySpending = calculateDailySpending(filteredTransactions, startDate, endDate)
        
        // Calculate highest spending day
        val highestSpendingDay = dailySpending.entries.maxByOrNull { it.value }
        
        return BudgetStatus(
            category = category,
            budgetAmount = adjustedBudgetAmount,
            spent = totalSpent,
            remaining = remaining,
            percentUsed = percentUsed,
            status = status,
            transactions = filteredTransactions,
            dailySpendingRate = dailySpendingRate,
            dailyBudget = dailyBudget,
            daysUntilDepleted = daysUntilDepleted.toInt(),
            trend = trend,
            dailySpending = dailySpending,
            highestSpendingDate = highestSpendingDay?.key,
            highestSpendingAmount = highestSpendingDay?.value ?: 0.0,
            forecastedTotalSpending = forecastedTotalSpending,
            forecastedPercentUsed = forecastedPercentUsed,
            forecastStatus = forecastStatus,
            adjustedForRollover = rolloverUnused && adjustedBudgetAmount > budgetAmount
        )
    }
    
    /**
     * Calculate daily spending for a list of transactions
     */
    private fun calculateDailySpending(
        transactions: List<Transaction>,
        startDate: Date,
        endDate: Date
    ): Map<Date, Double> {
        val result = mutableMapOf<Date, Double>()
        
        // Initialize calendar for date manipulation
        val calendar = Calendar.getInstance()
        
        // Initialize result with all days in range (with zero values)
        calendar.time = startDate
        while (!calendar.time.after(endDate)) {
            val dayStart = calendar.time
            result[Date(dayStart.time)] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Populate with actual spending
        transactions.forEach { transaction ->
            calendar.time = transaction.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayKey = calendar.time
            
            result[dayKey] = (result[dayKey] ?: 0.0) + transaction.amount.toDouble()
        }
        
        return result
    }

    /**
     * Analyze budget efficiency and provide improvement suggestions
     * @param budgets List of current budgets
     * @param transactions List of transactions to analyze
     * @param startDate Start date for analysis period
     * @param endDate End date for analysis period
     * @return Map of category to optimization suggestions
     */
    fun analyzeBudgetEfficiency(
        budgets: List<BudgetEntity>,
        transactions: List<Transaction>,
        startDate: Date,
        endDate: Date
    ): Map<String, BudgetOptimizationSuggestion> {
        val suggestions = mutableMapOf<String, BudgetOptimizationSuggestion>()
        
        budgets.forEach { budget ->
            // Calculate budget status for the category
            val status = calculateCategoryBudgetStatus(
                transactions = transactions,
                category = budget.category,
                budgetAmount = budget.budgetAmount,
                startDate = startDate,
                endDate = endDate,
                rolloverUnused = budget.rolloverUnused,
                alertThreshold = budget.alertThreshold
            )
            
            // Generate optimization suggestion based on spending patterns
            val suggestion = when {
                // Consistently under budget by significant margin
                status.percentUsed < 50.0 && status.trend == BudgetStatus.Trend.DECREASING ->
                    BudgetOptimizationSuggestion(
                        suggestedBudget = status.spent * 1.2, // Slightly more than actual spending
                        message = "You're consistently spending much less than budgeted. Consider reducing your budget to ${formatCurrency(status.spent * 1.2)}.",
                        type = BudgetOptimizationType.DECREASE
                    )
                
                // Consistently over budget
                status.percentUsed > 110.0 && (status.trend == BudgetStatus.Trend.INCREASING || status.trend == BudgetStatus.Trend.INCREASING_RAPIDLY) ->
                    BudgetOptimizationSuggestion(
                        suggestedBudget = status.spent * 1.1, // Slightly more than actual spending
                        message = "You're consistently exceeding this budget. Consider increasing it to ${formatCurrency(status.spent * 1.1)} or finding ways to reduce spending.",
                        type = BudgetOptimizationType.INCREASE
                    )
                
                // Budget is just right
                status.percentUsed in 85.0..105.0 && status.trend == BudgetStatus.Trend.STABLE ->
                    BudgetOptimizationSuggestion(
                        suggestedBudget = budget.budgetAmount,
                        message = "Your budget is well-aligned with your spending patterns.",
                        type = BudgetOptimizationType.MAINTAIN
                    )
                
                // Moderately under budget
                status.percentUsed < 85.0 ->
                    BudgetOptimizationSuggestion(
                        suggestedBudget = status.spent * 1.15,
                        message = "You have room in this budget. Consider reallocating ${formatCurrency(budget.budgetAmount - (status.spent * 1.15))} to other categories.",
                        type = BudgetOptimizationType.OPTIMIZE
                    )
                
                // Default case
                else ->
                    BudgetOptimizationSuggestion(
                        suggestedBudget = budget.budgetAmount,
                        message = "Continue monitoring this budget category.",
                        type = BudgetOptimizationType.MAINTAIN
                    )
            }
            
            suggestions[budget.category] = suggestion
        }
        
        return suggestions
    }

    /**
     * Check the status of a savings goal based on transaction history
     * @param transactions List of all transactions to analyze
     * @param savingsGoal The target savings amount
     * @return SavingsGoalStatus object with analysis
     */
    fun checkSavingsGoalProgress(transactions: List<Transaction>, savingsGoal: Double): SavingsGoalStatus {
        // Calculate total income and expenses
        val totalIncome = transactions
            .filter { it.type == "Credit" }
            .sumOf { it.amount.toDouble() }
            
        val totalExpenses = transactions
            .filter { it.type == "Debit" }
            .sumOf { it.amount.toDouble() }

        // Current savings
        val currentSavings = totalIncome - totalExpenses
        
        // Calculate percentage of goal achieved
        val percentOfGoal = if (savingsGoal > 0) (currentSavings / savingsGoal) * 100 else 0.0
        
        // Analyze spending and saving patterns
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -6) // Look at last 6 months
        val sixMonthsAgo = calendar.time

        val recentTransactions = transactions.filter { it.timestamp >= sixMonthsAgo }
        
        // Calculate monthly averages
        val monthlyIncomeSum = recentTransactions
            .filter { it.type == "Credit" }
            .sumOf { it.amount.toDouble() }
            
        val monthlyExpensesSum = recentTransactions
            .filter { it.type == "Debit" }
            .sumOf { it.amount.toDouble() }
            
        val months = 6.0 // We're looking at 6 months
        
        val avgMonthlyIncome = monthlyIncomeSum / months
        val avgMonthlyExpenses = monthlyExpensesSum / months
        val avgMonthlySavings = avgMonthlyIncome - avgMonthlyExpenses
        
        // Project future savings
        val monthsToGoal = if (avgMonthlySavings > 0) (savingsGoal - currentSavings) / avgMonthlySavings else Double.POSITIVE_INFINITY
        
        // Check if on track (savings rate vs goal timeframe)
        val isOnTrack = avgMonthlySavings > 0 && monthsToGoal < 24 // Set 24 months as a reasonable timeframe
        
        // Calculate projected savings in 1 year
        val projectedSavings = currentSavings + (avgMonthlySavings * 12)
        
        // Calculate daily savings needed to reach goal in 1 year
        val remainingToSave = savingsGoal - currentSavings
        val dailySavingsNeeded = remainingToSave / 365
        
        // Projected income and expenses for next year
        val projectedIncome = avgMonthlyIncome * 12
        val projectedExpenses = avgMonthlyExpenses * 12
        
        // Calculate savings rate (percentage of income saved)
        val savingsRate = if (avgMonthlyIncome > 0) (avgMonthlySavings / avgMonthlyIncome) * 100 else 0.0
        
        // Determine goal difficulty based on savings rate
        val difficultyLevel = when {
            savingsRate <= 0 -> SavingsGoalDifficulty.IMPOSSIBLE
            savingsRate < 5 -> SavingsGoalDifficulty.VERY_DIFFICULT
            savingsRate < 10 -> SavingsGoalDifficulty.DIFFICULT
            savingsRate < 20 -> SavingsGoalDifficulty.MODERATE
            else -> SavingsGoalDifficulty.ACHIEVABLE
        }

        return SavingsGoalStatus(
            savingsGoal = savingsGoal,
            currentSavings = currentSavings,
            percentOfGoal = percentOfGoal,
            isOnTrack = isOnTrack,
            projectedSavings = projectedSavings,
            remainingToSave = savingsGoal - currentSavings,
            dailySavingsNeeded = dailySavingsNeeded,
            projectedIncome = projectedIncome,
            projectedExpenses = projectedExpenses,
            monthsToGoal = monthsToGoal,
            savingsRate = savingsRate,
            difficultyLevel = difficultyLevel
        )
    }

    /**
     * Format currency amount to string with appropriate currency symbol
     */
    fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance()
        numberFormat.currency = Currency.getInstance("INR")
        return numberFormat.format(amount)
    }
}

/**
 * Data class representing the status of a budget category
 */
data class BudgetStatus(
    val category: String,
    val budgetAmount: Double,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Double,
    val status: Status,
    val transactions: List<Transaction>,
    val dailySpendingRate: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val daysUntilDepleted: Int = Int.MAX_VALUE,
    val trend: Trend = Trend.STABLE,
    val dailySpending: Map<Date, Double> = emptyMap(),
    val highestSpendingDate: Date? = null,
    val highestSpendingAmount: Double = 0.0,
    val forecastedTotalSpending: Double = 0.0,
    val forecastedPercentUsed: Double = 0.0,
    val forecastStatus: Status = Status.GOOD,
    val adjustedForRollover: Boolean = false
) {
    // Computed property to check if over budget
    val isOverBudget: Boolean get() = spent > budgetAmount

    // Will the budget be exceeded by the end of the period?
    val willExceedBudget: Boolean get() = forecastedTotalSpending > budgetAmount
    
    enum class Status {
        GOOD,       // Less than 50% used
        ATTENTION,  // 50-75% used
        WARNING,    // 75-90% used
        CRITICAL,   // 90-100% used
        EXCEEDED    // Over 100% used
    }
    
    enum class Trend {
        DECREASING,         // Spending less than budgeted
        STABLE,             // Spending at budgeted rate
        INCREASING,         // Spending slightly more than budgeted
        INCREASING_RAPIDLY  // Spending significantly more than budgeted
    }
}

/**
 * Data class representing budget optimization suggestions
 */
data class BudgetOptimizationSuggestion(
    val suggestedBudget: Double,
    val message: String,
    val type: BudgetOptimizationType
)

/**
 * Enum for budget optimization types
 */
enum class BudgetOptimizationType {
    INCREASE,   // Suggest increasing the budget
    DECREASE,   // Suggest decreasing the budget
    OPTIMIZE,   // Suggest optimization/reallocation
    MAINTAIN    // Suggest maintaining current budget
}

/**
 * Data class representing the status of a savings goal
 */
data class SavingsGoalStatus(
    val savingsGoal: Double,
    val currentSavings: Double,
    val percentOfGoal: Double,
    val isOnTrack: Boolean,
    val projectedSavings: Double,
    val remainingToSave: Double,
    val dailySavingsNeeded: Double,
    val projectedIncome: Double,
    val projectedExpenses: Double,
    val monthsToGoal: Double = Double.POSITIVE_INFINITY,
    val savingsRate: Double = 0.0,
    val difficultyLevel: SavingsGoalDifficulty = SavingsGoalDifficulty.MODERATE
)

/**
 * Enum for savings goal difficulty levels
 */
enum class SavingsGoalDifficulty {
    ACHIEVABLE,
    MODERATE,
    DIFFICULT,
    VERY_DIFFICULT,
    IMPOSSIBLE
}