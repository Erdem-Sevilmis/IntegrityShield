package com.integrity_shield.adapter.inbound.web.dto

data class VerificationRequestDto(
    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String,
    val uploadTimestamp: String,
    val knownRoot: String
)

