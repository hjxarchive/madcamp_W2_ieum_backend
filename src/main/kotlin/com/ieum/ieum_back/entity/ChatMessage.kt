package com.ieum.ieum_back.entity

import com.ieum.ieum_back.entity.enums.MessageType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: Couple,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Column(columnDefinition = "text")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    var type: MessageType = MessageType.TEXT,

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,

    @Column(name = "is_read")
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
