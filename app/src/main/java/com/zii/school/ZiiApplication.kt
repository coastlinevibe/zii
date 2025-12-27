package com.zii.school

import android.app.Application
import com.zii.school.ui.theme.ThemePreferenceManager
import com.zii.school.net.TorManager

/**
 * Main application class for Zii Chat Android
 * Simplified to BT Mesh only - v2.5.0
 */
class ZiiApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize theme preference
        ThemePreferenceManager.init(this)

        // Initialize debug preference manager
        try { com.zii.school.ui.debug.DebugPreferenceManager.init(this) } catch (_: Exception) { }
    }
}
