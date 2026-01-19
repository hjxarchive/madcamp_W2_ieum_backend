package com.ieum.ieum_back.auth.dto

import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.entity.enums.GenderType
import java.time.LocalDate
import java.util.*

data class GoogleLoginRequest(
    val idToken: String
)

data class GoogleUserInfo(
    val googleId: String,
    val email: String,
    val name: String,
    val profileImage: String?
)

data class AuthResponse(
    val accessToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val nickname: String?,
    val profileImage: String?,
    val birthday: LocalDate?,
    val gender: GenderType?,
    val coupleId: UUID?,
    val mbtiType: String?,
    val isActive: Boolean
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id!!,
            email = user.email,
            name = user.name,
            nickname = user.nickname,
            profileImage = user.profileImage,
            birthday = user.birthday,
            gender = user.gender,
            coupleId = user.couple?.id,
            mbtiType = user.mbtiType,
            isActive = user.isActive
        )
    }
}
