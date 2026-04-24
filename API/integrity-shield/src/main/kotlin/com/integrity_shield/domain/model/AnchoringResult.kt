package com.integrity_shield.domain.model

import java.time.Instant

data class AnchoringResult(
    val checkpointId: Long,
    val rootHash: String,
    val transactionHash: String,
    val blockNumber: Long,
    val anchorId: String?,
    val anchoredAt: Instant,
    val networkName: String
)

