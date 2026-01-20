package com.ieum.ieum_back.couples.dto

/**
 * 커플 공유 대칭키 설정 요청 DTO
 */
data class SharedKeyRequest(
    val encryptedSharedKey: String  // 내 공개키로 암호화된 대칭키 (Base64)
)
