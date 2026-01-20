package com.ieum.ieum_back.chat.dto

import java.time.LocalDateTime
import java.util.*

/**
 * 시스템 메시지 DTO (접속/종료/타이핑 등)
 */
data class SystemMessage(
    val type: String = "SYSTEM",
    val event: SystemEvent,
    val userId: UUID,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class SystemEvent {
    USER_CONNECTED,
    USER_DISCONNECTED,
    TYPING
}
