package com.example.money.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.money.models.MonthlyTotal
import com.example.money.models.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Room database for the financial app
 */
@Database(entities = [Transaction::class, BudgetEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class FinancialDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: FinancialDatabase? = null

        fun getDatabase(context: Context): FinancialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinancialDatabase::class.java,
                    "financial_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String,
    val budgetAmount: Double,
    val period: String = "MONTHLY", // Default to monthly budgets for now
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date(),
    val notes: String = "",
    val isRecurring: Boolean = true,
    val rolloverUnused: Boolean = false,
    val alertThreshold: Double = 0.8 // Alert at 80% by default
)

/**
 * Data Access Object for Budget entity
 */
@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category")
    fun getBudgetByCategory(category: String): Flow<BudgetEntity?>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
    
    @Query("SELECT * FROM budgets ORDER BY budgetAmount DESC")
    fun getBudgetsSortedByAmount(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getBudgetsSortedByCategory(): Flow<List<BudgetEntity>>
}

/**
 * Data Access Object for Transaction entity
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (:type IS NULL OR type = :type) AND (:category IS NULL OR category = :category) ORDER BY timestamp DESC")
    fun getTransactions(type: String? = null, category: String? = null): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end AND (:type IS NULL OR type = :type) AND (:category IS NULL OR category = :category) ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(start: Long, end: Long, type: String? = null, category: String? = null): Flow<List<Transaction>>

    @Query("SELECT DISTINCT category FROM transactions")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT SUM(CAST(amount as DOUBLE)) FROM transactions WHERE type = 'Debit'")
    fun getTotalDebits(): Flow<Double?>

    @Query("SELECT SUM(CAST(amount as DOUBLE)) FROM transactions WHERE type = 'Credit'")
    fun getTotalCredits(): Flow<Double?>

    data class CategoryAmount(
        val category: String,
        val total: Double
    )

    @Query("SELECT category, SUM(CAST(amount as DOUBLE)) as total FROM transactions WHERE type = 'Debit' GROUP BY category ORDER BY total DESC")
    fun getCategoryTotals(): Flow<List<CategoryAmount>>

    @Query("SELECT strftime('%Y-%m', timestamp/1000, 'unixepoch', 'localtime') as month, SUM(CASE WHEN type = 'Debit' THEN CAST(amount as DOUBLE) ELSE 0 END) as debits, SUM(CASE WHEN type = 'Credit' THEN CAST(amount as DOUBLE) ELSE 0 END) as credits FROM transactions GROUP BY month ORDER BY month")
    fun getMonthlyTotals(): Flow<List<MonthlyTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

/**
 * Type converter for Date objects
 */
class DateConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

}

/**
 * Repository class for handling budget data operations
 */
class BudgetRepository(private val budgetDao: BudgetDao) {
    // Get all budgets
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    
    // Get budgets sorted by amount
    fun getBudgetsSortedByAmount(): Flow<List<BudgetEntity>> = budgetDao.getBudgetsSortedByAmount()
    
    // Get budgets sorted by category
    fun getBudgetsSortedByCategory(): Flow<List<BudgetEntity>> = budgetDao.getBudgetsSortedByCategory()

    // Insert budget
    suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insert(budget)

    // Delete budget by category
    suspend fun deleteBudget(category: String) {
        budgetDao.getBudgetByCategory(category).collect { budget ->
            budget?.let { budgetDao.delete(it) }
        }
    }
    
    // Update budget
    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.update(budget)
}