package com.integrity_shield.adapter.inbound.web.dto

import java.time.Instant

data class UploadResponseDto(
    val id: Long,
    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String,
    val leafIndex: Long,
    val uploadTimestamp: Instant,
    val currentRoot: String,
    val leafCount: Long,
    val message: String,
    val status: String? = null
)

