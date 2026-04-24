package com.integrity_shield.adapter.inbound.web.dto

data class ErrorResponseDto(
    val error: String,
    val message: String? = null
)

