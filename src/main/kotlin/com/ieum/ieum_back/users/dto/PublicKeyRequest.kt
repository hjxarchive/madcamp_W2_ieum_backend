package com.ieum.ieum_back.users.dto

/**
 * 공개키 등록/업데이트 요청 DTO
 */
data class PublicKeyRequest(
    val publicKey: String  // Base64 인코딩된 공개키
)
