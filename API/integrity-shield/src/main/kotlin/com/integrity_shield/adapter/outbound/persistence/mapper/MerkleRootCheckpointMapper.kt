package com.integrity_shield.adapter.outbound.persistence.mapper

import com.integrity_shield.adapter.outbound.persistence.entity.MerkleRootCheckpointEntity
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint

object MerkleRootCheckpointMapper {

    fun MerkleRootCheckpointEntity.toDomain(): DomainMerkleRootCheckpoint = DomainMerkleRootCheckpoint(
        id = id,
        rootHash = rootHash,
        leafCount = leafCount,
        checkpointTime = checkpointTime,
        blockchainAnchorId = blockchainAnchorId,
        transactionHash = transactionHash,
        blockNumber = blockNumber,
        networkName = networkName,
        anchoredAt = anchoredAt,
        gasUsed = gasUsed
    )

    fun DomainMerkleRootCheckpoint.toEntity(): MerkleRootCheckpointEntity = MerkleRootCheckpointEntity(
        id = id,
        rootHash = rootHash,
        leafCount = leafCount,
        checkpointTime = checkpointTime,
        blockchainAnchorId = blockchainAnchorId,
        transactionHash = transactionHash,
        blockNumber = blockNumber,
        networkName = networkName,
        anchoredAt = anchoredAt,
        gasUsed = gasUsed
    )
}

