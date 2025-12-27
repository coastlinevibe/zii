package com.zii.school.messages

import android.content.Context
import android.util.Log
import com.zii.school.model.BitchatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages automatic deletion of messages after a configurable time period.
 * 
 * Benefits:
 * - Frees up storage space continuously
 * - Privacy: messages disappear automatically
 * - Perfect for mesh routing: relay nodes don't store messages forever
 * - Ephemeral messaging like Signal/Snapchat
 */
class MessageAutoDeleteManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MessageAutoDelete"
        private const val PREFS_NAME = "message_auto_delete"
        private const val KEY_DELETE_DELAY_SECONDS = "delete_delay_seconds"
        private const val DEFAULT_DELETE_DELAY_SECONDS = 10 // Changed to 10 seconds default
        
        @Volatile
        private var INSTANCE: MessageAutoDeleteManager? = null
        
        fun getInstance(context: Context): MessageAutoDeleteManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageAutoDeleteManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Track scheduled deletions: messageId -> deletion job
    private val scheduledDeletions = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    
    // Callbacks for comprehensive deletion
    var onDeleteMessage: ((messageId: String) -> Unit)? = null
    var onDeleteNotification: ((messageId: String) -> Unit)? = null
    var onDeleteLocationNote: ((noteId: String) -> Unit)? = null
    var onDeleteSystemMessage: ((messageId: String) -> Unit)? = null
    
    /**
     * Get current auto-delete delay in seconds
     */
    fun getDeleteDelaySeconds(): Int {
        return prefs.getInt(KEY_DELETE_DELAY_SECONDS, DEFAULT_DELETE_DELAY_SECONDS)
    }
    
    /**
     * Set auto-delete delay in seconds
     * Options: 10, 30, 60, 300 (5min), 0 (never)
     */
    fun setDeleteDelaySeconds(seconds: Int) {
        prefs.edit().putInt(KEY_DELETE_DELAY_SECONDS, seconds).apply()
        Log.d(TAG, "Auto-delete delay set to $seconds seconds")
    }
    
    /**
     * Check if auto-delete is enabled
     */
    fun isEnabled(): Boolean {
        return getDeleteDelaySeconds() > 0
    }
    
    /**
     * Schedule a message for auto-deletion
     */
    fun scheduleMessageDeletion(message: BitchatMessage) {
        val delaySeconds = getDeleteDelaySeconds()
        
        if (delaySeconds <= 0) {
            // Auto-delete disabled
            Log.d(TAG, "Auto-delete disabled (delay=$delaySeconds)")
            return
        }
        
        // Cancel any existing deletion for this message
        cancelMessageDeletion(message.id)
        
        Log.d(TAG, "Scheduling deletion for message ${message.id.take(8)}... in $delaySeconds seconds")
        
        // Schedule new deletion
        val job = scope.launch {
            try {
                delay(delaySeconds * 1000L)
                
                Log.d(TAG, "⏰ Auto-deleting message ${message.id.take(8)}... after $delaySeconds seconds")
                onDeleteMessage?.invoke(message.id)
                
                // Also dismiss notification for this message
                onDeleteNotification?.invoke(message.id)
                
                scheduledDeletions.remove(message.id)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-deleting message", e)
            }
        }
        
        scheduledDeletions[message.id] = job
    }
    
    /**
     * Schedule a location note for auto-deletion
     */
    fun scheduleLocationNoteDeletion(noteId: String) {
        val delaySeconds = getDeleteDelaySeconds()
        
        if (delaySeconds <= 0) {
            return
        }
        
        cancelMessageDeletion(noteId)
        
        Log.d(TAG, "Scheduling location note deletion for ${noteId.take(8)}... in $delaySeconds seconds")
        
        val job = scope.launch {
            try {
                delay(delaySeconds * 1000L)
                
                Log.d(TAG, "⏰ Auto-deleting location note ${noteId.take(8)}...")
                onDeleteLocationNote?.invoke(noteId)
                
                scheduledDeletions.remove(noteId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-deleting location note", e)
            }
        }
        
        scheduledDeletions[noteId] = job
    }
    
    /**
     * Schedule system message for auto-deletion
     */
    fun scheduleSystemMessageDeletion(messageId: String) {
        val delaySeconds = getDeleteDelaySeconds()
        
        if (delaySeconds <= 0) {
            return
        }
        
        cancelMessageDeletion(messageId)
        
        Log.d(TAG, "Scheduling system message deletion for ${messageId.take(8)}... in $delaySeconds seconds")
        
        val job = scope.launch {
            try {
                delay(delaySeconds * 1000L)
                
                Log.d(TAG, "⏰ Auto-deleting system message ${messageId.take(8)}...")
                onDeleteSystemMessage?.invoke(messageId)
                
                scheduledDeletions.remove(messageId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-deleting system message", e)
            }
        }
        
        scheduledDeletions[messageId] = job
    }
    
    /**
     * Cancel scheduled deletion for a message
     */
    fun cancelMessageDeletion(messageId: String) {
        scheduledDeletions.remove(messageId)?.cancel()
    }
    
    /**
     * Cancel all scheduled deletions
     */
    fun cancelAllDeletions() {
        scheduledDeletions.values.forEach { it.cancel() }
        scheduledDeletions.clear()
        Log.d(TAG, "Cancelled all scheduled deletions")
    }
    
    /**
     * Get count of messages scheduled for deletion
     */
    fun getScheduledCount(): Int {
        return scheduledDeletions.size
    }
    
    /**
     * Get formatted delay string for UI
     */
    fun getDelayString(): String {
        val seconds = getDeleteDelaySeconds()
        return when {
            seconds == 0 -> "Never"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h"
        }
    }
    
    /**
     * Get available delay options
     */
    fun getDelayOptions(): List<Pair<Int, String>> {
        return listOf(
            0 to "Never",
            10 to "10 seconds",
            30 to "30 seconds",
            60 to "1 minute",
            300 to "5 minutes",
            1800 to "30 minutes",
            3600 to "1 hour"
        )
    }
}
