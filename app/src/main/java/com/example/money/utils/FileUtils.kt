package com.example.money.utils

import android.content.Context
import android.net.Uri
import com.example.money.models.TransactionDTO
import com.example.money.models.TransactionsResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for file operations
 */
object FileUtils {
    
    /**
     * Parse transactions from a JSON file using coroutines
     * @param context Application context
     * @param uri URI of the JSON file
     * @return List of parsed TransactionDTO objects or null if parsing failed
     */
    suspend fun parseTransactionsFromJson(context: Context, uri: Uri): List<TransactionDTO>? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()
                inputStream?.close()
                
                val gson = Gson()
                val transactionsType = object : TypeToken<TransactionsResponse>() {}.type
                val response = gson.fromJson<TransactionsResponse>(jsonString, transactionsType)
                response.transactions
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}