package com.ieum.ieum_back.bucket.controller

import com.ieum.ieum_back.bucket.dto.*
import com.ieum.ieum_back.bucket.service.BucketService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/buckets")
class BucketController(
    private val bucketService: BucketService
) {

    @PostMapping
    fun createBucket(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateBucketRequest
    ): ResponseEntity<BucketResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(bucketService.createBucket(userId, request))
    }

    @GetMapping
    fun getBuckets(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<BucketListResponse> {
        return ResponseEntity.ok(bucketService.getBuckets(userId))
    }

    @PatchMapping("/{bucketId}")
    fun updateBucket(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable bucketId: UUID,
        @RequestBody request: UpdateBucketRequest
    ): ResponseEntity<BucketResponse> {
        return ResponseEntity.ok(bucketService.updateBucket(userId, bucketId, request))
    }

    @DeleteMapping("/{bucketId}")
    fun deleteBucket(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable bucketId: UUID
    ): ResponseEntity<Void> {
        bucketService.deleteBucket(userId, bucketId)
        return ResponseEntity.noContent().build()
    }
}
