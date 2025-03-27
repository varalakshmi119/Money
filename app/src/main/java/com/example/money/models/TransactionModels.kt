package com.example.money.models

import androidx.room.*
import com.example.money.utils.FinancialUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction entity representing a financial transaction in the database
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val transactionId: String,
    val date: String,
    val time: String,
    val details: String,
    val utrNo: String,
    val accountReference: String,
    val type: String,
    val amount: String,
    val category: String,
    val timestamp: Date
)

/**
 * Data class for transaction DTO (Data Transfer Object)
 * Used for importing transactions from external sources
 */
data class TransactionDTO(
    val transactionId: String,
    val date: String,
    val time: String,
    val details: String,
    val utrNo: String,
    val accountReference: String,
    val type: String,
    val amount: String
) {
    fun toTransaction(): Transaction {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)
        val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        try {
            dateOnlyFormat.parse(date) ?: throw Exception("Invalid date: $date")
        } catch (e: ParseException) {
            throw Exception("Invalid date format for '$date'. Expected format: MMM dd, yyyy (e.g. Nov 18, 2022)", e)
        }
        
        val timestamp = try {
            dateFormat.parse("$date $time")
        } catch (e: ParseException) {
            Date() // Fallback to current date if time format is invalid
        }
        
        return Transaction(
            transactionId = transactionId,
            date = date,
            time = time,
            details = details,
            utrNo = utrNo,
            accountReference = accountReference,
            type = type,
            amount = amount,
            category = FinancialUtils.categorizeTransactionExtended(this),
            timestamp = timestamp
        )
    }
}

/**
 * Response wrapper for transaction imports
 */
data class TransactionsResponse(
    val transactions: List<TransactionDTO>
)

/**
 * Data class for monthly transaction totals
 */
data class MonthlyTotal(
    val month: String,
    val debits: Double,
    val credits: Double
)

/**
 * Data class for monthly financial analysis
 */
data class MonthlyAnalysis(
    val totalDebits: Double,
    val totalCredits: Double,
    val balance: Double,
    val transactionCount: Int,
    val debitCount: Int,
    val creditCount: Int,
    val categoryBreakdown: Map<String, Double>,
    val mostExpensiveCategory: String
)

