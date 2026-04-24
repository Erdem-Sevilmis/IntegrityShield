package com.integrity_shield.domain.model

import java.time.Instant

data class DomainFileVersion(
    val id: Long = 0,
    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String,
    val uploadTimestamp: Instant,
    val metadata: String? = null,
    val leafHash: String,
    val merkleLeafIndex: Long,
    val auditLogTimestamp: Instant = Instant.now()
)

