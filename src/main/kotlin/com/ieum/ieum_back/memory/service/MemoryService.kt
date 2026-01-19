package com.ieum.ieum_back.memory.service

import com.ieum.ieum_back.entity.Memory
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.memory.dto.*
import com.ieum.ieum_back.repository.MemoryRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class MemoryService(
    private val memoryRepository: MemoryRepository,
    private val userRepository: UserRepository
) {

    fun createMemory(userId: UUID, request: CreateMemoryRequest): MemoryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val memory = Memory(
            couple = couple,
            createdBy = user,
            title = request.title,
            content = request.content,
            date = request.date,
            location = request.location,
            images = request.images
        )

        return MemoryResponse.from(memoryRepository.save(memory))
    }

    @Transactional(readOnly = true)
    fun getMemories(userId: UUID, page: Int, size: Int): MemoryListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val pageable = PageRequest.of(page, size)
        val memoryPage = memoryRepository.findByCoupleAndDeletedAtIsNullOrderByDateDesc(couple, pageable)

        return MemoryListResponse(
            memories = memoryPage.content.map { MemoryResponse.from(it) },
            totalCount = memoryPage.totalElements,
            page = page,
            size = size
        )
    }

    @Transactional(readOnly = true)
    fun getMemory(userId: UUID, memoryId: UUID): MemoryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val memory = memoryRepository.findByIdAndCoupleAndDeletedAtIsNull(memoryId, couple)
            ?: throw NotFoundException("Memory not found")

        return MemoryResponse.from(memory)
    }

    fun updateMemory(userId: UUID, memoryId: UUID, request: UpdateMemoryRequest): MemoryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val memory = memoryRepository.findByIdAndCoupleAndDeletedAtIsNull(memoryId, couple)
            ?: throw NotFoundException("Memory not found")

        request.title?.let { memory.title = it }
        request.content?.let { memory.content = it }
        request.date?.let { memory.date = it }
        request.location?.let { memory.location = it }
        request.images?.let { memory.images = it }
        memory.updatedAt = LocalDateTime.now()

        return MemoryResponse.from(memoryRepository.save(memory))
    }

    fun deleteMemory(userId: UUID, memoryId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val memory = memoryRepository.findByIdAndCoupleAndDeletedAtIsNull(memoryId, couple)
            ?: throw NotFoundException("Memory not found")

        memory.deletedAt = LocalDateTime.now()
        memoryRepository.save(memory)
    }
}
