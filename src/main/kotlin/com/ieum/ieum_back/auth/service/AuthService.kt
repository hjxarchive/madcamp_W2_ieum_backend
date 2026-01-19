package com.ieum.ieum_back.auth.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.ieum.ieum_back.auth.JwtProvider
import com.ieum.ieum_back.auth.dto.AuthResponse
import com.ieum.ieum_back.auth.dto.GoogleLoginRequest
import com.ieum.ieum_back.auth.dto.GoogleUserInfo
import com.ieum.ieum_back.auth.dto.UserResponse
import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.exception.UnauthorizedException
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    @Value("\${google.client-id}")
    private val googleClientId: String
) {
    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(googleClientId))
            .build()
    }

    fun googleLogin(request: GoogleLoginRequest): AuthResponse {
        // 1. Google ID Token 검증
        val googleUserInfo = verifyGoogleToken(request.idToken)

        // 2. 기존 사용자 조회 또는 신규 생성
        val user = userRepository.findByGoogleId(googleUserInfo.googleId)
            ?: userRepository.findByEmail(googleUserInfo.email)?.apply {
                // 이메일은 같지만 Google ID가 없는 경우 -> Google ID 연결
                this.googleId = googleUserInfo.googleId
                this.profileImage = this.profileImage ?: googleUserInfo.profileImage
                userRepository.save(this)
            }
            ?: createNewUser(googleUserInfo)

        // 3. JWT 토큰 생성
        val accessToken = jwtProvider.generateToken(user.id!!, user.email)

        return AuthResponse(
            accessToken = accessToken,
            user = UserResponse.from(user)
        )
    }

    private fun verifyGoogleToken(idToken: String): GoogleUserInfo {
        val googleIdToken: GoogleIdToken? = try {
            verifier.verify(idToken)
        } catch (e: Exception) {
            throw UnauthorizedException("Invalid Google token: ${e.message}")
        }

        if (googleIdToken == null) {
            throw UnauthorizedException("Invalid Google token")
        }

        val payload = googleIdToken.payload

        return GoogleUserInfo(
            googleId = payload.subject,  // Google 고유 사용자 ID
            email = payload.email,
            name = payload["name"] as? String ?: payload.email.substringBefore("@"),
            profileImage = payload["picture"] as? String
        )
    }

    private fun createNewUser(googleUserInfo: GoogleUserInfo): User {
        val user = User(
            email = googleUserInfo.email,
            name = googleUserInfo.name,
            googleId = googleUserInfo.googleId,
            profileImage = googleUserInfo.profileImage
        )
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UnauthorizedException("User not found") }
        return UserResponse.from(user)
    }

    fun logout(userId: UUID) {
        // JWT는 stateless이므로 서버에서 무효화할 수 없음
        // 클라이언트에서 토큰 삭제로 처리
        // Redis 등을 사용한 토큰 블랙리스트 구현 가능
    }
}
