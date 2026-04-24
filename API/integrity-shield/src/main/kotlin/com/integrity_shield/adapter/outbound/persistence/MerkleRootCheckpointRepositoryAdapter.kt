package com.integrity_shield.adapter.outbound.persistence

import com.integrity_shield.adapter.outbound.persistence.mapper.MerkleRootCheckpointMapper.toDomain
import com.integrity_shield.adapter.outbound.persistence.mapper.MerkleRootCheckpointMapper.toEntity
import com.integrity_shield.adapter.outbound.persistence.repository.JpaMerkleRootCheckpointRepository
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.port.outbound.MerkleRootCheckpointRepositoryPort
import org.springframework.stereotype.Service

@Service
class MerkleRootCheckpointRepositoryAdapter(
    private val jpaRepository: JpaMerkleRootCheckpointRepository
) : MerkleRootCheckpointRepositoryPort {

    override fun save(checkpoint: DomainMerkleRootCheckpoint): DomainMerkleRootCheckpoint {
        val entity = checkpoint.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): DomainMerkleRootCheckpoint? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findLatestCheckpoint(): DomainMerkleRootCheckpoint? {
        return jpaRepository.findLatestCheckpoint()?.toDomain()
    }

    override fun findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc(): List<DomainMerkleRootCheckpoint> {
        return jpaRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc().map { it.toDomain() }
    }

    override fun findByRootHash(rootHash: String): DomainMerkleRootCheckpoint? {
        return jpaRepository.findByRootHash(rootHash)?.toDomain()
    }
}

