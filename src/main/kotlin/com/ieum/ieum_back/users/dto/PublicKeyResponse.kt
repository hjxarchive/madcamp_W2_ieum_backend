package com.ieum.ieum_back.users.dto

import java.util.*

/**
 * 공개키 응답 DTO
 */
data class PublicKeyResponse(
    val userId: UUID,
    val publicKey: String?,
    val hasKey: Boolean
) {
    companion object {
        fun from(userId: UUID, publicKey: String?): PublicKeyResponse {
            return PublicKeyResponse(
                userId = userId,
                publicKey = publicKey,
                hasKey = publicKey != null
            )
        }
    }
}
