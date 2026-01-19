package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.ChatMessage
import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    fun findByCoupleOrderByCreatedAtDesc(couple: Couple, pageable: Pageable): Page<ChatMessage>

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.couple = :couple AND m.sender != :user AND m.isRead = false")
    fun countUnreadMessages(couple: Couple, user: User): Long

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = :readAt WHERE m.couple = :couple AND m.sender != :user AND m.isRead = false")
    fun markMessagesAsRead(couple: Couple, user: User, readAt: LocalDateTime)
}
