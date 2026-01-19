package com.ieum.ieum_back.files.service

import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.files.dto.FileResponse
import com.ieum.ieum_back.files.dto.PresignRequest
import com.ieum.ieum_back.files.dto.PresignResponse
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class FileService(
    private val userRepository: UserRepository
) {
    @Value("\${app.file.base-url:http://localhost:8080/api/files}")
    private lateinit var baseUrl: String

    fun getPresignedUrl(userId: UUID, request: PresignRequest): PresignResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val fileId = UUID.randomUUID().toString()
        val extension = request.filename.substringAfterLast(".", "")
        val storedFilename = "$fileId.$extension"

        // TODO: 실제 S3/Cloud Storage presigned URL 생성 로직 구현
        // 현재는 로컬 파일 시스템 또는 mock URL 반환

        return PresignResponse(
            fileId = fileId,
            uploadUrl = "$baseUrl/upload/$storedFilename",
            fileUrl = "$baseUrl/$fileId",
            expiresIn = 3600 // 1 hour
        )
    }

    fun getFile(fileId: String): FileResponse {
        // TODO: 실제 파일 스토리지에서 파일 정보 조회 로직 구현
        // 현재는 mock 응답 반환

        return FileResponse(
            fileId = fileId,
            url = "$baseUrl/$fileId",
            filename = "file_$fileId",
            contentType = "image/jpeg"
        )
    }
}
