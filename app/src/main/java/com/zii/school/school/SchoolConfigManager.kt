package com.zii.school.school

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Manages embedded school configuration
 * Config is baked into APK during build from admin website
 */
class SchoolConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SchoolConfigManager"
        private const val CONFIG_FILE = "school_config.json"
        
        @Volatile
        private var INSTANCE: SchoolConfigManager? = null
        
        fun getInstance(context: Context): SchoolConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SchoolConfigManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    private var cachedConfig: SchoolConfig? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
    
    /**
     * Load school configuration from embedded JSON file
     * File is generated during APK build by admin website
     */
    fun loadConfig(): SchoolConfig {
        // Return cached config if available
        cachedConfig?.let { return it }
        
        try {
            Log.d(TAG, "Loading school config from assets/$CONFIG_FILE")
            
            // Read embedded school_config.json from assets
            val jsonString = context.assets.open(CONFIG_FILE)
                .bufferedReader()
                .use { it.readText() }
            
            Log.d(TAG, "Config file loaded, size: ${jsonString.length} bytes")
            
            // Parse JSON
            val config = json.decodeFromString<SchoolConfig>(jsonString)
            
            Log.i(TAG, "✅ School config loaded successfully")
            Log.i(TAG, "School: ${config.school.name} (${config.school.code})")
            Log.i(TAG, "Grades: ${config.grades.size}")
            Log.i(TAG, "Teachers: ${config.teachers.size}")
            Log.i(TAG, "Students: ${config.students.size}")
            Log.i(TAG, "Parents: ${config.parents.size}")
            
            // Cache for future use
            cachedConfig = config
            
            return config
            
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to load config file: ${e.message}")
            throw SchoolConfigException("Config file not found. This APK may not be properly configured.", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse config: ${e.message}")
            throw SchoolConfigException("Invalid school configuration format.", e)
        }
    }
    
    /**
     * Validate login credentials against embedded data
     * Returns LoginResult if valid, null otherwise
     */
    fun validateLogin(phoneOrStudentId: String): LoginResult? {
        val config = loadConfig()
        
        Log.d(TAG, "Validating login for: $phoneOrStudentId")
        
        // Check if it's a student ID (format: ABC00001)
        if (phoneOrStudentId.matches(Regex("^[A-Z]{3}\\d+$"))) {
            val student = config.students.find { it.idNumber == phoneOrStudentId }
            if (student != null) {
                val grade = config.grades.find { it.id == student.gradeId }
                Log.i(TAG, "✅ Student login: ${student.name} (${grade?.name})")
                return LoginResult(
                    role = UserRole.STUDENT,
                    userId = student.id,
                    name = student.name,
                    gradeId = student.gradeId,
                    gradeName = grade?.name
                )
            }
        }
        
        // Check if it's a teacher
        val teacher = config.teachers.find { it.phone == phoneOrStudentId }
        if (teacher != null) {
            val grade = teacher.gradeId?.let { gradeId ->
                config.grades.find { it.id == gradeId }
            }
            val role = if (teacher.role == "assistant") UserRole.ASSISTANT else UserRole.TEACHER
            Log.i(TAG, "✅ Teacher login: ${teacher.name} (${grade?.name ?: "All grades"})")
            return LoginResult(
                role = role,
                userId = teacher.id,
                name = teacher.name,
                phone = teacher.phone,
                gradeId = teacher.gradeId,
                gradeName = grade?.name
            )
        }
        
        // Check if it's a parent
        val parent = config.parents.find { it.phone == phoneOrStudentId }
        if (parent != null) {
            // Get children names for display
            val children = config.students.filter { it.id in parent.studentIds }
            val childrenNames = children.joinToString(", ") { it.name }
            Log.i(TAG, "✅ Parent login: ${parent.name} (Children: $childrenNames)")
            return LoginResult(
                role = UserRole.PARENT,
                userId = parent.id,
                name = parent.name,
                phone = parent.phone,
                studentIds = parent.studentIds
            )
        }
        
        Log.w(TAG, "❌ Login failed: No match found for $phoneOrStudentId")
        return null
    }
    
    /**
     * Get grade by ID
     */
    fun getGrade(gradeId: String): Grade? {
        return loadConfig().grades.find { it.id == gradeId }
    }
    
    /**
     * Get all students in a grade
     */
    fun getStudentsInGrade(gradeId: String): List<Student> {
        return loadConfig().students.filter { it.gradeId == gradeId }
    }
    
    /**
     * Get children for a parent
     */
    fun getChildrenForParent(parentId: String): List<Student> {
        val config = loadConfig()
        val parent = config.parents.find { it.id == parentId } ?: return emptyList()
        return config.students.filter { it.id in parent.studentIds }
    }
    
    /**
     * Get teacher's grade
     */
    fun getTeacherGrade(teacherId: String): Grade? {
        val config = loadConfig()
        val teacher = config.teachers.find { it.id == teacherId } ?: return null
        return teacher.gradeId?.let { gradeId ->
            config.grades.find { it.id == gradeId }
        }
    }
    
    /**
     * Check if current time is within school hours
     */
    fun isSchoolHours(): Boolean {
        val config = loadConfig()
        val now = java.time.LocalTime.now()
        val start = java.time.LocalTime.parse(config.timetable.schoolStart)
        val end = java.time.LocalTime.parse(config.timetable.schoolEnd)
        return now.isAfter(start) && now.isBefore(end)
    }
    
    /**
     * Check if current time is within pickup window
     */
    fun isPickupTime(): Boolean {
        val config = loadConfig()
        val now = java.time.LocalTime.now()
        val start = java.time.LocalTime.parse(config.timetable.pickupStart)
        val end = java.time.LocalTime.parse(config.timetable.pickupEnd)
        return now.isAfter(start) && now.isBefore(end)
    }
    
    /**
     * Check if current time is during a break
     */
    fun isBreakTime(): Boolean {
        val config = loadConfig()
        val now = java.time.LocalTime.now()
        return config.timetable.breaks.any { breakPeriod ->
            val start = java.time.LocalTime.parse(breakPeriod.start)
            val end = java.time.LocalTime.parse(breakPeriod.end)
            now.isAfter(start) && now.isBefore(end)
        }
    }
    
    /**
     * Get current school status for display
     */
    fun getCurrentStatus(): String {
        return when {
            isBreakTime() -> "Break Time"
            isSchoolHours() -> "In Class"
            isPickupTime() -> "Pickup Time"
            else -> "After School"
        }
    }
    
    /**
     * Clear cached config (for testing)
     */
    fun clearCache() {
        cachedConfig = null
        Log.d(TAG, "Config cache cleared")
    }
}

/**
 * Exception thrown when school config cannot be loaded
 */
class SchoolConfigException(message: String, cause: Throwable? = null) : Exception(message, cause)
