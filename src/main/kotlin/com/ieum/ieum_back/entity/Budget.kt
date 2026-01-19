package com.ieum.ieum_back.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "budgets")
class Budget(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: Couple,

    @Column(name = "year_month", nullable = false, length = 7)
    var yearMonth: String, // "2024-01" 형식

    @Column(name = "total_budget", nullable = false, precision = 12, scale = 2)
    var totalBudget: BigDecimal,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_budgets", columnDefinition = "json")
    var categoryBudgets: Map<String, BigDecimal>? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
