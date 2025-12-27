package com.zii.school.school

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages user session after successful login
 * Stores login state in SharedPreferences
 */
class SchoolSession private constructor(context: Context) {
    
    companion object {
        private const val TAG = "SchoolSession"
        private const val PREFS_NAME = "school_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_GRADE_ID = "grade_id"
        private const val KEY_GRADE_NAME = "grade_name"
        private const val KEY_STUDENT_IDS = "student_ids"
        
        @Volatile
        private var INSTANCE: SchoolSession? = null
        
        fun getInstance(context: Context): SchoolSession {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SchoolSession(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save login session
     */
    fun saveLogin(loginResult: LoginResult) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, loginResult.userId)
            putString(KEY_USER_NAME, loginResult.name)
            putString(KEY_USER_ROLE, loginResult.role.name)
            putString(KEY_USER_PHONE, loginResult.phone)
            putString(KEY_GRADE_ID, loginResult.gradeId)
            putString(KEY_GRADE_NAME, loginResult.gradeName)
            putString(KEY_STUDENT_IDS, loginResult.studentIds.joinToString(","))
            apply()
        }
        
        Log.i(TAG, "✅ Session saved: ${loginResult.name} (${loginResult.role})")
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get current login result
     */
    fun getLoginResult(): LoginResult? {
        if (!isLoggedIn()) return null
        
        val roleStr = prefs.getString(KEY_USER_ROLE, null) ?: return null
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid role: $roleStr")
            return null
        }
        
        val studentIdsStr = prefs.getString(KEY_STUDENT_IDS, "") ?: ""
        val studentIds = if (studentIdsStr.isNotEmpty()) {
            studentIdsStr.split(",")
        } else {
            emptyList()
        }
        
        return LoginResult(
            role = role,
            userId = prefs.getString(KEY_USER_ID, "") ?: "",
            name = prefs.getString(KEY_USER_NAME, "") ?: "",
            phone = prefs.getString(KEY_USER_PHONE, null),
            gradeId = prefs.getString(KEY_GRADE_ID, null),
            gradeName = prefs.getString(KEY_GRADE_NAME, null),
            studentIds = studentIds
        )
    }
    
    /**
     * Logout and clear session
     */
    fun logout() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Session cleared")
    }
    
    /**
     * Get user role
     */
    fun getUserRole(): UserRole? {
        return getLoginResult()?.role
    }
    
    /**
     * Get user name
     */
    fun getUserName(): String? {
        return getLoginResult()?.name
    }
}
