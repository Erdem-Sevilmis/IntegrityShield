package com.integrity_shield.adapter.outbound.persistence

import com.integrity_shield.adapter.outbound.persistence.mapper.FileVersionMapper.toDomain
import com.integrity_shield.adapter.outbound.persistence.mapper.FileVersionMapper.toEntity
import com.integrity_shield.adapter.outbound.persistence.repository.JpaFileVersionRepository
import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.port.outbound.FileVersionRepositoryPort
import org.springframework.stereotype.Service

@Service
class FileVersionRepositoryAdapter(
    private val jpaRepository: JpaFileVersionRepository
) : FileVersionRepositoryPort {

    override fun save(fileVersion: DomainFileVersion): DomainFileVersion {
        val entity = fileVersion.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): DomainFileVersion? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByFileIdentifierAndStorageVersionId(
        fileIdentifier: String,
        storageVersionId: String
    ): DomainFileVersion? {
        return jpaRepository.findByFileIdentifierAndStorageVersionId(fileIdentifier, storageVersionId)?.toDomain()
    }

    override fun findByLeafHash(leafHash: String): DomainFileVersion? {
        return jpaRepository.findByLeafHash(leafHash)?.toDomain()
    }

    override fun findByContentHash(contentHash: String): DomainFileVersion? {
        return jpaRepository.findByContentHash(contentHash)?.toDomain()
    }

    override fun findByFileIdentifierOrderByMerkleLeafIndexAsc(fileIdentifier: String): List<DomainFileVersion> {
        return jpaRepository.findByFileIdentifierOrderByMerkleLeafIndexAsc(fileIdentifier).map { it.toDomain() }
    }

    override fun findAll(): List<DomainFileVersion> {
        return jpaRepository.findAll().map { it.toDomain() }
    }
}

