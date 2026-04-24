package com.integrity_shield.domain.model

import java.math.BigInteger
import java.time.Instant

data class AnchorTransaction(
    val transactionHash: String,
    val blockNumber: Long,
    val anchorId: String = transactionHash,
    val timestamp: Instant = Instant.now(),
    val gasUsed: BigInteger? = null
)

