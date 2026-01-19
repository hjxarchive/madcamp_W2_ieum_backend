package com.ieum.ieum_back.files.dto

data class PresignRequest(
    val filename: String,
    val contentType: String
)

data class PresignResponse(
    val fileId: String,
    val uploadUrl: String,
    val fileUrl: String,
    val expiresIn: Int // seconds
)

data class FileResponse(
    val fileId: String,
    val url: String,
    val filename: String,
    val contentType: String
)
