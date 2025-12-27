package com.zii.school.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.zii.school.geohash.GeoPerson
import com.zii.school.geohash.ChannelID
import com.zii.school.geohash.GeohashChannel

/**
 * Geohash ViewModel Stub - BT Mesh Only
 * Minimal stub to avoid extensive refactoring
 */
class GeohashViewModel(
    application: Application,
    private val state: ChatState,
    private val meshService: com.zii.school.mesh.BluetoothMeshService
) : AndroidViewModel(application) {
    
    val geohashPeople = MutableLiveData<List<GeoPerson>>(emptyList())
    
    fun initialize() {
        // Stub - BT mesh only
    }
    
    fun sendGeohashMessage(content: String, channel: GeohashChannel, myPeerID: String, nickname: String?) {
        // Stub - BT mesh only
    }
    
    fun deleteLocationNote(noteId: String) {
        // Stub - BT mesh only
    }
    
    fun geohashParticipantCount(geohash: String): Int = 0
    
    fun beginGeohashSampling(geohashes: List<String>) {
        // Stub - BT mesh only
    }
    
    fun isPersonTeleported(pubkeyHex: String): Boolean = false
    
    fun startGeohashDM(pubkeyHex: String, callback: (String) -> Unit) {
        // Stub - BT mesh only
    }
    
    fun selectLocationChannel(channel: ChannelID) {
        // Stub - BT mesh only
    }
    
    fun blockUserInGeohash(targetNickname: String) {
        // Stub - BT mesh only
    }
    
    fun colorForNostrPubkey(pubkeyHex: String, isDark: Boolean): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color.Gray
    }
    
    fun panicReset() {
        // Stub - BT mesh only
    }
}
