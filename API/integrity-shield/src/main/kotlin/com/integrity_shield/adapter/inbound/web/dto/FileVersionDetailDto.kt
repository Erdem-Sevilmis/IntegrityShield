package com.integrity_shield.adapter.inbound.web.dto

import java.time.Instant

data class FileVersionDetailDto(
    val id: Long,
    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String,
    val leafIndex: Long,
    val uploadTimestamp: Instant? = null
)

