package com.ieum.ieum_back.chat.dto

/**
 * 타이핑 인디케이터 요청 DTO
 * 클라이언트 -> 서버
 */
data class TypingRequest(
    val isTyping: Boolean
)
