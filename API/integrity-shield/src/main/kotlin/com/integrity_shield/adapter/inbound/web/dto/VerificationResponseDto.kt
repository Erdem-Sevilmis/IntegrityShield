package com.integrity_shield.adapter.inbound.web.dto

data class VerificationResponseDto(
    val verified: Boolean,
    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String
)

