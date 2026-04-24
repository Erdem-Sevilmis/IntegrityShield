package com.integrity_shield.domain.port.outbound

import com.integrity_shield.domain.model.DomainFileVersion

interface FileVersionRepositoryPort {
    fun save(fileVersion: DomainFileVersion): DomainFileVersion
    fun findById(id: Long): DomainFileVersion?
    fun findByFileIdentifierAndStorageVersionId(fileIdentifier: String, storageVersionId: String): DomainFileVersion?
    fun findByLeafHash(leafHash: String): DomainFileVersion?
    fun findByContentHash(contentHash: String): DomainFileVersion?
    fun findByFileIdentifierOrderByMerkleLeafIndexAsc(fileIdentifier: String): List<DomainFileVersion>
    fun findAll(): List<DomainFileVersion>
}

