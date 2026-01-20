package com.ieum.ieum_back.chat.dto

/**
 * 타이핑 인디케이터 응답 DTO
 * 서버 -> 클라이언트
 */
data class TypingIndicatorResponse(
    val userId: String,      // UUID를 String으로 변환
    val isTyping: Boolean
)
