package com.ieum.ieum_back.users.controller

import com.ieum.ieum_back.auth.dto.UserResponse
import com.ieum.ieum_back.users.dto.CreateUserRequest
import com.ieum.ieum_back.users.dto.UpdateUserRequest
import com.ieum.ieum_back.users.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request))
    }

    @GetMapping("/me")
    fun getMe(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUser(userId))
    }

    @PatchMapping("/me")
    fun updateMe(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.updateUser(userId, request))
    }
}
