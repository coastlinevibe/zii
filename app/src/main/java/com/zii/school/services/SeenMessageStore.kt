package com.zii.school.services

import android.content.Context
import android.util.Log
import com.zii.school.identity.SecureIdentityStateManager
import com.google.gson.Gson

/**
 * Persistent store for message IDs we've already acknowledged (DELIVERED) or READ.
 * Limits to last MAX_IDS entries per set to avoid memory bloat.
 */
class SeenMessageStore private constructor(private val context: Context) {
    companion object {
        private const val TAG = "SeenMessageStore"
        private const val STORAGE_KEY = "seen_message_store_v1"
        private const val MAX_IDS = com.zii.school.util.AppConstants.Services.SEEN_MESSAGE_MAX_IDS
        private const val AUTO_CLEANUP_INTERVAL_MS = 3600000L // 1 hour

        @Volatile private var INSTANCE: SeenMessageStore? = null
        fun getInstance(appContext: Context): SeenMessageStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeenMessageStore(appContext.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = Gson()
    private val secure = SecureIdentityStateManager(context)

    private val delivered = LinkedHashSet<String>(MAX_IDS)
    private val read = LinkedHashSet<String>(MAX_IDS)
    private var lastCleanupTime = System.currentTimeMillis()

    init { 
        load()
        schedulePeriodicCleanup()
    }

    @Synchronized fun hasDelivered(id: String) = delivered.contains(id)
    @Synchronized fun hasRead(id: String) = read.contains(id)

    @Synchronized fun markDelivered(id: String) {
        if (delivered.remove(id)) delivered.add(id) else {
            delivered.add(id)
            trim(delivered)
        }
        persist()
        checkAutoCleanup()
    }

    @Synchronized fun markRead(id: String) {
        if (read.remove(id)) read.add(id) else {
            read.add(id)
            trim(read)
        }
        persist()
        checkAutoCleanup()
    }
    
    /**
     * Check if auto-cleanup should run (respects auto-delete settings)
     */
    private fun checkAutoCleanup() {
        val now = System.currentTimeMillis()
        if (now - lastCleanupTime > AUTO_CLEANUP_INTERVAL_MS) {
            performAutoCleanup()
            lastCleanupTime = now
        }
    }
    
    /**
     * Perform automatic cleanup of old entries
     */
    private fun performAutoCleanup() {
        // Aggressively trim to 50% of max to reduce memory pressure
        val targetSize = MAX_IDS / 2
        
        if (delivered.size > targetSize) {
            val toRemove = delivered.size - targetSize
            val it = delivered.iterator()
            var removed = 0
            while (it.hasNext() && removed < toRemove) {
                it.next()
                it.remove()
                removed++
            }
            Log.d(TAG, "Auto-cleanup: trimmed delivered set to ${delivered.size} entries")
        }
        
        if (read.size > targetSize) {
            val toRemove = read.size - targetSize
            val it = read.iterator()
            var removed = 0
            while (it.hasNext() && removed < toRemove) {
                it.next()
                it.remove()
                removed++
            }
            Log.d(TAG, "Auto-cleanup: trimmed read set to ${read.size} entries")
        }
        
        persist()
    }
    
    /**
     * Schedule periodic cleanup (called from init)
     */
    private fun schedulePeriodicCleanup() {
        // Cleanup runs automatically when markDelivered/markRead are called
        // This is a lightweight check, no separate thread needed
    }

    private fun trim(set: LinkedHashSet<String>) {
        if (set.size <= MAX_IDS) return
        val it = set.iterator()
        while (set.size > MAX_IDS && it.hasNext()) {
            it.next(); it.remove()
        }
    }

    @Synchronized private fun load() {
        try {
            val json = secure.getSecureValue(STORAGE_KEY) ?: return
            val data = gson.fromJson(json, StorePayload::class.java) ?: return
            delivered.clear(); read.clear()
            data.delivered.takeLast(MAX_IDS).forEach { delivered.add(it) }
            data.read.takeLast(MAX_IDS).forEach { read.add(it) }
            Log.d(TAG, "Loaded delivered=${delivered.size}, read=${read.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SeenMessageStore: ${e.message}")
        }
    }

    @Synchronized private fun persist() {
        try {
            val payload = StorePayload(delivered.toList(), read.toList())
            val json = gson.toJson(payload)
            secure.storeSecureValue(STORAGE_KEY, json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist SeenMessageStore: ${e.message}")
        }
    }

    private data class StorePayload(
        val delivered: List<String> = emptyList(),
        val read: List<String> = emptyList()
    )
}
