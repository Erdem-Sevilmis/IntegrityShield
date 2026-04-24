package com.integrity_shield.adapter.outbound.persistence.repository

import com.integrity_shield.adapter.outbound.persistence.entity.MerkleProofEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaMerkleProofRepository : JpaRepository<MerkleProofEntity, Long> {
    fun findByFileVersionId(fileVersionId: Long): MerkleProofEntity?

    fun findByLeafHash(leafHash: String): MerkleProofEntity?
}

