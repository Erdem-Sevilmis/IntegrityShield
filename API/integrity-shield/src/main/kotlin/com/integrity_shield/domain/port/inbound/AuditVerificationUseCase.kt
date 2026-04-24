package com.integrity_shield.domain.port.inbound

import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.DomainMerkleProofData
import java.time.Instant

interface AuditVerificationUseCase {
    fun appendFileVersion(
        fileIdentifier: String,
        storageVersionId: String,
        contentHash: String,
        uploadTimestamp: Instant
    ): DomainFileVersion?

    fun createRootCheckpoint(): com.integrity_shield.domain.model.DomainMerkleRootCheckpoint

    fun recordBlockchainAnchor(
        checkpointId: Long,
        anchorId: String,
        transactionHash: String? = null,
        blockNumber: Long? = null,
        networkName: String? = null,
        gasUsed: java.math.BigInteger? = null
    ): com.integrity_shield.domain.model.DomainMerkleRootCheckpoint?

    fun getCurrentRoot(): String
    fun getCurrentLeafCount(): Long

    fun verifyFileInclusion(
        fileIdentifier: String,
        storageVersionId: String,
        contentHash: String,
        uploadTimestamp: Instant,
        knownRoot: String
    ): Boolean

    fun getProofForFileVersion(fileVersionId: Long): DomainMerkleProofData?

    fun getLatestAnchoredCheckpoint(): com.integrity_shield.domain.model.DomainMerkleRootCheckpoint?

    fun reconstructTreeFromPersistence()

    fun getAllFileVersions(): List<DomainFileVersion>
    fun getFileVersions(fileIdentifier: String): List<DomainFileVersion>
}

