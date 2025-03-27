package com.example.money.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.money.database.BudgetEntity
import com.example.money.database.BudgetRepository
import com.example.money.database.FinancialDatabase
import com.example.money.database.TransactionDao
import com.example.money.database.TransactionRepository
import com.example.money.models.MonthlyAnalysis
import com.example.money.models.Transaction
import com.example.money.models.TransactionDTO
import com.example.money.utils.BudgetUtils
import com.example.money.utils.FinancialUtils
import com.example.money.utils.PerformanceUtils
import com.example.money.utils.SavingsGoalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for managing financial data and UI state
 */
class FinancialViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository: TransactionRepository
    private val budgetRepository: BudgetRepository

    val allTransactions: Flow<List<Transaction>>
    val totalDebits: Flow<Double?>
    val totalCredits: Flow<Double?>
    val categories: Flow<List<String>>
    val categoryTotals: Flow<List<TransactionDao.CategoryAmount>>
    private val monthlyTotals: Flow<List<com.example.money.models.MonthlyTotal>>
    val allBudgets: Flow<List<BudgetEntity>>
    val budgetsSortedByAmount: Flow<List<BudgetEntity>>
    val budgetsSortedByCategory: Flow<List<BudgetEntity>>

    // Theme preferences
    private val _isDarkTheme = mutableStateOf(false)
    val isDarkTheme: State<Boolean> = _isDarkTheme

    private val _isDynamicTheme = mutableStateOf(true)
    val isDynamicTheme: State<Boolean> = _isDynamicTheme

    // Savings goal
    private val _savingsGoal = mutableDoubleStateOf(5000.0)
    private val savingsGoal: State<Double> = _savingsGoal
    
    private val _showEditSavingsGoalDialog = MutableStateFlow(false)
    val showEditSavingsGoalDialog: StateFlow<Boolean> = _showEditSavingsGoalDialog.asStateFlow()
    
    // Budget sorting and filtering
    private val _sortOrder = mutableStateOf(BudgetSortOrder.DEFAULT)
    val sortOrder: State<BudgetSortOrder> = _sortOrder
    
    private val _filterOverBudget = mutableStateOf(false)
    val filterOverBudget: State<Boolean> = _filterOverBudget

    init {
        val database = FinancialDatabase.getDatabase(application)
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()

        transactionRepository = TransactionRepository(transactionDao)
        budgetRepository = BudgetRepository(budgetDao)

        allTransactions = transactionRepository.allTransactions
        totalDebits = transactionRepository.totalDebits
        totalCredits = transactionRepository.totalCredits
        categories = transactionRepository.categories
        categoryTotals = transactionRepository.categoryTotals
        monthlyTotals = transactionRepository.monthlyTotals
        allBudgets = budgetRepository.getAllBudgets()
        budgetsSortedByAmount = budgetRepository.getBudgetsSortedByAmount()
        budgetsSortedByCategory = budgetRepository.getBudgetsSortedByCategory()
    }

    // Import transactions from JSON
    fun importTransactions(transactionDTOs: List<TransactionDTO>) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = transactionDTOs.map { it.toTransaction() }
            transactionRepository.insertTransactions(transactions)
        }
    }
    
    // Insert a single transaction
    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.insertTransaction(transaction)
        }
    }

    // Delete all transactions
    fun deleteAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.deleteAllTransactions()
        }
    }

    // Theme management
    fun updateThemePreferences(isDark: Boolean, isDynamic: Boolean) {
        _isDarkTheme.value = isDark
        _isDynamicTheme.value = isDynamic
    }

    // Budget management
    fun insertBudget(budget: BudgetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.insertBudget(budget)
        }
    }
    
    fun updateBudget(budget: BudgetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedBudget = budget.copy(lastUpdated = Date())
            budgetRepository.updateBudget(updatedBudget)
        }
    }

    fun deleteBudgetByCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.deleteBudget(category)
        }
    }
    
    fun setSortOrder(order: BudgetSortOrder) {
        _sortOrder.value = order
    }
    
    fun toggleFilterOverBudget() {
        _filterOverBudget.value = !_filterOverBudget.value
    }
    
    fun updateSavingsGoal(amount: Double) {
        _savingsGoal.doubleValue = amount
    }
    
    fun showEditSavingsGoalDialog() {
        _showEditSavingsGoalDialog.value = true
    }
    
    fun hideEditSavingsGoalDialog() {
        _showEditSavingsGoalDialog.value = false
    }

    // Get monthly analysis with memoization
    fun getMonthlyAnalysis(transactions: List<Transaction>): Map<String, MonthlyAnalysis> {
        return PerformanceUtils.memoize("monthly_analysis_${transactions.size}") {
            FinancialUtils.getMonthlyAnalysis(transactions)
        }
    }

    // Check savings goal progress
    fun checkSavingsGoalProgress(transactions: List<Transaction>): SavingsGoalStatus {
        return BudgetUtils.checkSavingsGoalProgress(transactions, savingsGoal.value)
    }
}

enum class BudgetSortOrder {
    DEFAULT,
    AMOUNT_DESC,
    CATEGORY_ASC,
    PERCENTAGE_USED
}