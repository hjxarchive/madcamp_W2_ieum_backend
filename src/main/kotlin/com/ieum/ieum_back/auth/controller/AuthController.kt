package com.ieum.ieum_back.auth.controller

import com.ieum.ieum_back.auth.CurrentUser
import com.ieum.ieum_back.auth.dto.AuthResponse
import com.ieum.ieum_back.auth.dto.GoogleLoginRequest
import com.ieum.ieum_back.auth.dto.UserResponse
import com.ieum.ieum_back.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * Google OAuth 로그인
     * - 클라이언트에서 Google Sign-In으로 받은 ID Token을 전송
     * - 서버에서 토큰 검증 후 JWT 발급
     */
    @PostMapping("/google")
    fun googleLogin(@RequestBody request: GoogleLoginRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.googleLogin(request))
    }

    /**
     * 로그아웃
     * - JWT는 stateless이므로 클라이언트에서 토큰 삭제로 처리
     */
    @PostMapping("/logout")
    fun logout(@CurrentUser userId: UUID): ResponseEntity<Map<String, String>> {
        authService.logout(userId)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    fun getCurrentUser(@CurrentUser userId: UUID): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(authService.getCurrentUser(userId))
    }
}
