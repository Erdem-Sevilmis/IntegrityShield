package com.integrity_shield.adapter.outbound.persistence.mapper

import com.integrity_shield.adapter.outbound.persistence.entity.FileVersionEntity
import com.integrity_shield.domain.model.DomainFileVersion

object FileVersionMapper {

    fun FileVersionEntity.toDomain(): DomainFileVersion = DomainFileVersion(
        id = id,
        fileIdentifier = fileIdentifier,
        storageVersionId = storageVersionId,
        contentHash = contentHash,
        uploadTimestamp = uploadTimestamp,
        metadata = metadata,
        leafHash = leafHash,
        merkleLeafIndex = merkleLeafIndex,
        auditLogTimestamp = auditLogTimestamp
    )

    fun DomainFileVersion.toEntity(): FileVersionEntity = FileVersionEntity(
        id = id,
        fileIdentifier = fileIdentifier,
        storageVersionId = storageVersionId,
        contentHash = contentHash,
        uploadTimestamp = uploadTimestamp,
        metadata = metadata,
        leafHash = leafHash,
        merkleLeafIndex = merkleLeafIndex,
        auditLogTimestamp = auditLogTimestamp
    )
}


