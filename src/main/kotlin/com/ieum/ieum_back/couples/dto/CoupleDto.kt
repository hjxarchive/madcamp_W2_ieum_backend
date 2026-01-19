package com.ieum.ieum_back.couples.dto

import com.ieum.ieum_back.auth.dto.UserResponse
import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class InviteCodeResponse(
    val inviteCode: String,
    val expiresAt: LocalDateTime
)

data class JoinCoupleRequest(
    val inviteCode: String
)

data class UpdateCoupleRequest(
    val anniversary: LocalDate? = null
)

data class CoupleResponse(
    val id: UUID,
    val anniversary: LocalDate?,
    val partner: UserResponse?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(couple: Couple, currentUser: User, partner: User?): CoupleResponse = CoupleResponse(
            id = couple.id!!,
            anniversary = couple.anniversary,
            partner = partner?.let { UserResponse.from(it) },
            createdAt = couple.createdAt
        )
    }
}
