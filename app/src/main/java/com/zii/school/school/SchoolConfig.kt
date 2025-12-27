package com.zii.school.school

import kotlinx.serialization.Serializable

/**
 * School configuration embedded in APK
 * Generated from admin website during build
 */
@Serializable
data class SchoolConfig(
    val school: School,
    val grades: List<Grade>,
    val teachers: List<Teacher>,
    val students: List<Student>,
    val parents: List<Parent>,
    val timetable: Timetable
)

@Serializable
data class School(
    val id: String,
    val code: String,
    val name: String,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val address: String? = null,
    val logoUrl: String? = null,
    val primaryColor: String? = null
)

@Serializable
data class Grade(
    val id: String,
    val name: String,
    val encryptionKey: String // Base64 encoded key for grade-level encryption
)

@Serializable
data class Teacher(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val gradeId: String? = null,
    val role: String = "teacher" // "teacher" or "assistant"
)

@Serializable
data class Student(
    val id: String,
    val name: String,
    val idNumber: String, // e.g., "ABC00001"
    val gradeId: String
)

@Serializable
data class Parent(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val studentIds: List<String> // Can have multiple children
)

@Serializable
data class Timetable(
    val schoolStart: String, // e.g., "08:00"
    val schoolEnd: String,   // e.g., "14:00"
    val pickupStart: String, // e.g., "14:00"
    val pickupEnd: String,   // e.g., "15:00"
    val breaks: List<Break> = emptyList()
)

@Serializable
data class Break(
    val name: String,
    val start: String,
    val end: String
)

/**
 * User role in the school system
 */
enum class UserRole {
    TEACHER,
    ASSISTANT,
    PARENT,
    STUDENT
}

/**
 * Login result after validation
 */
data class LoginResult(
    val role: UserRole,
    val userId: String,
    val name: String,
    val phone: String? = null,
    val gradeId: String? = null,
    val gradeName: String? = null,
    val studentIds: List<String> = emptyList()
)
