package com.integrity_shield.domain.model

import java.time.Instant

data class DomainMerkleProofData(
    val id: Long = 0,
    val leafHash: String,
    val leafIndex: Long,
    val proofPath: List<ProofElement> = emptyList(),
    val rootHash: String,
    val generatedAt: Instant = Instant.now(),
    val fileVersionId: Long
)

