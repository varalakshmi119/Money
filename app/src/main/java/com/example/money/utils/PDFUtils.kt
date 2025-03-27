package com.example.money.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.money.models.TransactionDTO
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

private const val TAG = "PDFUtils"

object PDFUtils {
    private val DATE_PATTERN = Pattern.compile(
        "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}$",
        Pattern.CASE_INSENSITIVE
    )

    private val TIME_PATTERN = Pattern.compile(
        "^\\d{1,2}:\\d{2}\\s+(?:AM|PM)$",
        Pattern.CASE_INSENSITIVE
    )

    private val INR_PATTERN = Pattern.compile("\\b(?:INR|₹)\\s+[\\d,]+\\.\\d{2}\\b", Pattern.CASE_INSENSITIVE)
    private val AMOUNT_PATTERN = Pattern.compile("\\b(?:INR|₹)\\s+([\\d,]+\\.\\d{2})\\b", Pattern.CASE_INSENSITIVE)
    private val NUMBER_PATTERN = Pattern.compile("^[\\d,]+\\.?\\d{0,2}$")
    private val TRANSACTION_ID_PATTERN = Pattern.compile("Transaction ID\\s*:\\s*([\\w\\d]+)", Pattern.CASE_INSENSITIVE)
    private val UTR_PATTERN = Pattern.compile("UTR No\\s*:\\s*([\\w\\d]+)", Pattern.CASE_INSENSITIVE)
    private val ACCOUNT_REFERENCE_PATTERN = Pattern.compile("(Debited from|Credited to)\\s+([\\w\\d]+)", Pattern.CASE_INSENSITIVE)
    private val VALID_STATEMENT_KEYWORDS = listOf(
        "Transaction Statement",
        "Transaction Details",
        "Transaction ID",
        "UTR No"
    )

    fun initializePdfBox(context: Context) {
        PDFBoxResourceLoader.init(context)
        try {
            Class.forName("com.tom_roush.pdfbox.filter.JPXFilter")
        } catch (e: Exception) {
            Log.w(TAG, "JPXFilter not available")
        }
    }

    suspend fun parsePhonePePDF(
        context: Context,
        uri: Uri,
        password: String
    ): List<TransactionDTO>? = withContext(Dispatchers.IO) {
        try {
            initializePdfBox(context)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val document = if (password.isNotEmpty()) {
                    PDDocument.load(stream, password)
                } else {
                    PDDocument.load(stream)
                }
                
                disableImageLoading(document)
                
                val textStripper = PDFTextStripper()
                val text = textStripper.getText(document)
                document.close()

                if (!isValidPhonePeStatement(text)) {
                    throw IllegalArgumentException("Invalid PhonePe statement")
                }

                Log.d(TAG, "PDF text: ${text.take(1000)}...")
                val result = processContent(text)
                Log.d(TAG, "Parsed ${result.transactions.size} transactions")
                result.transactions
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF parsing failed", e)
            null
        }
    }

