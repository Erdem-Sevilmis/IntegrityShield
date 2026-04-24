package com.integrity_shield.adapter.inbound.web.dto

data class FileVersionsResponseDto(
    val fileIdentifier: String,
    val count: Int,
    val versions: List<FileVersionDetailDto>
)

