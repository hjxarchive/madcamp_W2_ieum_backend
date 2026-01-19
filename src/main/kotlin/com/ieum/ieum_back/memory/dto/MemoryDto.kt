package com.ieum.ieum_back.memory.dto

import com.ieum.ieum_back.entity.Memory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class CreateMemoryRequest(
    val title: String,
    val content: String? = null,
    val date: LocalDate,
    val location: String? = null,
    val images: List<String>? = null
)

data class UpdateMemoryRequest(
    val title: String? = null,
    val content: String? = null,
    val date: LocalDate? = null,
    val location: String? = null,
    val images: List<String>? = null
)

data class MemoryResponse(
    val id: UUID,
    val title: String,
    val content: String?,
    val date: LocalDate,
    val location: String?,
    val images: List<String>?,
    val createdById: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(memory: Memory): MemoryResponse = MemoryResponse(
            id = memory.id!!,
            title = memory.title,
            content = memory.content,
            date = memory.date,
            location = memory.location,
            images = memory.images,
            createdById = memory.createdBy.id!!,
            createdAt = memory.createdAt
        )
    }
}

data class MemoryListResponse(
    val memories: List<MemoryResponse>,
    val totalCount: Long,
    val page: Int,
    val size: Int
)
