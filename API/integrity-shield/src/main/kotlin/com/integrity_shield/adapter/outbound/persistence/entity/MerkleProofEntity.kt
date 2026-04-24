package com.integrity_shield.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "merkle_proofs")
data class MerkleProofEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val leafHash: String,
    val leafIndex: Long,

    @Column(columnDefinition = "TEXT")
    val proofPathJson: String = "[]",

    val rootHash: String,
    val generatedAt: Instant = Instant.now(),
    val fileVersionId: Long
) {
    init {
        require(leafHash.isNotBlank()) { "leafHash must not be blank" }
        require(rootHash.isNotBlank()) { "rootHash must not be blank" }
    }
}

