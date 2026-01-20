package com.ieum.ieum_back.chat.dto

/**
 * E2EE 암호화된 메시지 전송 DTO
 */
data class E2EEMessageRequest(
    val type: String = "TEXT",
    val encryptedContent: String,  // Base64 인코딩된 암호화 메시지
    val encryptedKey: String,      // Base64 인코딩된 암호화 세션키
    val iv: String,                // Base64 인코딩된 초기화 벡터
    val imageUrl: String? = null,  // 이미지는 암호화 안함 (옵션)
    val tempId: String? = null
)
