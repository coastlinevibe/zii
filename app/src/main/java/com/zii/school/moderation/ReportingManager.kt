package com.zii.school.moderation

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.util.*

/**
 * Community reporting system for inappropriate content
 * Allows users to report offensive location notes or messages
 */
class ReportingManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ReportingManager"
        private const val MAX_REPORTS_PER_USER = 10 // Prevent spam reporting
        
        @Volatile
        private var INSTANCE: ReportingManager? = null
        
        fun getInstance(context: Context): ReportingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReportingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    data class Report(
        val id: String,
        val contentId: String, // Note ID or message ID
        val contentType: ContentType,
        val reason: ReportReason,
        val reporterPubkey: String,
        val timestamp: Long,
        val content: String // Copy of reported content
    )
    
    enum class ContentType {
        LOCATION_NOTE,
        CHAT_MESSAGE
    }
    
    enum class ReportReason {
        PROFANITY,
        HARASSMENT,
        SPAM,
        HATE_SPEECH,
        INAPPROPRIATE,
        OTHER
    }
    
    private val _reports = MutableLiveData<List<Report>>(emptyList())
    val reports: LiveData<List<Report>> = _reports
    
    private val reportCounts = mutableMapOf<String, Int>() // contentId -> report count
    private val userReportCounts = mutableMapOf<String, Int>() // userPubkey -> report count
    
    /**
     * Report inappropriate content
     */
    fun reportContent(
        contentId: String,
        contentType: ContentType,
        reason: ReportReason,
        reporterPubkey: String,
        content: String
    ): Boolean {
        
        // Check if user has exceeded report limit
        val userReports = userReportCounts[reporterPubkey] ?: 0
        if (userReports >= MAX_REPORTS_PER_USER) {
            Log.w(TAG, "User $reporterPubkey has exceeded report limit")
            return false
        }
        
        // Check if user already reported this content
        val existingReport = _reports.value?.find { 
            it.contentId == contentId && it.reporterPubkey == reporterPubkey 
        }
        if (existingReport != null) {
            Log.w(TAG, "User already reported this content")
            return false
        }
        
        // Create report
        val report = Report(
            id = UUID.randomUUID().toString(),
            contentId = contentId,
            contentType = contentType,
            reason = reason,
            reporterPubkey = reporterPubkey,
            timestamp = System.currentTimeMillis(),
            content = content
        )
        
        // Add to reports
        val currentReports = _reports.value?.toMutableList() ?: mutableListOf()
        currentReports.add(report)
        _reports.postValue(currentReports)
        
        // Update counters
        reportCounts[contentId] = (reportCounts[contentId] ?: 0) + 1
        userReportCounts[reporterPubkey] = userReports + 1
        
        Log.i(TAG, "Content reported: $contentId (${reportCounts[contentId]} total reports)")
        
        // Auto-hide content if it gets multiple reports
        if (getReportCount(contentId) >= 3) {
            Log.w(TAG, "Content $contentId auto-hidden due to multiple reports")
            // TODO: Implement auto-hide mechanism
        }
        
        return true
    }
    
    /**
     * Get number of reports for specific content
     */
    fun getReportCount(contentId: String): Int {
        return reportCounts[contentId] ?: 0
    }
    
    /**
     * Check if content should be hidden due to reports
     */
    fun isContentHidden(contentId: String): Boolean {
        return getReportCount(contentId) >= 3
    }
    
    /**
     * Get reports for specific content (for moderation)
     */
    fun getReportsForContent(contentId: String): List<Report> {
        return _reports.value?.filter { it.contentId == contentId } ?: emptyList()
    }
    
    /**
     * Clear old reports (cleanup)
     */
    fun cleanupOldReports() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val currentReports = _reports.value?.toMutableList() ?: mutableListOf()
        
        val cleaned = currentReports.filter { it.timestamp > oneWeekAgo }
        if (cleaned.size != currentReports.size) {
            _reports.postValue(cleaned)
            Log.d(TAG, "Cleaned up ${currentReports.size - cleaned.size} old reports")
        }
    }
}