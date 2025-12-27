package com.zii.school.geohash

/**
 * Minimal stubs for geohash types - BT mesh only
 * These are kept to avoid extensive UI refactoring
 */

sealed class ChannelID {
    object Mesh : ChannelID()
    data class Location(val channel: GeohashChannel) : ChannelID()
}

data class GeohashChannel(
    val level: GeohashChannelLevel,
    val geohash: String
)

enum class GeohashChannelLevel {
    REGION, PROVINCE, CITY, NEIGHBORHOOD, BLOCK
}

data class GeoPerson(
    val pubkeyHex: String,
    val nickname: String
) {
    val displayName: String get() = nickname
}

class GeohashBookmarksStore private constructor() {
    companion object {
        fun getInstance(context: android.content.Context): GeohashBookmarksStore = instance
        private val instance = GeohashBookmarksStore()
    }
    
    val bookmarks = androidx.lifecycle.MutableLiveData<List<String>>(emptyList())
    
    fun toggle(geohash: String) {
        // Stub - BT mesh only
    }
}

object GeohashAliasRegistry {
    fun get(alias: String): String? = null
    fun contains(alias: String): Boolean = false
    fun snapshot(): Map<String, String> = emptyMap()
}

enum class PermissionState {
    GRANTED, DENIED, NOT_REQUESTED
}
