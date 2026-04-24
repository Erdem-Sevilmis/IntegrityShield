package com.integrity_shield.adapter.inbound.web.dto

import java.time.Instant

data class MerkleProofResponseDto(
    val leafHash: String,
    val leafIndex: Long,
    val proofPath: List<ProofStepDto>,
    val rootHash: String,
    val generatedAt: Instant
)

