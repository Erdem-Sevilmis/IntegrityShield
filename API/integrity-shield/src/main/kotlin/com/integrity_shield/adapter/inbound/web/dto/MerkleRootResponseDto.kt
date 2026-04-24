package com.integrity_shield.adapter.inbound.web.dto

data class MerkleRootResponseDto(
    val currentRoot: String,
    val leafCount: Long
)

