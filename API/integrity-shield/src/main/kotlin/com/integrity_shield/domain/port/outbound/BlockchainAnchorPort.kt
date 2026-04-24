package com.integrity_shield.domain.port.outbound

import com.integrity_shield.domain.model.AnchorTransaction
import java.time.Instant

interface BlockchainAnchorPort {
    fun anchorRoot(rootHash: String, timestamp: Instant): AnchorTransaction?
    fun verifyAnchor(rootHash: String, transactionHash: String): Boolean
    fun getNetworkName(): String
}