    private fun disableImageLoading(document: PDDocument) {
        try {
            document.javaClass.getDeclaredField("resources").apply {
                isAccessible = true
                set(document, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to disable image loading", e)
        }
    }

    private fun isValidPhonePeStatement(content: String): Boolean {
        return VALID_STATEMENT_KEYWORDS.any { keyword ->
            content.contains(keyword, ignoreCase = true)
        }
    }

    private fun processContent(content: String): TransactionsResponse {
        val lines = content.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        Log.d(TAG, "Processing ${lines.size} lines")
        
        val transactions = mutableListOf<TransactionDTO>()
        var currentTransaction: MutableMap<String, String>? = null
        
        // Debug - print first few lines
        lines.take(20).forEachIndexed { index, line ->
            Log.d(TAG, "Line $index: $line")
        }
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            // Match date pattern like "Mar 08, 2025"
            if (DATE_PATTERN.matcher(line).matches()) {
                // If we find a time on the next line, this is part of a transaction
                if (i + 1 < lines.size && TIME_PATTERN.matcher(lines[i + 1]).matches()) {
                    // Save the previous transaction if exists
                    currentTransaction?.let {
                        // Only add if we have details and possibly an amount
                        if (it.containsKey("details") && (it.containsKey("amount") || it.containsKey("type"))) {
                            transactions.add(validateTransaction(it))
                            Log.d(TAG, "Added transaction: ${it["details"]} - ${it["amount"]}")
                        }
                    }
                    
                    // Create new transaction with date and time
                    currentTransaction = mutableMapOf(
                        "date" to line.trim(),
                        "time" to lines[i + 1].trim()
                    )
                    
                    // Skip the time line since we've processed it
                    i += 2
                    
                    // Process remaining transaction lines
                    while (i < lines.size) {
                        val nextLine = lines[i]
                        
                        // Check if we've reached the next date (potential new transaction)
                        if (DATE_PATTERN.matcher(nextLine).matches()) {
                            break
                        }
                        
                        // Process the current line
                        processTransactionLine(nextLine, currentTransaction)
                        i++
                    }
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        
        // Add the last transaction if it exists
        currentTransaction?.let {
            if (it.containsKey("details") && (it.containsKey("amount") || it.containsKey("type"))) {
                transactions.add(validateTransaction(it))
                Log.d(TAG, "Added last transaction: ${it["details"]} - ${it["amount"]}")
            }
        }

        return TransactionsResponse(transactions)
    }

    private fun processTransactionLine(line: String, transaction: MutableMap<String, String>) {
        when {
            // Transaction detail line like "Paid to Nandini milk parlour" or "Received from M K BUILDING MATERIALS"
            isTransactionDetail(line) -> {
                transaction["details"] = line.trim()
                transaction["type"] = getTransactionType(line)
                Log.d(TAG, "Found transaction details: ${line.trim()} - Type: ${getTransactionType(line)}")
            }
            // Transaction ID line like "Transaction ID : T25030814064432917711"
            TRANSACTION_ID_PATTERN.matcher(line).find() -> {
                val matcher = TRANSACTION_ID_PATTERN.matcher(line)
                if (matcher.find()) {
                    transaction["transactionId"] = matcher.group(1).trim()
                    Log.d(TAG, "Found transaction ID: ${matcher.group(1).trim()}")
                }
            }
            // UTR line like "UTR No : 841302199001" 
            UTR_PATTERN.matcher(line).find() -> {
                val matcher = UTR_PATTERN.matcher(line)
                if (matcher.find()) {
                    transaction["utrNo"] = matcher.group(1).trim()
                    Log.d(TAG, "Found UTR No: ${matcher.group(1).trim()}")
                }
            }
            // Account reference line like "Debited from XX5779" or "Credited to XX5779"
            ACCOUNT_REFERENCE_PATTERN.matcher(line).find() -> {
                transaction["accountReference"] = line.replaceMultipleSpaces()
                Log.d(TAG, "Found account reference: ${line.replaceMultipleSpaces()}")
            }
            // Amount line like "INR 14.00" or "INR 8000.00"
            INR_PATTERN.matcher(line).find() -> {
                transaction["amount"] = extractAmount(line)
                Log.d(TAG, "Found amount: ${extractAmount(line)} from line: $line")
                
                // Set type based on page headers if not already set
                if (transaction["type"].isNullOrEmpty()) {
                    if (line.contains("Credit", ignoreCase = true) || 
                        line.contains("Received", ignoreCase = true)) {
                        transaction["type"] = "Credit"
                    } else if (line.contains("Debit", ignoreCase = true) || 
                              line.contains("Paid", ignoreCase = true)) {
                        transaction["type"] = "Debit"
                    }
                }
            }
            // Type information (Debit/Credit column)
            line.equals("Debit", ignoreCase = true) || line.equals("Credit", ignoreCase = true) -> {
                if (transaction["type"].isNullOrEmpty()) {
                    transaction["type"] = line.trim()
                    Log.d(TAG, "Found transaction type from column: ${line.trim()}")
                }
            }
            // Check if this is a column header line
            line.contains("Date") && line.contains("Transaction Details", ignoreCase = true) -> {
                Log.d(TAG, "Found header line: $line")
            }
            // Any other line that might contain useful information
            else -> {
                // Log for debugging
                if (line.length > 5) {
                    Log.d(TAG, "Unprocessed line: $line")
                }
            }
        }
    }

    private fun isTransactionDetail(line: String): Boolean {
        val lowerLine = line.lowercase()
        return lowerLine.startsWith("paid to") ||
               lowerLine.startsWith("received from") ||
               lowerLine.startsWith("paid -")
    }

    private fun getTransactionType(line: String): String {
        val lowerLine = line.lowercase()
        return when {
            lowerLine.startsWith("received from") -> "Credit"
            lowerLine.startsWith("paid to") || lowerLine.startsWith("paid -") -> "Debit"
            else -> ""
        }
    }

    private fun String.replaceMultipleSpaces(): String {
        return this.replace("\\s+".toRegex(), " ").trim()
    }

    private fun extractAmount(line: String): String {
        val matcher = AMOUNT_PATTERN.matcher(line)
        if (matcher.find()) {
            val amountStr = matcher.group(1).replace(",", "")
            try {
                // Format to ensure exactly 2 decimal places
                val amount = amountStr.toDouble()
                return "%.2f".format(amount)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse amount: $amountStr", e)
                return "0.00"
            }
        }
        return "0.00"
    }

    private fun validateTransaction(transaction: Map<String, String>): TransactionDTO {
        return TransactionDTO(
            transactionId = transaction["transactionId"] ?: "",
            date = transaction["date"]?.trim() ?: "",
            time = transaction["time"]?.trim() ?: "",
            details = transaction["details"] ?: "",
            utrNo = transaction["utrNo"] ?: "",
            accountReference = transaction["accountReference"] ?: "",
            type = transaction["type"] ?: "",
            amount = transaction["amount"] ?: "0.00"
        )
    }
}

data class TransactionsResponse(val transactions: List<TransactionDTO>)