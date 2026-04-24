package com.integrity_shield.domain.port.inbound

import com.integrity_shield.domain.model.AnchoringResult
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint

interface BlockchainAnchoringUseCase {
    fun anchorCurrentRoot(): AnchoringResult?
    fun getCurrentRootForAnchoring(): String
    fun getLatestAnchoredCheckpoint(): DomainMerkleRootCheckpoint?
    fun verifyAnchor(rootHash: String, transactionHash: String): Boolean
    fun getLastAnchoringResult(): AnchoringResult?
}

