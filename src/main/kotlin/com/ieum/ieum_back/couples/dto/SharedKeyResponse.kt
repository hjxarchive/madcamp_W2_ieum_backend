package com.ieum.ieum_back.couples.dto

/**
 * 커플 공유 대칭키 응답 DTO
 */
data class SharedKeyResponse(
    val encryptedSharedKey: String?,  // 내 공개키로 암호화된 대칭키
    val hasSharedKey: Boolean
)
