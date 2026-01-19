package com.ieum.ieum_back.entity

import com.ieum.ieum_back.entity.enums.GenderType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 50)
    var nickname: String? = null,

    @Column(name = "profile_image", length = 500)
    var profileImage: String? = null,

    var birthday: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    var gender: GenderType? = null,

    @Column(name = "google_id", unique = true)
    var googleId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    var couple: Couple? = null,

    @Column(name = "mbti_type", length = 4)
    var mbtiType: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mbti_answers", columnDefinition = "json")
    var mbtiAnswers: Map<String, Any>? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
