package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Couple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoupleRepository : JpaRepository<Couple, UUID> {
    fun findByInviteCode(inviteCode: String): Couple?
    fun findByUser1IdOrUser2Id(user1Id: UUID, user2Id: UUID): Couple?
}
