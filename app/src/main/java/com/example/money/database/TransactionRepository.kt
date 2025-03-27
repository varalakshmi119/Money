package com.example.money.database

import com.example.money.models.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository class for Transaction data operations
 */
class TransactionRepository(private val transactionDao: TransactionDao) {
    // Get all transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getTransactions()
    
    // Get total debits
    val totalDebits: Flow<Double?> = transactionDao.getTotalDebits()
    
    // Get total credits
    val totalCredits: Flow<Double?> = transactionDao.getTotalCredits()
    
    // Get all categories
    val categories: Flow<List<String>> = transactionDao.getAllCategories()
    
    // Get category totals
    val categoryTotals: Flow<List<TransactionDao.CategoryAmount>> = transactionDao.getCategoryTotals()
    
    // Get monthly totals
    val monthlyTotals: Flow<List<com.example.money.models.MonthlyTotal>> = transactionDao.getMonthlyTotals()
    
    // Insert a single transaction
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
    
    // Insert multiple transactions
    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }
    
    // Delete all transactions
    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
}
