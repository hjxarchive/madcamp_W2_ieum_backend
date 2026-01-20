package com.ieum.ieum_back.users.controller

import com.ieum.ieum_back.users.dto.PublicKeyRequest
import com.ieum.ieum_back.users.dto.PublicKeyResponse
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * E2EE 공개키 관리 API
 */
@RestController
@RequestMapping("/api/users")
class PublicKeyController(
    private val userRepository: UserRepository
) {

    /**
     * 내 공개키 등록/업데이트
     */
    @PutMapping("/me/public-key")
    fun updateMyPublicKey(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: PublicKeyRequest
    ): ResponseEntity<PublicKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        user.publicKey = request.publicKey
        userRepository.save(user)

        return ResponseEntity.ok(PublicKeyResponse.from(user.id!!, user.publicKey))
    }

    /**
     * 내 공개키 조회
     */
    @GetMapping("/me/public-key")
    fun getMyPublicKey(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<PublicKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        return ResponseEntity.ok(PublicKeyResponse.from(user.id!!, user.publicKey))
    }

    /**
     * 상대방(파트너) 공개키 조회
     */
    @GetMapping("/partner/public-key")
    fun getPartnerPublicKey(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<PublicKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = user.couple
            ?: throw IllegalArgumentException("커플이 연결되어 있지 않습니다")

        val partnerId = when (user.id) {
            couple.user1Id -> couple.user2Id
            couple.user2Id -> couple.user1Id
            else -> throw IllegalArgumentException("커플 정보가 올바르지 않습니다")
        } ?: throw IllegalArgumentException("파트너를 찾을 수 없습니다")

        val partner = userRepository.findById(partnerId)
            .orElseThrow { IllegalArgumentException("파트너를 찾을 수 없습니다") }

        return ResponseEntity.ok(PublicKeyResponse.from(partner.id!!, partner.publicKey))
    }

    /**
     * 특정 사용자의 공개키 조회 (UUID로)
     */
    @GetMapping("/{userId}/public-key")
    fun getUserPublicKey(
        @PathVariable userId: String
    ): ResponseEntity<PublicKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        return ResponseEntity.ok(PublicKeyResponse.from(user.id!!, user.publicKey))
    }
}
