package com.zii.school.services

import android.content.Context
import com.zii.school.mesh.BluetoothMeshService
import com.zii.school.model.ReadReceipt

/**
 * Message Router - Simplified for BT Mesh only
 * Routes all messages through Bluetooth mesh
 */
class MessageRouter(
    private val context: Context,
    private val mesh: BluetoothMeshService
) {
    
    companion object {
        @Volatile
        private var instance: MessageRouter? = null
        
        fun tryGetInstance(): MessageRouter? = instance
        
        fun initialize(context: Context, mesh: BluetoothMeshService): MessageRouter {
            return instance ?: synchronized(this) {
                instance ?: MessageRouter(context, mesh).also { instance = it }
            }
        }
    }
    
    fun onPeersUpdated(peers: List<String>) {
        // BT mesh only - no routing needed
    }
    
    fun sendMessage(content: String, channel: String?, recipientPeerID: String?, messageID: String) {
        // All messages go through BT mesh
        if (recipientPeerID != null) {
            // Private message
            mesh.sendPrivateMessage(content, recipientPeerID, "", messageID)
        } else if (channel != null) {
            // Channel message - use sendMessage with channel prefix
            mesh.sendMessage("$channel: $content")
        } else {
            // Public mesh message
            mesh.sendMessage(content)
        }
    }
    
    fun sendReadReceipt(receipt: ReadReceipt) {
        // Send via BT mesh
        mesh.sendReadReceipt(receipt.originalMessageID, receipt.readerPeerID ?: "", "")
    }
    
    fun sendDeliveryAck(messageID: String, recipientPeerID: String, senderNickname: String) {
        // Send via BT mesh - use sendReadReceipt as delivery ack
        mesh.sendReadReceipt(messageID, recipientPeerID, senderNickname)
    }
    
    fun sendFavoriteNotification(peerID: String, isFavorite: Boolean) {
        // BT mesh only - no favorite notifications needed
    }
}
