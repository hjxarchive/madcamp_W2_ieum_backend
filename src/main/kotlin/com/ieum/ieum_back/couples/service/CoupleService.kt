package com.ieum.ieum_back.couples.service

import com.ieum.ieum_back.couples.dto.AnniversaryDto
import com.ieum.ieum_back.couples.dto.AnniversarySyncMessage
import com.ieum.ieum_back.couples.dto.CoupleResponse
import com.ieum.ieum_back.couples.dto.InviteCodeResponse
import com.ieum.ieum_back.couples.dto.UpdateCoupleRequest
import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.exception.BadRequestException
import com.ieum.ieum_back.exception.ConflictException
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.CoupleRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class CoupleService(
    private val coupleRepository: CoupleRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    fun createInviteCode(userId: UUID): InviteCodeResponse {
        val user = findUserById(userId)

        if (user.couple != null) {
            throw ConflictException("User already has a couple")
        }

        val inviteCode = generateInviteCode()
        val expiresAt = LocalDateTime.now().plusDays(1)

        val couple = Couple(
            user1Id = userId,
            inviteCode = inviteCode,
            inviteExpiresAt = expiresAt
        )

        val savedCouple = coupleRepository.save(couple)
        user.couple = savedCouple
        userRepository.save(user)

        return InviteCodeResponse(
            inviteCode = inviteCode,
            expiresAt = expiresAt
        )
    }

    fun joinCouple(userId: UUID, inviteCode: String): CoupleResponse {
        val user = findUserById(userId)

        if (user.couple != null) {
            throw ConflictException("User already has a couple")
        }

        val couple = coupleRepository.findByInviteCode(inviteCode)
            ?: throw NotFoundException("Invalid invite code")

        if (couple.inviteExpiresAt?.isBefore(LocalDateTime.now()) == true) {
            throw BadRequestException("Invite code has expired")
        }

        if (couple.user2Id != null) {
            throw ConflictException("Couple already complete")
        }

        if (couple.user1Id == userId) {
            throw BadRequestException("Cannot join your own couple")
        }

        couple.user2Id = userId
        couple.inviteCode = null
        couple.inviteExpiresAt = null

        val savedCouple = coupleRepository.save(couple)
        user.couple = savedCouple
        userRepository.save(user)

        val partner = userRepository.findById(couple.user1Id).orElse(null)

        return CoupleResponse.from(savedCouple, user, partner)
    }

    @Transactional(readOnly = true)
    fun getCouple(userId: UUID): CoupleResponse {
        val user = findUserById(userId)
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val partnerId = if (couple.user1Id == userId) couple.user2Id else couple.user1Id
        val partner = partnerId?.let { userRepository.findById(it).orElse(null) }

        return CoupleResponse.from(couple, user, partner)
    }

    fun updateCouple(userId: UUID, request: UpdateCoupleRequest): CoupleResponse {
        val user = findUserById(userId)
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        request.anniversary?.let { couple.anniversary = it }

        val savedCouple = coupleRepository.save(couple)

        val partnerId = if (couple.user1Id == userId) couple.user2Id else couple.user1Id
        val partner = partnerId?.let { userRepository.findById(it).orElse(null) }

        val response = CoupleResponse.from(savedCouple, user, partner)
        
        // WebSocket 브로드캐스트
        if (request.anniversary != null) {
            broadcastAnniversarySync(savedCouple.id!!, savedCouple.anniversary, userId)
        }
        
        return response
    }

    fun deleteCouple(userId: UUID) {
        val user = findUserById(userId)
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        // 두 사용자의 couple 참조 제거
        val user1 = userRepository.findById(couple.user1Id).orElse(null)
        val user2 = couple.user2Id?.let { userRepository.findById(it).orElse(null) }

        user1?.let {
            it.couple = null
            userRepository.save(it)
        }
        user2?.let {
            it.couple = null
            userRepository.save(it)
        }

        couple.deletedAt = LocalDateTime.now()
        coupleRepository.save(couple)
    }

    private fun findUserById(userId: UUID): User {
        return userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
    
    /**
     * 기념일 WebSocket 실시간 동기화 브로드캐스트
     */
    private fun broadcastAnniversarySync(
        coupleId: UUID,
        anniversary: java.time.LocalDate?,
        userId: UUID
    ) {
        val message = AnniversarySyncMessage(
            eventType = "ANNIVERSARY_UPDATED",
            anniversary = AnniversaryDto.from(anniversary),
            userId = userId.toString(),
            timestamp = LocalDateTime.now().toString()
        )
        
        messagingTemplate.convertAndSend(
            "/topic/couple/$coupleId/anniversary",
            message
        )
    }
}
