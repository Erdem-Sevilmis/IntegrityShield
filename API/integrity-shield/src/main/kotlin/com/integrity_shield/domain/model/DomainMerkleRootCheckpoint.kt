package com.integrity_shield.domain.model

import java.math.BigInteger
import java.time.Instant

data class DomainMerkleRootCheckpoint(
    val id: Long = 0,
    val rootHash: String,
    val leafCount: Long,
    val checkpointTime: Instant = Instant.now(),
    val blockchainAnchorId: String? = null,
    val transactionHash: String? = null,
    val blockNumber: Long? = null,
    val networkName: String? = null,
    val anchoredAt: Instant? = null,
    val gasUsed: BigInteger? = null
)

