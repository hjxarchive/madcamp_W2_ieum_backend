package com.ieum.ieum_back.entity

import jakarta.persistence.*

@Entity
@Table(name = "users") // PostgreSQL의 users 테이블과 매핑
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var nickname: String
)