package com.integrity_shield.adapter.outbound.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "file_versions")
data class FileVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val fileIdentifier: String,
    val storageVersionId: String,
    val contentHash: String,
    val uploadTimestamp: Instant,
    val metadata: String? = null,
    val leafHash: String,
    val merkleLeafIndex: Long,
    val auditLogTimestamp: Instant = Instant.now()
) {
    init {
        require(fileIdentifier.isNotBlank()) { "fileIdentifier must not be blank" }
        require(storageVersionId.isNotBlank()) { "storageVersionId must not be blank" }
        require(contentHash.isNotBlank()) { "contentHash must not be blank" }
        require(leafHash.isNotBlank()) { "leafHash must not be blank" }
    }
}

