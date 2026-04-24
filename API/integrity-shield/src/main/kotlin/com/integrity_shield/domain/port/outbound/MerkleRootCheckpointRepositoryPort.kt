package com.integrity_shield.domain.port.outbound

import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint

interface MerkleRootCheckpointRepositoryPort {
    fun save(checkpoint: DomainMerkleRootCheckpoint): DomainMerkleRootCheckpoint
    fun findById(id: Long): DomainMerkleRootCheckpoint?
    fun findLatestCheckpoint(): DomainMerkleRootCheckpoint?
    fun findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc(): List<DomainMerkleRootCheckpoint>
    fun findByRootHash(rootHash: String): DomainMerkleRootCheckpoint?
}

