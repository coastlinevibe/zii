package com.zii.school.net

import android.content.Context

/**
 * Tor Manager - Stub for BT Mesh only
 * Tor functionality removed in simplification
 */
class TorManager(private val context: Context) {
    
    companion object {
        fun currentSocksAddress(): java.net.InetSocketAddress? = null
        
        val statusFlow = kotlinx.coroutines.flow.MutableStateFlow(TorStatus())
    }
    
    data class TorStatus(
        val running: Boolean = false,
        val bootstrapPercent: Int = 0,
        val lastLogLine: String = ""
    )
    
    fun start() {
        // Tor removed - BT mesh only
    }
    
    fun stop() {
        // Tor removed - BT mesh only
    }
    
    fun isRunning(): Boolean = false
    
    fun getSocksPort(): Int = 0
}
