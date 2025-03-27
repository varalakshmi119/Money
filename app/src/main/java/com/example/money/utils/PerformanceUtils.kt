package com.example.money.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class for performance optimizations in the Money app.
 * Provides caching and memoization capabilities to reduce redundant calculations.
 */
object PerformanceUtils {
    // Cache for expensive calculations
    private val calculationCache = ConcurrentHashMap<String, Any>()
    
    // Cache timeout in milliseconds (60 minutes)
    private const val CACHE_TIMEOUT = 60 * 60 * 1000
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    /**
     * Generic function to cache results of expensive calculations
     * @param key Unique identifier for the calculation
     * @param calculation Function that performs the expensive calculation
     * @return The cached result or the result of the calculation if not cached
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> memoize(key: String, calculation: () -> T): T {
        val currentTime = System.currentTimeMillis()
        val timestamp = cacheTimestamps[key] ?: 0
        
        // Check if cache is valid
        if (calculationCache.containsKey(key) && currentTime - timestamp < CACHE_TIMEOUT) {
            return calculationCache[key] as T
        }
        
        // Perform calculation and cache result
        val result = calculation()
        calculationCache[key] = result as Any
        cacheTimestamps[key] = currentTime
        return result
    }

}