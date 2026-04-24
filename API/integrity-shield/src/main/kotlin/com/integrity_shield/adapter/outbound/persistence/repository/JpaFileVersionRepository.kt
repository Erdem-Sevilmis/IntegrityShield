package com.integrity_shield.adapter.outbound.persistence.repository

import com.integrity_shield.adapter.outbound.persistence.entity.FileVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaFileVersionRepository : JpaRepository<FileVersionEntity, Long> {
    fun findByFileIdentifierAndStorageVersionId(
        fileIdentifier: String,
        storageVersionId: String
    ): FileVersionEntity?

    fun findByFileIdentifierOrderByMerkleLeafIndexAsc(fileIdentifier: String): List<FileVersionEntity>

    fun findByLeafHash(leafHash: String): FileVersionEntity?


    fun findByContentHash(contentHash: String): FileVersionEntity?
}

