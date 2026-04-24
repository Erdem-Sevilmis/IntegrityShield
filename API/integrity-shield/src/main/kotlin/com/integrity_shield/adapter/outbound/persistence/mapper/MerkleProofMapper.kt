package com.integrity_shield.adapter.outbound.persistence.mapper

import com.integrity_shield.adapter.outbound.persistence.entity.MerkleProofEntity
import com.integrity_shield.domain.model.DomainMerkleProofData
import com.integrity_shield.domain.model.ProofElement
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

object MerkleProofMapper {

    private val objectMapper = jacksonObjectMapper()

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
}

