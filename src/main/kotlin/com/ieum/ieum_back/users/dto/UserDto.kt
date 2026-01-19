package com.ieum.ieum_back.users.dto

import com.ieum.ieum_back.entity.enums.GenderType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Name is required")
    val name: String,

    val nickname: String? = null,
    val profileImage: String? = null,
    val birthday: LocalDate? = null,
    val gender: GenderType? = null,
    val googleId: String? = null
)

data class UpdateUserRequest(
    val name: String? = null,
    val nickname: String? = null,
    val profileImage: String? = null,
    val birthday: LocalDate? = null,
    val gender: GenderType? = null
)
