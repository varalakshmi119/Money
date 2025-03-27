package com.example.money.utils

import com.example.money.models.Transaction
import com.example.money.models.MonthlyAnalysis
import com.example.money.models.TransactionDTO
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Data processing and analysis utilities
object FinancialUtils {
    // Category keywords mapping for auto-categorization
    private val categoryKeywords = mapOf(
        "Food and Dining" to listOf(
            "restaurant", "cafe", "coffee", "pizza", "burger", "food", "dining", 
            "lunch", "dinner", "breakfast", "meal", "takeout", "delivery", "swiggy", "zomato"
        ),
        "Groceries" to listOf(
            "grocery", "supermarket", "market", "fruit", "vegetable", "bread", "milk", 
            "store", "mart", "big basket", "grofers", "dmart", "reliance fresh"
        ),
        "Transportation" to listOf(
            "uber", "ola", "taxi", "cab", "auto", "rickshaw", "metro", "bus", "train", 
            "petrol", "diesel", "fuel", "gas", "parking", "toll", "rapido"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "mall", "shop", "store", "retail", 
            "clothing", "apparel", "fashion", "purchase", "buy"
        ),
        "Entertainment" to listOf(
            "movie", "theatre", "cinema", "concert", "show", "netflix", "prime", "hotstar", 
            "disney", "subscription", "streaming", "music", "spotify", "game"
        ),
        "Bills and Utilities" to listOf(
            "electricity", "water", "gas", "internet", "wifi", "broadband", "phone", "mobile", 
            "bill", "utility", "recharge", "dth", "cable", "tv"
        ),
        "Health and Medical" to listOf(
            "doctor", "hospital", "clinic", "medical", "medicine", "pharmacy", "health", 
            "dental", "eye", "optician", "fitness", "gym", "workout", "apollo", "medplus"
        ),
        "Education" to listOf(
            "school", "college", "university", "tuition", "course", "class", "education", 
            "book", "stationery", "learning", "tutorial", "coaching", "udemy", "coursera"
        ),
        "Travel" to listOf(
            "hotel", "resort", "booking", "flight", "airline", "travel", "trip", "vacation", 
            "holiday", "tour", "makemytrip", "goibibo", "airbnb", "oyo"
        ),
        "Rent" to listOf(
            "rent", "lease", "apartment", "flat", "house", "accommodation", "housing"
        ),
        "Salary" to listOf(
            "salary", "wage", "income", "pay", "payroll", "compensation", "earnings", "deposit"
        ),
        "Investment" to listOf(
            "investment", "stock", "mutual fund", "bond", "dividend", "interest", "return", 
            "zerodha", "upstox", "groww", "etf", "nps", "ppf"
        ),
        "Transfer" to listOf(
            "transfer", "send", "receive", "payment", "upi", "neft", "rtgs", "imps", 
            "gpay", "phonepe", "paytm"
        )
    )
    // Format currency
    fun formatCurrency(amount: Double): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return currencyFormat.format(amount)
    }

    /**
     * Suggest a category based on transaction description
     * @param description The transaction description to analyze
     * @return The suggested category or empty string if no match
     */
    fun suggestCategoryFromDescription(description: String): String {
        val lowerDesc = description.lowercase()
        
        // Check each category's keywords for matches
        categoryKeywords.forEach { (category, keywords) ->
            for (keyword in keywords) {
                if (lowerDesc.contains(keyword.lowercase())) {
                    return category
                }
            }
        }
        
        return "" // No match found
    }

    /**
     * Get weekly spending trends
     * @param transactions List of transactions
     * @return Map of week to total spending
     */
    fun getWeeklyTrends(transactions: List<Transaction>): Map<String, Double> {
        if (transactions.isEmpty()) return emptyMap()
        val weeklySpending = mutableMapOf<String, Double>()
        val debitTransactions = transactions.filter { it.type == "Debit" }
        
        // Get the current week and previous weeks
        val currentCalendar = Calendar.getInstance()
        currentCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)
        
        // Create a map for the last 8 weeks
        for (i in 0 until 8) {
            val weekStart = currentCalendar.time
            currentCalendar.add(Calendar.DATE, -7)
            val weekEnd = currentCalendar.time
            
            val weekLabel = "Week ${8-i}"
            val weekTotal = debitTransactions
                .filter { it.timestamp.after(weekEnd) && (it.timestamp.before(weekStart) || it.timestamp == weekStart) }
                .sumOf { it.amount.toDouble() }
            
            weeklySpending[weekLabel] = weekTotal
        }
        
        // Return only weeks with data, limited to 6 most recent
        return weeklySpending.filterValues { it > 0 }.toList().takeLast(6).toMap()
    }
    
    /**
     * Get monthly spending trends
     * @param transactions List of transactions
     * @return Map of month to total spending
     */
    fun getMonthlyTrends(transactions: List<Transaction>): Map<String, Double> {
        if (transactions.isEmpty()) return emptyMap()
        
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthlySpending = mutableMapOf<String, Double>()
        val debitTransactions = transactions.filter { it.type == "Debit" }
        
        // Get the current month and previous months
        val currentCalendar = Calendar.getInstance()
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)
        
        // Create a map for the last 12 months
        for (i in 0 until 12) {
            val monthStart = currentCalendar.time
            val monthName = monthFormat.format(currentCalendar.time)
            
            currentCalendar.add(Calendar.MONTH, -1)
            val monthEnd = currentCalendar.time
            
            val monthTotal = debitTransactions
                .filter { it.timestamp.after(monthEnd) && (it.timestamp.before(monthStart) || it.timestamp == monthStart) }
                .sumOf { it.amount.toDouble() }
            
            monthlySpending[monthName] = monthTotal
        }
        
        // Return only months with data, limited to 6 most recent
        return monthlySpending.filterValues { it > 0 }.toList().takeLast(6).toMap()
    }
    
    /**
     * Get yearly spending trends
     * @param transactions List of transactions
     * @return Map of year to total spending
     */
    fun getYearlyTrends(transactions: List<Transaction>): Map<String, Double> {
        if (transactions.isEmpty()) return emptyMap()
        
        val calendar = Calendar.getInstance()
        val yearlySpending = mutableMapOf<String, Double>()
        val debitTransactions = transactions.filter { it.type == "Debit" }
        
        // Get the current year and previous years
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Create a map for the last 5 years
        for (i in 0 until 5) {
            val year = currentYear - i
            val yearLabel = year.toString()
            
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val yearStart = calendar.time
            
            calendar.set(Calendar.YEAR, year + 1)
            val yearEnd = calendar.time
            
            val yearTotal = debitTransactions
                .filter { it.timestamp.after(yearStart) || it.timestamp == yearStart && it.timestamp.before(yearEnd) }
                .sumOf { it.amount.toDouble() }
            
            yearlySpending[yearLabel] = yearTotal
        }
        
        // Return only years with data
        return yearlySpending.filterValues { it > 0 }.toList().sortedBy { it.first }.toMap()
    }

    /**
     * Categorize a transaction based on its details
     * @param transaction The transaction to categorize
     * @return The category name
     */
    fun categorizeTransactionExtended(transaction: TransactionDTO): String {
        val details = transaction.details.lowercase()
        
        // Food and Dining
        if (details.contains("restaurant") || details.contains("cafe") || 
            details.contains("food") || details.contains("swiggy") || 
            details.contains("zomato") || details.contains("dining")) {
            return "Food and Dining"
        }
        
        // Travel
        if (details.contains("uber") || details.contains("ola") || 
            details.contains("travel") || details.contains("flight") || 
            details.contains("train") || details.contains("bus") || 
            details.contains("metro") || details.contains("taxi")) {
            return "Travel"
        }
        
        // Online Purchase/Service
        if (details.contains("amazon") || details.contains("flipkart") || 
            details.contains("myntra") || details.contains("ajio") || 
            details.contains("purchase") || details.contains("order") || 
            details.contains("payment") || details.contains("subscription")) {
            return "Online Purchase/Service"
        }
        
        // Entertainment
        if (details.contains("movie") || details.contains("netflix") || 
            details.contains("prime") || details.contains("hotstar") || 
            details.contains("entertainment") || details.contains("game") || 
            details.contains("sport")) {
            return "Entertainment"
        }
        
        // Mobile Recharge
        if (details.contains("recharge") || details.contains("airtel") || 
            details.contains("jio") || details.contains("vodafone") || 
            details.contains("mobile") || details.contains("phone")) {
            return "Mobile Recharge"
        }
        
        // Utilities
        if (details.contains("electricity") || details.contains("water") || 
            details.contains("gas") || details.contains("bill") || 
            details.contains("utility") || details.contains("broadband") || 
            details.contains("internet")) {
            return "Utilities"
        }
        
        // Healthcare
        if (details.contains("hospital") || details.contains("doctor") || 
            details.contains("medical") || details.contains("pharmacy") || 
            details.contains("health") || details.contains("medicine")) {
            return "Healthcare"
        }
        
        // Education
        if (details.contains("school") || details.contains("college") || 
            details.contains("university") || details.contains("course") || 
            details.contains("education") || details.contains("tuition") || 
            details.contains("book")) {
            return "Education"
        }
        
        // Personal Payment
        if (details.contains("transfer") || details.contains("sent") || 
            details.contains("upi") || details.contains("gpay") || 
            details.contains("phonepe") || details.contains("paytm") || 
            details.contains("personal")) {
            return "Personal Payment"
        }
        
        // Building Materials
        if (details.contains("construction") || details.contains("material") || 
            details.contains("hardware") || details.contains("repair") || 
            details.contains("renovation")) {
            return "Building Materials"
        }
        
        // Groceries/Daily Needs
        if (details.contains("grocery") || details.contains("supermarket") || 
            details.contains("mart") || details.contains("store") || 
            details.contains("daily") || details.contains("essentials")) {
            return "Groceries/Daily Needs"
        }
        
        // Laundry
        if (details.contains("laundry") || details.contains("dry clean") || 
            details.contains("wash") || details.contains("iron")) {
            return "Laundry"
        }
        
        // Financial Service/Payment Platform
        if (details.contains("bank") || details.contains("finance") || 
            details.contains("loan") || details.contains("emi") || 
            details.contains("insurance") || details.contains("investment")) {
            return "Financial Service/Payment Platform"
        }
        
        // Government/Regulatory Fee
        if (details.contains("tax") || details.contains("government") || 
            details.contains("fee") || details.contains("fine") || 
            details.contains("penalty") || details.contains("license")) {
            return "Government/Regulatory Fee"
        }
        
        // Default category
        return "Other"
    }
    
    /**
     * Get monthly analysis for transactions
     * @param transactions List of transactions to analyze
     * @return Map of month to MonthlyAnalysis object
     */
    fun getMonthlyAnalysis(transactions: List<Transaction>): Map<String, MonthlyAnalysis> {
        if (transactions.isEmpty()) {
            return emptyMap()
        }
        
        val calendar = Calendar.getInstance()
        val monthlyTransactions = transactions.groupBy {
            calendar.time = it.timestamp
            "${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.YEAR)}"
        }
        
        return monthlyTransactions.mapValues { (_, monthTxns) ->
            // Calculate total debits and credits
            val debits = monthTxns.filter { it.type == "Debit" }
            val credits = monthTxns.filter { it.type == "Credit" }
            val totalDebits = debits.sumOf { it.amount.toDouble() }
            val totalCredits = credits.sumOf { it.amount.toDouble() }
            val balance = totalCredits - totalDebits
            
            // Calculate category breakdown
            val categoryBreakdown = debits
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { it.amount.toDouble() } }
            
            // Find most expensive category
            val mostExpensiveCategory = categoryBreakdown.entries
                .maxByOrNull { it.value }?.key ?: "None"

            MonthlyAnalysis(
                totalDebits = totalDebits,
                totalCredits = totalCredits,
                balance = balance,
                transactionCount = monthTxns.size,
                debitCount = debits.size,
                creditCount = credits.size,
                categoryBreakdown = categoryBreakdown,
                mostExpensiveCategory = mostExpensiveCategory
            )
        }
    }
}