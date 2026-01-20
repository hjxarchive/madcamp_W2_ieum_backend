package com.ieum.ieum_back.chat.dto

import java.time.LocalDateTime
import java.util.*

/**
 * 읽음 상태 업데이트 DTO
 * 프론트엔드 호환성을 위해 UUID와 날짜를 String으로 변환
 */
data class ReadReceiptMessage(
    val type: String = "READ_RECEIPT",
    val messageIds: List<String>,     // UUID -> String
    val readAt: String                // LocalDateTime -> String (ISO-8601)
) {
    companion object {
        fun create(messageIds: List<UUID>): ReadReceiptMessage {
            return ReadReceiptMessage(
                messageIds = messageIds.map { it.toString() },
                readAt = LocalDateTime.now().toString()
            )
        }
    }
}
