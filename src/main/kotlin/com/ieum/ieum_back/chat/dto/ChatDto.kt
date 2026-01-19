package com.ieum.ieum_back.chat.dto

import com.ieum.ieum_back.entity.ChatMessage
import com.ieum.ieum_back.entity.enums.MessageType
import java.time.LocalDateTime
import java.util.*

data class ChatRoomResponse(
    val coupleId: UUID,
    val partnerId: UUID?,
    val partnerName: String?,
    val partnerProfileImage: String?,
    val lastMessage: ChatMessageResponse?,
    val unreadCount: Long
)

data class SendMessageRequest(
    val content: String? = null,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null
)

data class ChatMessageResponse(
    val id: UUID,
    val senderId: UUID,
    val content: String?,
    val type: MessageType,
    val imageUrl: String?,
    val isRead: Boolean,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(message: ChatMessage): ChatMessageResponse = ChatMessageResponse(
            id = message.id!!,
            senderId = message.sender.id!!,
            content = message.content,
            type = message.type,
            imageUrl = message.imageUrl,
            isRead = message.isRead,
            readAt = message.readAt,
            createdAt = message.createdAt
        )
    }
}

data class ChatMessageListResponse(
    val messages: List<ChatMessageResponse>,
    val totalCount: Long,
    val page: Int,
    val size: Int
)
