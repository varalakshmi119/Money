package com.example.money.utils

import android.content.Context
import android.net.Uri
import com.example.money.models.TransactionDTO
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Utility class for PDF parsing operations
 */
object PDFUtils {
    private const val LOOK_AHEAD_LINES = 10
    private val DATE_PATTERN = Pattern.compile("^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}$", Pattern.CASE_INSENSITIVE)
    private val TIME_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}\\s+(?:AM|PM)$", Pattern.CASE_INSENSITIVE)
    private val INR_PATTERN = Pattern.compile("(?:INR|₹)", Pattern.CASE_INSENSITIVE)
    private val NUMBER_PATTERN = Pattern.compile("^[\\d,]+\\.?\\d{0,2}$")

    /**
     * Initialize PDFBox library if needed
     */
    fun initializePdfBox(context: Context) {
        PDFBoxResourceLoader.init(context)
        
        // Handle JPX filter issue
        try {
            // This forces class initialization to avoid JPX filter error
            // We're not actually using this class directly, but pre-loading it helps
            Class.forName("com.tom_roush.pdfbox.filter.JPXFilter")
        } catch (e: Exception) {
            // Ignore any errors, we just want to make sure the class is loaded
        }
    }

    /**
     * Parse PhonePe PDF statement into transactions using coroutines
     * @param context Application context
     * @param uri URI of the PDF file
     * @param password Password for the PDF (typically phone number)
     * @return List of parsed TransactionDTO objects or null if parsing failed
     */
    suspend fun parsePhonePePDF(context: Context, uri: Uri, password: String): List<TransactionDTO>? {
        return withContext(Dispatchers.IO) {
            try {
                initializePdfBox(context)
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val document = if (password.isNotEmpty()) {
                        PDDocument.load(stream, password)
                    } else {
                        PDDocument.load(stream)
                    }
                    
                    document.use { doc ->
                        // Skip loading images to avoid JPX filter issues
                        disableImageLoading(doc)
                        
                        val textStripper = PDFTextStripper()
                        val text = textStripper.getText(doc)
                        
                        if (!isValidPhonePeStatement(text)) {
                            throw Exception("The uploaded file does not appear to be a valid PhonePe statement.")
                        }
                        
                        processContent(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Disable image loading in the PDF to avoid JPX filter issues
     */
    private fun disableImageLoading(document: PDDocument) {
        // This is a hacky way to prevent JPX filter errors, but it works
        // We're only interested in text content anyway
        try {
            val resources = document.javaClass.getDeclaredField("resources")
            resources.isAccessible = true
            resources.set(document, null)
        } catch (e: Exception) {
            // Ignore if we can't disable image loading
            // The app will still work, just might have issues with some PDFs
        }
    }
    
    /**
     * Check if the PDF content is a valid PhonePe statement
     */
    private fun isValidPhonePeStatement(content: String): Boolean {
        val contentLowerCase = content.lowercase()
        return listOf("Transaction Statement", "Transaction ID", "UTR No")
            .any { contentLowerCase.contains(it.lowercase()) }
    }
    
    /**
     * Process the PDF content and extract transactions
     */
    private fun processContent(content: String): List<TransactionDTO> {
        val lines = content.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            
        val transactions = mutableListOf<TransactionDTO>()
        var currentTransaction: MutableMap<String, String>? = null
        
        for (i in lines.indices) {
            val line = lines[i]
            
            if (DATE_PATTERN.matcher(line).matches()) {
                currentTransaction?.let {
                    if (it.containsKey("date") && it.containsKey("time")) {
                        transactions.add(createTransaction(it))
                    }
                }
                
                currentTransaction = mutableMapOf("date" to line.trim())
                processTransactionDetails(lines, i, currentTransaction)
            }
        }
        
        currentTransaction?.let {
            if (it.containsKey("date") && it.containsKey("time")) {
                transactions.add(createTransaction(it))
            }
        }
        
        return transactions
    }
    
    /**
     * Process transaction details from the lines
     */
    private fun processTransactionDetails(lines: List<String>, startIndex: Int, transaction: MutableMap<String, String>) {
        for (j in startIndex + 1 until Math.min(startIndex + LOOK_AHEAD_LINES, lines.size)) {
            val line = lines[j]
            
            when {
                TIME_PATTERN.matcher(line).matches() -> {
                    transaction["time"] = line.trim()
                }
                isTransactionDetail(line) -> {
                    transaction["details"] = line.trim()
                    transaction["type"] = getTransactionType(line)
                }
                line.contains("Transaction ID", ignoreCase = true) -> {
                    val transactionIdMatch = "Transaction ID[:\\s]*([^\\s]+)".toRegex(RegexOption.IGNORE_CASE).find(line)
                    if (transactionIdMatch != null) {
                        transaction["transactionId"] = transactionIdMatch.groupValues[1].trim()
                    }
                }
                line.contains("UTR No", ignoreCase = true) -> {
                    val utrNoMatch = "UTR No[.:\\s]*([^\\s]+)".toRegex(RegexOption.IGNORE_CASE).find(line)
                    if (utrNoMatch != null) {
                        transaction["utrNo"] = utrNoMatch.groupValues[1].trim()
                    }
                }
                isAccountReference(line) -> {
                    transaction["accountReference"] = line.replace("\\s+".toRegex(), " ").trim()
                }
                INR_PATTERN.matcher(line).find() -> {
                    transaction["amount"] = extractAmount(lines, j)
                }
            }
        }
    }
    
    /**
     * Check if the line is a transaction detail
     */
    private fun isTransactionDetail(line: String): Boolean {
        val lowerLine = line.lowercase()
        return lowerLine.startsWith("paid to") || 
               lowerLine.startsWith("received from") || 
               lowerLine.startsWith("paid -")
    }
    
    /**
     * Get transaction type from the detail line
     */
    private fun getTransactionType(line: String): String {
        val lowerLine = line.lowercase()
        return when {
            lowerLine.startsWith("received from") -> "Credit"
            lowerLine.startsWith("paid to") || lowerLine.startsWith("paid -") -> "Debit"
            else -> ""
        }
    }
    
    /**
     * Check if the line is an account reference
     */
    private fun isAccountReference(line: String): Boolean {
        val lowerLine = line.lowercase()
        return lowerLine.contains("debited from") || 
               lowerLine.contains("credited to") || 
               lowerLine.contains("paid by")
    }
    
    /**
     * Extract amount from transaction lines
     */
    private fun extractAmount(lines: List<String>, currentIndex: Int): String {
        val currentLine = lines[currentIndex]
        val nextLine = if (currentIndex + 1 < lines.size) lines[currentIndex + 1] else ""
        
        val normalMatch = "(?:INR|₹)\\s*([\\d,]+\\.?\\d{0,2})".toRegex(RegexOption.IGNORE_CASE).find(currentLine)
        if (normalMatch != null) {
            val cleanAmount = normalMatch.groupValues[1].replace(",", "")
            return cleanAmount
        }
        
        if (INR_PATTERN.matcher(currentLine).find() && 
            nextLine.isNotEmpty() && 
            NUMBER_PATTERN.matcher(nextLine.trim()).matches()) {
            val cleanAmount = nextLine.trim().replace(",", "")
            return cleanAmount
        }
        
        return "0.00"
    }
    
    /**
     * Create a TransactionDTO from the parsed data
     */
    private fun createTransaction(data: Map<String, String>): TransactionDTO {
        return TransactionDTO(
            transactionId = data["transactionId"] ?: "",
            date = data["date"] ?: "",
            time = data["time"] ?: "",
            details = data["details"] ?: "",
            utrNo = data["utrNo"] ?: "",
            accountReference = data["accountReference"] ?: "",
            type = data["type"] ?: "",
            amount = data["amount"] ?: "0.00"
        )
    }
} 