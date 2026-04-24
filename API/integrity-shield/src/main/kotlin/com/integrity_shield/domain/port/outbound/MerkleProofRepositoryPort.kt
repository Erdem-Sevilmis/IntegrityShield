package com.integrity_shield.domain.port.outbound

import com.integrity_shield.domain.model.DomainMerkleProofData

interface MerkleProofRepositoryPort {
    fun save(proof: DomainMerkleProofData): DomainMerkleProofData
    fun findByFileVersionId(fileVersionId: Long): DomainMerkleProofData?
    fun findByLeafHash(leafHash: String): DomainMerkleProofData?
}

