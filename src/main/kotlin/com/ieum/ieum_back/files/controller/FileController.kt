package com.ieum.ieum_back.files.controller

import com.ieum.ieum_back.files.dto.FileResponse
import com.ieum.ieum_back.files.dto.PresignRequest
import com.ieum.ieum_back.files.dto.PresignResponse
import com.ieum.ieum_back.files.service.FileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileService
) {

    @PostMapping("/presign")
    fun getPresignedUrl(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: PresignRequest
    ): ResponseEntity<PresignResponse> {
        return ResponseEntity.ok(fileService.getPresignedUrl(userId, request))
    }

    @GetMapping("/{fileId}")
    fun getFile(@PathVariable fileId: String): ResponseEntity<FileResponse> {
        return ResponseEntity.ok(fileService.getFile(fileId))
    }
}
