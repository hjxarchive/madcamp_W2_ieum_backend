package com.ieum.ieum_back.entity

import com.ieum.ieum_back.entity.enums.RecommendationStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "recommendations")
class Recommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: Couple,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Enumerated(EnumType.STRING)
    var status: RecommendationStatus = RecommendationStatus.PENDING,

    @Column(name = "location_address", length = 500)
    var locationAddress: String? = null,

    @Column(name = "location_lat", precision = 10, scale = 8)
    var locationLat: BigDecimal? = null,

    @Column(name = "location_lng", precision = 11, scale = 8)
    var locationLng: BigDecimal? = null,

    var date: LocalDate? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var preferences: Map<String, Any>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var result: Map<String, Any>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var feedback: Map<String, Any>? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_event_id")
    var savedEvent: Event? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
)
