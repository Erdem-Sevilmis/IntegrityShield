package com.integrity_shield.adapter.outbound.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigInteger
import java.time.Instant

@Entity
@Table(name = "merkle_root_checkpoints")
data class MerkleRootCheckpointEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
) {
    init {
        require(rootHash.isNotBlank()) { "rootHash must not be blank" }
        require(leafCount >= 0) { "leafCount must be non-negative" }
    }
}

