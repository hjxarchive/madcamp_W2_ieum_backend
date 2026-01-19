package com.ieum.ieum_back.users.service

import com.ieum.ieum_back.auth.dto.UserResponse
import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.exception.ConflictException
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.UserRepository
import com.ieum.ieum_back.users.dto.CreateUserRequest
import com.ieum.ieum_back.users.dto.UpdateUserRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {

    fun createUser(request: CreateUserRequest): UserResponse {
        if (userRepository.findByEmail(request.email) != null) {
            throw ConflictException("Email already exists")
        }

        val user = User(
            email = request.email,
            name = request.name,
            nickname = request.nickname,
            profileImage = request.profileImage,
            birthday = request.birthday,
            gender = request.gender,
            googleId = request.googleId
        )

        return UserResponse.from(userRepository.save(user))
    }

    @Transactional(readOnly = true)
    fun getUser(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        return UserResponse.from(user)
    }

    fun updateUser(userId: UUID, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        request.name?.let { user.name = it }
        request.nickname?.let { user.nickname = it }
        request.profileImage?.let { user.profileImage = it }
        request.birthday?.let { user.birthday = it }
        request.gender?.let { user.gender = it }
        user.updatedAt = LocalDateTime.now()

        return UserResponse.from(userRepository.save(user))
    }
}
