package com.integrity_shield.adapter.inbound.web.dto

import java.time.Instant

data class AnchoringResultDto(
    val checkpointId: Long,
    val rootHash: String,
    val transactionHash: String,
    val blockNumber: Long,
    val anchoredAt: Instant,
    val networkName: String
)

