package com.ieum.ieum_back.couples.controller

import com.ieum.ieum_back.couples.dto.SharedKeyRequest
import com.ieum.ieum_back.couples.dto.SharedKeyResponse
import com.ieum.ieum_back.repository.CoupleRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * 커플 공유 대칭키 관리 API
 */
@RestController
@RequestMapping("/api/couples")
class SharedKeyController(
    private val coupleRepository: CoupleRepository,
    private val userRepository: UserRepository
) {

    /**
     * 커플 공유 대칭키 설정
     * 
     * 커플 연결 시 user1이 대칭키를 생성하고 설정
     * - user1: 자신의 공개키로 암호화된 대칭키 저장
     * - user2: user2의 공개키로 암호화된 대칭키 저장 (user1이 암호화)
     */
    @PostMapping("/me/shared-key")
    fun setSharedKey(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: SharedKeyRequest
    ): ResponseEntity<SharedKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = user.couple
            ?: throw IllegalArgumentException("커플이 연결되어 있지 않습니다")

        // 현재 사용자가 user1인지 user2인지 확인
        if (couple.user1Id == user.id) {
            couple.encryptedSharedKeyUser1 = request.encryptedSharedKey
        } else if (couple.user2Id == user.id) {
            couple.encryptedSharedKeyUser2 = request.encryptedSharedKey
        } else {
            throw IllegalArgumentException("커플 정보가 올바르지 않습니다")
        }

        coupleRepository.save(couple)

        return ResponseEntity.ok(SharedKeyResponse(
            encryptedSharedKey = request.encryptedSharedKey,
            hasSharedKey = true
        ))
    }

    /**
     * 커플 공유 대칭키 조회
     * 
     * 내 공개키로 암호화된 대칭키를 반환
     */
    @GetMapping("/me/shared-key")
    fun getSharedKey(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<SharedKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = user.couple
            ?: throw IllegalArgumentException("커플이 연결되어 있지 않습니다")

        val encryptedKey = when (user.id) {
            couple.user1Id -> couple.encryptedSharedKeyUser1
            couple.user2Id -> couple.encryptedSharedKeyUser2
            else -> null
        }

        return ResponseEntity.ok(SharedKeyResponse(
            encryptedSharedKey = encryptedKey,
            hasSharedKey = encryptedKey != null
        ))
    }

    /**
     * 상대방을 위한 암호화된 대칭키 설정
     * 
     * user1이 대칭키를 생성한 후, user2의 공개키로 암호화하여 저장
     */
    @PostMapping("/partner/shared-key")
    fun setPartnerSharedKey(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: SharedKeyRequest
    ): ResponseEntity<SharedKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = user.couple
            ?: throw IllegalArgumentException("커플이 연결되어 있지 않습니다")

        // 상대방의 암호화된 키 저장
        if (couple.user1Id == user.id) {
            couple.encryptedSharedKeyUser2 = request.encryptedSharedKey
        } else if (couple.user2Id == user.id) {
            couple.encryptedSharedKeyUser1 = request.encryptedSharedKey
        } else {
            throw IllegalArgumentException("커플 정보가 올바르지 않습니다")
        }

        coupleRepository.save(couple)

        return ResponseEntity.ok(SharedKeyResponse(
            encryptedSharedKey = request.encryptedSharedKey,
            hasSharedKey = true
        ))
    }
}
