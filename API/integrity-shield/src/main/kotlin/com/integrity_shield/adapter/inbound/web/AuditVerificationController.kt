package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.adapter.inbound.web.dto.*
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/audit")
class AuditVerificationController(
    private val auditVerificationUseCase: AuditVerificationUseCase
) {

    @GetMapping("/current-root")
    fun getCurrentRoot(): ResponseEntity<MerkleRootResponseDto> {
        return ResponseEntity.ok(MerkleRootResponseDto(
            currentRoot = auditVerificationUseCase.getCurrentRoot(),
            leafCount = auditVerificationUseCase.getCurrentLeafCount()
        ))
    }

    @PostMapping("/verify-inclusion")
    fun verifyFileInclusion(
        @RequestBody request: VerificationRequestDto
    ): ResponseEntity<VerificationResponseDto> {
        val verified = auditVerificationUseCase.verifyFileInclusion(
            fileIdentifier = request.fileIdentifier,
            storageVersionId = request.storageVersionId,
            contentHash = request.contentHash,
            uploadTimestamp = Instant.parse(request.uploadTimestamp),
            knownRoot = request.knownRoot
        )

        return ResponseEntity.ok(VerificationResponseDto(
            verified = verified,
            fileIdentifier = request.fileIdentifier,
            storageVersionId = request.storageVersionId,
            contentHash = request.contentHash
        ))
    }

    @GetMapping("/proof/{fileVersionId}")
    fun getProof(@PathVariable fileVersionId: Long): ResponseEntity<MerkleProofResponseDto> {
        val proof = auditVerificationUseCase.getProofForFileVersion(fileVersionId)
            ?: return ResponseEntity.notFound().build()

        val proofSteps = proof.proofPath.map { element ->
            ProofStepDto(
                siblingHash = element.siblingHash,
                position = element.position.name
            )
        }

        return ResponseEntity.ok(MerkleProofResponseDto(
            leafHash = proof.leafHash,
            leafIndex = proof.leafIndex,
            proofPath = proofSteps,
            rootHash = proof.rootHash,
            generatedAt = proof.generatedAt
        ))
    }

    @GetMapping("/all-versions")
    fun getAllFileVersions(): ResponseEntity<AllVersionsResponseDto> {
        val versions = auditVerificationUseCase.getAllFileVersions()
        return ResponseEntity.ok(AllVersionsResponseDto(
            count = versions.size,
            versions = versions.map {
                FileVersionDetailDto(
                    id = it.id,
                    fileIdentifier = it.fileIdentifier,
                    storageVersionId = it.storageVersionId,
                    contentHash = it.contentHash,
                    leafIndex = it.merkleLeafIndex,
                    uploadTimestamp = it.uploadTimestamp
                )
            }
        ))
    }

    @GetMapping("/versions/{fileIdentifier}")
    fun getFileVersions(
        @PathVariable fileIdentifier: String
    ): ResponseEntity<FileVersionsResponseDto> {
        val versions = auditVerificationUseCase.getFileVersions(fileIdentifier)
        return ResponseEntity.ok(FileVersionsResponseDto(
            fileIdentifier = fileIdentifier,
            count = versions.size,
            versions = versions.map {
                FileVersionDetailDto(
                    id = it.id,
                    fileIdentifier = it.fileIdentifier,
                    storageVersionId = it.storageVersionId,
                    contentHash = it.contentHash,
                    leafIndex = it.merkleLeafIndex,
                    uploadTimestamp = it.uploadTimestamp
                )
            }
        ))
    }
}

