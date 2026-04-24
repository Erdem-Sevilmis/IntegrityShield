package com.integrity_shield.adapter.outbound.persistence

import com.integrity_shield.adapter.outbound.persistence.mapper.MerkleProofMapper.toDomain
import com.integrity_shield.adapter.outbound.persistence.mapper.MerkleProofMapper.toEntity
import com.integrity_shield.adapter.outbound.persistence.repository.JpaMerkleProofRepository
import com.integrity_shield.domain.model.DomainMerkleProofData
import com.integrity_shield.domain.port.outbound.MerkleProofRepositoryPort
import org.springframework.stereotype.Service

@Service
class MerkleProofRepositoryAdapter(
    private val jpaRepository: JpaMerkleProofRepository
) : MerkleProofRepositoryPort {

    override fun save(proof: DomainMerkleProofData): DomainMerkleProofData {
        val entity = proof.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByFileVersionId(fileVersionId: Long): DomainMerkleProofData? {
        return jpaRepository.findByFileVersionId(fileVersionId)?.toDomain()
    }

    override fun findByLeafHash(leafHash: String): DomainMerkleProofData? {
        return jpaRepository.findByLeafHash(leafHash)?.toDomain()
    }
}

