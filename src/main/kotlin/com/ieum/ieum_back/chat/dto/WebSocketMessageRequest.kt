package com.ieum.ieum_back.chat.dto

import com.ieum.ieum_back.entity.enums.MessageType

/**
 * 클라이언트 -> 서버 메시지 전송 DTO
 */
data class WebSocketMessageRequest(
    val type: MessageType = MessageType.TEXT,
    val content: String? = null,
    val imageUrl: String? = null,
    val tempId: String? = null  // 클라이언트 임시 ID (전송 상태 추적용)
)
