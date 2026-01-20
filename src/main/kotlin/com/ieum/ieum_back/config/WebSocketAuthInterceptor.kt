package com.ieum.ieum_back.config

import com.ieum.ieum_back.auth.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.*

/**
 * WebSocket Handshake 시 JWT 토큰 검증
 */
@Component
class WebSocketAuthInterceptor(
    private val jwtProvider: JwtProvider
) : HandshakeInterceptor {

    private val logger = LoggerFactory.getLogger(WebSocketAuthInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        try {
            logger.info("========== WebSocket Handshake Attempt ==========")
            logger.info("Request URI: ${request.uri}")
            logger.info("Request Headers: ${request.headers.entries.joinToString { "${it.key}: ${it.value}" }}")
            
            // Query Parameter에서 토큰 추출: ?token=eyJhbGciOi...
            val token = extractTokenFromQuery(request)

            if (token.isNullOrBlank()) {
                logger.error("❌ WebSocket connection REJECTED: No token provided in query parameter")
                logger.error("Query String: ${(request as? ServletServerHttpRequest)?.servletRequest?.queryString}")
                return false
            }

            logger.info("✅ Token found: ${token.take(50)}...")

            // JWT 토큰 검증
            if (!jwtProvider.validateToken(token)) {
                logger.error("❌ WebSocket connection REJECTED: Invalid token")
                return false
            }

            logger.info("✅ Token validation successful")

            // 토큰에서 사용자 정보 추출하여 세션에 저장
            val userId = jwtProvider.getUserIdFromToken(token)
            attributes["userId"] = userId
            attributes["token"] = token

            logger.info("✅ WebSocket connection ACCEPTED for user: $userId")
            logger.info("=================================================")
            return true

        } catch (e: Exception) {
            logger.error("❌ WebSocket authentication error: ${e.message}", e)
            logger.error("Stack trace:", e)
            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // Handshake 후 처리 (필요 시 구현)
    }

    /**
     * Query Parameter에서 token 추출
     */
    private fun extractTokenFromQuery(request: ServerHttpRequest): String? {
        if (request is ServletServerHttpRequest) {
            return request.servletRequest.getParameter("token")
        }
        return null
    }
}
