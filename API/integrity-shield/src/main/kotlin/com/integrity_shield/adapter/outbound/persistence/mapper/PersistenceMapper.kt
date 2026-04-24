package com.integrity_shield.adapter.outbound.persistence.mapper

import com.integrity_shield.adapter.outbound.persistence.entity.FileVersionEntity
import com.integrity_shield.adapter.outbound.persistence.entity.MerkleProofEntity
import com.integrity_shield.adapter.outbound.persistence.entity.MerkleRootCheckpointEntity
import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.DomainMerkleProofData
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.model.ProofElement
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

object PersistenceMapper {

    private val objectMapper = jacksonObjectMapper()

    // FileVersion
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

    // MerkleProof
    fun MerkleProofEntity.toDomain(): DomainMerkleProofData {
        val proofPath: List<ProofElement> = if (proofPathJson.isBlank() || proofPathJson == "[]") {
            emptyList()
        } else {
            objectMapper.readValue(proofPathJson)
        }
        return DomainMerkleProofData(
            id = id,
            leafHash = leafHash,
            leafIndex = leafIndex,
            proofPath = proofPath,
            rootHash = rootHash,
            generatedAt = generatedAt,
            fileVersionId = fileVersionId
        )
    }

    fun DomainMerkleProofData.toEntity(): MerkleProofEntity = MerkleProofEntity(
        id = id,
        leafHash = leafHash,
        leafIndex = leafIndex,
        proofPathJson = objectMapper.writeValueAsString(proofPath),
        rootHash = rootHash,
        generatedAt = generatedAt,
        fileVersionId = fileVersionId
    )

    // MerkleRootCheckpoint
    fun MerkleRootCheckpointEntity.toDomain(): DomainMerkleRootCheckpoint = DomainMerkleRootCheckpoint(
        id = id,
        rootHash = rootHash,
        leafCount = leafCount,
        checkpointTime = checkpointTime,
        blockchainAnchorId = blockchainAnchorId,
        transactionHash = transactionHash,
        blockNumber = blockNumber,
        networkName = networkName,
        anchoredAt = anchoredAt,
        gasUsed = gasUsed
    )

    fun DomainMerkleRootCheckpoint.toEntity(): MerkleRootCheckpointEntity = MerkleRootCheckpointEntity(
        id = id,
        rootHash = rootHash,
        leafCount = leafCount,
        checkpointTime = checkpointTime,
        blockchainAnchorId = blockchainAnchorId,
        transactionHash = transactionHash,
        blockNumber = blockNumber,
        networkName = networkName,
        anchoredAt = anchoredAt,
        gasUsed = gasUsed
    )
}

