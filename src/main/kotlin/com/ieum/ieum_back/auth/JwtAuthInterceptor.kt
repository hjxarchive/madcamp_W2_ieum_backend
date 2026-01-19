package com.ieum.ieum_back.auth

import com.ieum.ieum_back.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class JwtAuthInterceptor(
    private val jwtProvider: JwtProvider
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // OPTIONS 요청은 통과 (CORS preflight)
        if (request.method == "OPTIONS") {
            return true
        }

        val authHeader = request.getHeader("Authorization")
            ?: throw UnauthorizedException("Authorization header is required")

        if (!authHeader.startsWith("Bearer ")) {
            throw UnauthorizedException("Invalid authorization header format")
        }

        val token = authHeader.substring(7)

        if (!jwtProvider.validateToken(token)) {
            throw UnauthorizedException("Invalid or expired token")
        }

        // 요청에 사용자 ID 저장 (컨트롤러에서 사용)
        val userId = jwtProvider.getUserIdFromToken(token)
        request.setAttribute("userId", userId)

        return true
    }
}
