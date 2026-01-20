package com.ieum.ieum_back.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "couples")
class Couple(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user1_id", nullable = false)
    var user1Id: UUID,

    @Column(name = "user2_id")
    var user2Id: UUID? = null,

    var anniversary: LocalDate? = null,

    @Column(name = "invite_code", unique = true, length = 6)
    var inviteCode: String? = null,

    @Column(name = "invite_expires_at")
    var inviteExpiresAt: LocalDateTime? = null,

    // E2EE 공유 대칭키 (각 사용자의 공개키로 암호화된 버전)
    @Column(name = "encrypted_shared_key_user1", columnDefinition = "text")
    var encryptedSharedKeyUser1: String? = null,  // user1의 공개키로 암호화된 대칭키

    @Column(name = "encrypted_shared_key_user2", columnDefinition = "text")
    var encryptedSharedKeyUser2: String? = null,  // user2의 공개키로 암호화된 대칭키

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)
