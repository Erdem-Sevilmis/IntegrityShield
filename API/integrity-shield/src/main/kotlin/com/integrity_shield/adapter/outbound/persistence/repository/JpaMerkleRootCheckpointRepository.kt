package com.integrity_shield.adapter.outbound.persistence.repository

import com.integrity_shield.adapter.outbound.persistence.entity.MerkleRootCheckpointEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JpaMerkleRootCheckpointRepository : JpaRepository<MerkleRootCheckpointEntity, Long> {
    @Query("SELECT m FROM MerkleRootCheckpointEntity m ORDER BY m.checkpointTime DESC LIMIT 1")
    fun findLatestCheckpoint(): MerkleRootCheckpointEntity?


    fun findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc(): List<MerkleRootCheckpointEntity>

    fun findByRootHash(rootHash: String): MerkleRootCheckpointEntity?
}

