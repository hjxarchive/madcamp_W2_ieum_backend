package com.ieum.ieum_back.config

import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import java.security.Principal

/**
 * STOMP CONNECT 프레임 인터셉터
 * WebSocket Handshake 이후 STOMP 레벨에서 추가 검증
 */
@Component
class StompConnectInterceptor : ChannelInterceptor {

    private val logger = LoggerFactory.getLogger(StompConnectInterceptor::class.java)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null) {
            when (accessor.command) {
                StompCommand.CONNECT -> {
                    logger.info("========== STOMP CONNECT Frame Received ==========")
                    
                    val sessionId = accessor.sessionId
                    val userId = accessor.sessionAttributes?.get("userId")
                    val token = accessor.sessionAttributes?.get("token")
                    
                    logger.info("Session ID: $sessionId")
                    logger.info("User ID: $userId")
                    logger.info("Token present: ${token != null}")
                    
                    if (userId != null) {
                        // 사용자 인증 정보를 Principal로 설정
                        accessor.user = Principal { userId.toString() }
                        logger.info("✅ STOMP CONNECT authenticated for user: $userId")
                    } else {
                        logger.warn("⚠️ STOMP CONNECT without userId (handshake may have failed)")
                    }
                    
                    logger.info("===================================================")
                }
                
                StompCommand.DISCONNECT -> {
                    logger.info("STOMP DISCONNECT: session=${accessor.sessionId}")
                }
                
                StompCommand.SUBSCRIBE -> {
                    logger.info("STOMP SUBSCRIBE: destination=${accessor.destination}, session=${accessor.sessionId}")
                }
                
                StompCommand.SEND -> {
                    logger.debug("STOMP SEND: destination=${accessor.destination}")
                }
                
                else -> {
                    // 다른 STOMP 명령어는 로그하지 않음
                }
            }
        }

        return message
    }
}
