package com.integrity_shield.adapter.inbound.web.dto

data class AnchorVerificationResponseDto(
    val rootHash: String,
    val transactionHash: String,
    val isValid: Boolean,
    val message: String
)

