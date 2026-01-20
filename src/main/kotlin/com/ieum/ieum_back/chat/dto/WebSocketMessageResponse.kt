package com.ieum.ieum_back.chat.dto

import com.ieum.ieum_back.entity.ChatMessage
import com.ieum.ieum_back.entity.enums.MessageType
import java.time.LocalDateTime
import java.util.*

/**
 * 서버 -> 클라이언트 메시지 응답 DTO
 * 프론트엔드 호환성을 위해 UUID와 날짜를 String으로 변환
 */
data class WebSocketMessageResponse(
    val id: String,                  // UUID -> String
    val senderId: String,            // UUID -> String
    val senderName: String,
    val senderProfileImage: String?,
    val content: String?,
    val type: MessageType,
    val imageUrl: String?,
    val isRead: Boolean,
    val readAt: String?,             // LocalDateTime -> String (ISO-8601)
    val createdAt: String,           // LocalDateTime -> String (ISO-8601)
    val tempId: String? = null       // 클라이언트 임시 ID 반환
) {
    companion object {
        fun from(message: ChatMessage, tempId: String? = null): WebSocketMessageResponse {
            return WebSocketMessageResponse(
                id = message.id!!.toString(),
                senderId = message.sender.id!!.toString(),
                senderName = message.sender.name,
                senderProfileImage = message.sender.profileImage,
                content = message.content,
                type = message.type,
                imageUrl = message.imageUrl,
                isRead = message.isRead,
                readAt = message.readAt?.toString(),
                createdAt = message.createdAt.toString(),
                tempId = tempId
            )
        }
    }
}
