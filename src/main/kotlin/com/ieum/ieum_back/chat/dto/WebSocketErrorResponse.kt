package com.ieum.ieum_back.chat.dto

/**
 * WebSocket 에러 응답 DTO
 */
data class WebSocketErrorResponse(
    val type: String = "ERROR",
    val code: ErrorCode,
    val message: String,
    val tempId: String? = null
)

enum class ErrorCode {
    AUTH_FAILED,
    UNAUTHORIZED,
    SEND_FAILED,
    INVALID_MESSAGE,
    NOT_IN_COUPLE
}
