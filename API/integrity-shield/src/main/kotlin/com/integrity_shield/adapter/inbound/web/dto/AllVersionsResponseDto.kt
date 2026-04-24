package com.integrity_shield.adapter.inbound.web.dto

data class AllVersionsResponseDto(
    val count: Int,
    val versions: List<FileVersionDetailDto>
)

