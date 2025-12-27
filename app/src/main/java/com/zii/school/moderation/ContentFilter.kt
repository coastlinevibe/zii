package com.zii.school.moderation

import android.util.Log

/**
 * Content moderation system for Zii Chat
 * Simple blacklist + community reporting for location notes and messages
 */
object ContentFilter {
    private const val TAG = "ContentFilter"
    
    // Basic profanity blacklist - expandable
    private val profanityList = setOf(
        // Common offensive words (keeping it basic for now)
        "fuck", "shit", "damn", "bitch", "asshole", "bastard",
        "crap", "piss", "cock", "dick", "pussy", "whore", "slut",
        // Hate speech basics
        "nazi", "hitler", "terrorist", "kill", "murder", "rape",
        // Spam indicators
        "viagra", "casino", "lottery", "winner", "congratulations",
        // Add more as needed...
    )
    
    // Leetspeak and common substitutions
    private val substitutions = mapOf(
        "@" to "a", "3" to "e", "1" to "i", "0" to "o", "5" to "s",
        "7" to "t", "4" to "a", "$" to "s", "!" to "i"
    )
    
    /**
     * Check if content contains inappropriate language
     * Returns FilterResult with details
     */
    fun checkContent(content: String): FilterResult {
        if (content.isBlank()) {
            return FilterResult.CLEAN
        }
        
        val normalizedContent = normalizeText(content)
        
        // Check against blacklist
        val foundWords = profanityList.filter { word ->
            normalizedContent.contains(word, ignoreCase = true)
        }
        
        return if (foundWords.isNotEmpty()) {
            Log.w(TAG, "Content blocked: found ${foundWords.size} inappropriate words")
            FilterResult.BLOCKED(foundWords)
        } else {
            FilterResult.CLEAN
        }
    }
    
    /**
     * Normalize text for checking (handle leetspeak, etc.)
     */
    private fun normalizeText(text: String): String {
        var normalized = text.lowercase()
        
        // Replace common substitutions
        substitutions.forEach { (from, to) ->
            normalized = normalized.replace(from, to)
        }
        
        // Remove non-alphanumeric except spaces
        normalized = normalized.replace(Regex("[^a-z0-9 ]"), "")
        
        return normalized
    }
    
    /**
     * Clean content by replacing inappropriate words with asterisks
     */
    fun cleanContent(content: String): String {
        var cleaned = content
        
        profanityList.forEach { word ->
            val regex = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replace(regex, "*".repeat(word.length))
        }
        
        return cleaned
    }
}

/**
 * Result of content filtering
 */
sealed class FilterResult {
    object CLEAN : FilterResult()
    data class BLOCKED(val foundWords: List<String>) : FilterResult()
    
    val isClean: Boolean get() = this is CLEAN
    val isBlocked: Boolean get() = this is BLOCKED
}