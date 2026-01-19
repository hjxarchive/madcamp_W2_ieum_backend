package com.ieum.ieum_back.chat.service

import com.ieum.ieum_back.chat.dto.*
import com.ieum.ieum_back.entity.ChatMessage
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.ChatMessageRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getChatRoom(userId: UUID): ChatRoomResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val partnerId = if (couple.user1Id == userId) couple.user2Id else couple.user1Id
        val partner = partnerId?.let { userRepository.findById(it).orElse(null) }

        val lastMessagePage = chatMessageRepository.findByCoupleOrderByCreatedAtDesc(
            couple,
            PageRequest.of(0, 1)
        )
        val lastMessage = lastMessagePage.content.firstOrNull()?.let { ChatMessageResponse.from(it) }

        val unreadCount = chatMessageRepository.countUnreadMessages(couple, user)

        return ChatRoomResponse(
            coupleId = couple.id!!,
            partnerId = partner?.id,
            partnerName = partner?.name,
            partnerProfileImage = partner?.profileImage,
            lastMessage = lastMessage,
            unreadCount = unreadCount
        )
    }

    fun sendMessage(userId: UUID, roomId: UUID, request: SendMessageRequest): ChatMessageResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        if (couple.id != roomId) {
            throw NotFoundException("Chat room not found")
        }

        val message = ChatMessage(
            couple = couple,
            sender = user,
            content = request.content,
            type = request.type,
            imageUrl = request.imageUrl
        )

        return ChatMessageResponse.from(chatMessageRepository.save(message))
    }

    fun getMessages(userId: UUID, roomId: UUID, page: Int, size: Int): ChatMessageListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        if (couple.id != roomId) {
            throw NotFoundException("Chat room not found")
        }

        // 읽음 처리
        chatMessageRepository.markMessagesAsRead(couple, user, LocalDateTime.now())

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val messagePage = chatMessageRepository.findByCoupleOrderByCreatedAtDesc(couple, pageable)

        return ChatMessageListResponse(
            messages = messagePage.content.map { ChatMessageResponse.from(it) },
            totalCount = messagePage.totalElements,
            page = page,
            size = size
        )
    }
}
