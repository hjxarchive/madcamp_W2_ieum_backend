package com.ieum.ieum_back.chat.service

import com.ieum.ieum_back.chat.dto.*
import com.ieum.ieum_back.entity.ChatMessage
import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.entity.enums.MessageType
import com.ieum.ieum_back.repository.ChatMessageRepository
import com.ieum.ieum_back.repository.CoupleRepository
import com.ieum.ieum_back.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * WebSocket 채팅 비즈니스 로직
 */
@Service
@Transactional
class ChatWebSocketService(
    private val chatMessageRepository: ChatMessageRepository,
    private val coupleRepository: CoupleRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(ChatWebSocketService::class.java)

    /**
     * 메시지 저장 및 브로드캐스트
     */
    fun sendMessage(
        coupleId: UUID,
        senderId: UUID,
        request: WebSocketMessageRequest
    ): WebSocketMessageResponse {
        // 1. 사용자 조회
        val sender = userRepository.findById(senderId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다: $senderId") }

        // 2. 커플 조회
        val couple = coupleRepository.findById(coupleId)
            .orElseThrow { IllegalArgumentException("커플을 찾을 수 없습니다: $coupleId") }

        // 3. 권한 확인 (해당 커플의 멤버인지)
        if (!isCoupleMember(sender, couple)) {
            throw IllegalArgumentException("해당 커플의 멤버가 아닙니다")
        }

        // 4. 메시지 유효성 검증
        validateMessage(request)

        // 5. 메시지 저장
        val message = ChatMessage(
            couple = couple,
            sender = sender,
            content = request.content,
            type = request.type,
            imageUrl = request.imageUrl,
            isRead = false
        )
        val savedMessage = chatMessageRepository.save(message)

        logger.info("Message saved: ${savedMessage.id} from ${sender.name} to couple ${coupleId}")

        // 6. 응답 DTO 생성
        return WebSocketMessageResponse.from(savedMessage, request.tempId)
    }

    /**
     * 메시지 읽음 처리
     */
    fun markMessagesAsRead(
        coupleId: UUID,
        userId: UUID,
        messageIds: List<UUID>
    ): ReadReceiptMessage {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = coupleRepository.findById(coupleId)
            .orElseThrow { IllegalArgumentException("커플을 찾을 수 없습니다") }

        val readAt = LocalDateTime.now()

        // 특정 메시지들 읽음 처리
        messageIds.forEach { messageId ->
            chatMessageRepository.findById(messageId).ifPresent { message ->
                if (message.couple.id == coupleId && message.sender.id != userId && !message.isRead) {
                    message.isRead = true
                    message.readAt = readAt
                    chatMessageRepository.save(message)
                }
            }
        }

        logger.info("Marked ${messageIds.size} messages as read for user $userId in couple $coupleId")

        return ReadReceiptMessage.create(messageIds)
    }

    /**
     * 모든 안 읽은 메시지 읽음 처리
     */
    fun markAllMessagesAsRead(coupleId: UUID, userId: UUID): Int {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

        val couple = coupleRepository.findById(coupleId)
            .orElseThrow { IllegalArgumentException("커플을 찾을 수 없습니다") }

        val readAt = LocalDateTime.now()
        chatMessageRepository.markMessagesAsRead(couple, user, readAt)

        logger.info("Marked all messages as read for user $userId in couple $coupleId")

        return 0 // 업데이트된 개수 (필요 시 쿼리 수정)
    }

    /**
     * 시스템 메시지 브로드캐스트 (접속/종료)
     */
    fun broadcastSystemMessage(coupleId: UUID, userId: UUID, event: SystemEvent) {
        val systemMessage = SystemMessage(
            event = event,
            userId = userId
        )

        messagingTemplate.convertAndSend("/topic/couple/$coupleId", systemMessage)
        logger.info("System message broadcast: $event for user $userId in couple $coupleId")
    }

    /**
     * 메시지 유효성 검증
     */
    private fun validateMessage(request: WebSocketMessageRequest) {
        when (request.type) {
            MessageType.TEXT -> {
                if (request.content.isNullOrBlank()) {
                    throw IllegalArgumentException("텍스트 메시지는 내용이 필요합니다")
                }
            }
            MessageType.IMAGE -> {
                if (request.imageUrl.isNullOrBlank()) {
                    throw IllegalArgumentException("이미지 메시지는 이미지 URL이 필요합니다")
                }
            }
            MessageType.STICKER -> {
                if (request.imageUrl.isNullOrBlank()) {
                    throw IllegalArgumentException("스티커 메시지는 이미지 URL이 필요합니다")
                }
            }
            MessageType.SHARED_SCHEDULE,
            MessageType.SHARED_PLACE,
            MessageType.SHARED_BUCKET -> {
                if (request.content.isNullOrBlank()) {
                    throw IllegalArgumentException("공유 컨텐츠는 내용이 필요합니다 (JSON 형식)")
                }
            }
        }
    }

    /**
     * 사용자가 해당 커플의 멤버인지 확인
     */
    private fun isCoupleMember(user: User, couple: Couple): Boolean {
        return couple.user1Id == user.id || couple.user2Id == user.id
    }

    /**
     * 상대방 ID 조회
     */
    fun getPartnerId(coupleId: UUID, userId: UUID): UUID? {
        val couple = coupleRepository.findById(coupleId).orElse(null) ?: return null
        return when (userId) {
            couple.user1Id -> couple.user2Id
            couple.user2Id -> couple.user1Id
            else -> null
        }
    }

    /**
     * E2EE 암호화된 메시지 저장 및 브로드캐스트
     */
    fun sendE2EEMessage(
        coupleId: UUID,
        senderId: UUID,
        request: E2EEMessageRequest
    ): E2EEMessageResponse {
        // 1. 사용자 조회
        val sender = userRepository.findById(senderId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다: $senderId") }

        // 2. 커플 조회
        val couple = coupleRepository.findById(coupleId)
            .orElseThrow { IllegalArgumentException("커플을 찾을 수 없습니다: $coupleId") }

        // 3. 권한 확인
        if (!isCoupleMember(sender, couple)) {
            throw IllegalArgumentException("해당 커플의 멤버가 아닙니다")
        }

        // 4. E2EE 메시지 저장 (암호화된 상태 그대로)
        val message = ChatMessage(
            couple = couple,
            sender = sender,
            content = null,  // 평문은 저장하지 않음
            type = MessageType.TEXT,
            imageUrl = request.imageUrl,
            isRead = false,
            isEncrypted = true,
            encryptedContent = request.encryptedContent,
            encryptedKey = request.encryptedKey,
            iv = request.iv
        )
        val savedMessage = chatMessageRepository.save(message)

        logger.info("E2EE message saved: ${savedMessage.id} from ${sender.name} to couple ${coupleId}")

        // 5. 응답 DTO 생성
        return E2EEMessageResponse.from(savedMessage, request.tempId)
    }
}
