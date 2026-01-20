package com.ieum.ieum_back.chat.dto

import com.ieum.ieum_back.entity.ChatMessage
import java.time.LocalDateTime
import java.util.*

/**
 * E2EE 암호화된 메시지 응답 DTO
 */
data class E2EEMessageResponse(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val senderProfileImage: String?,
    val type: String,
    val encryptedContent: String,  // 암호화된 메시지
    val encryptedKey: String,      // 암호화된 세션키
    val iv: String,                // 초기화 벡터
    val imageUrl: String?,
    val isRead: Boolean,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val tempId: String? = null,
    val isEncrypted: Boolean = true
) {
    companion object {
        fun from(message: ChatMessage, tempId: String? = null): E2EEMessageResponse {
            return E2EEMessageResponse(
                id = message.id!!,
                senderId = message.sender.id!!,
                senderName = message.sender.name,
                senderProfileImage = message.sender.profileImage,
                type = message.type.name,
                encryptedContent = message.encryptedContent ?: "",
                encryptedKey = message.encryptedKey ?: "",
                iv = message.iv ?: "",
                imageUrl = message.imageUrl,
                isRead = message.isRead,
                readAt = message.readAt,
                createdAt = message.createdAt,
                tempId = tempId
            )
        }
    }
}
