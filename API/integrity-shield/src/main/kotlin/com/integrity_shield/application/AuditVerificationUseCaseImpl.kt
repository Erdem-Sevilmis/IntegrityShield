package com.integrity_shield.application

import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.DomainMerkleProofData
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.model.ProofElement
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import com.integrity_shield.domain.port.outbound.FileVersionRepositoryPort
import com.integrity_shield.domain.port.outbound.MerkleProofRepositoryPort
import com.integrity_shield.domain.port.outbound.MerkleRootCheckpointRepositoryPort
import com.integrity_shield.domain.service.AuditLogService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant

@Service
@Transactional
class AuditVerificationUseCaseImpl(
    private val fileVersionRepository: FileVersionRepositoryPort,
    private val merkleProofRepository: MerkleProofRepositoryPort,
    private val merkleRootCheckpointRepository: MerkleRootCheckpointRepositoryPort
) : AuditVerificationUseCase {

    private val log = LoggerFactory.getLogger(javaClass)
    private var auditLogService: AuditLogService = AuditLogService()

    override fun appendFileVersion(
        fileIdentifier: String,
        storageVersionId: String,
        contentHash: String,
        uploadTimestamp: Instant
    ): DomainFileVersion? {
        // Check for exact duplicate by content hash (idempotent detection)
        val existingByHash = fileVersionRepository.findByContentHash(contentHash)
        if (existingByHash != null) {
            log.info("File with same content already in audit log (idempotent): {}", fileIdentifier)
            return null
        }

        val existing = fileVersionRepository.findByFileIdentifierAndStorageVersionId(
            fileIdentifier, storageVersionId
        )
        if (existing != null) {
            log.info("File version already in audit log: $fileIdentifier / $storageVersionId")
            return null
        }

        val leafHash = auditLogService.deriveLeafHash(
            fileIdentifier = fileIdentifier,
            storageVersionId = storageVersionId,
            contentHash = contentHash,
            uploadTimestamp = uploadTimestamp
        )

        val leafIndex = auditLogService.appendLeaf(leafHash)

        val fileVersion = DomainFileVersion(
            fileIdentifier = fileIdentifier,
            storageVersionId = storageVersionId,
            contentHash = contentHash,
            uploadTimestamp = uploadTimestamp,
            leafHash = leafHash,
            merkleLeafIndex = leafIndex
        )
        val savedVersion = fileVersionRepository.save(fileVersion)

        // Generate and store the REAL proof path from the MerkleTree
        val proofPath: List<ProofElement> = auditLogService.generateProof(leafIndex) ?: emptyList()

        val merkleProof = DomainMerkleProofData(
            leafHash = leafHash,
            leafIndex = leafIndex,
            proofPath = proofPath,
            rootHash = auditLogService.getCurrentRoot(),
            fileVersionId = savedVersion.id
        )
        merkleProofRepository.save(merkleProof)

        log.info("Appended file version to audit log: $fileIdentifier (leaf index: $leafIndex)")
        return savedVersion
    }

    override fun createRootCheckpoint(): DomainMerkleRootCheckpoint {
        val checkpoint = DomainMerkleRootCheckpoint(
            rootHash = auditLogService.getCurrentRoot(),
            leafCount = auditLogService.getCurrentLeafCount()
        )
        val saved = merkleRootCheckpointRepository.save(checkpoint)
        log.info("Created Merkle root checkpoint: ${saved.rootHash} (${saved.leafCount} leaves)")
        return saved
    }

    override fun recordBlockchainAnchor(
        checkpointId: Long,
        anchorId: String,
        transactionHash: String?,
        blockNumber: Long?,
        networkName: String?,
        gasUsed: BigInteger?
    ): DomainMerkleRootCheckpoint? {
        val checkpoint = merkleRootCheckpointRepository.findById(checkpointId)
            ?: return null

        val anchored = checkpoint.copy(
            blockchainAnchorId = anchorId,
            transactionHash = transactionHash,
            blockNumber = blockNumber,
            networkName = networkName,
            anchoredAt = Instant.now(),
            gasUsed = gasUsed
        )
        val saved = merkleRootCheckpointRepository.save(anchored)
        log.info("Recorded blockchain anchor for checkpoint ${checkpoint.rootHash}: $anchorId (tx: $transactionHash, block: $blockNumber, gasUsed: $gasUsed)")
        return saved
    }

    override fun getCurrentRoot(): String = auditLogService.getCurrentRoot()

    override fun getCurrentLeafCount(): Long = auditLogService.getCurrentLeafCount()

    override fun verifyFileInclusion(
        fileIdentifier: String,
        storageVersionId: String,
        contentHash: String,
        uploadTimestamp: Instant,
        knownRoot: String
    ): Boolean {
        val leafHash = auditLogService.deriveLeafHash(
            fileIdentifier = fileIdentifier,
            storageVersionId = storageVersionId,
            contentHash = contentHash,
            uploadTimestamp = uploadTimestamp
        )

        val fileVersion = fileVersionRepository.findByLeafHash(leafHash)
        if (fileVersion == null) {
            log.warn("No file version found for leaf hash: $leafHash")
            return false
        }

        val verified = auditLogService.verifyInclusion(leafHash, fileVersion.merkleLeafIndex, knownRoot)
        if (!verified) {
            log.warn("Merkle inclusion verification failed for leaf $leafHash at index ${fileVersion.merkleLeafIndex}")
        }
        return verified
    }

    override fun getProofForFileVersion(fileVersionId: Long): DomainMerkleProofData? {
        val fileVersion = fileVersionRepository.findById(fileVersionId)
            ?: return null

        val leafIndex = fileVersion.merkleLeafIndex
        val leafHash = fileVersion.leafHash

        val proofPath: List<ProofElement> = auditLogService.generateProof(leafIndex) ?: return null

        return DomainMerkleProofData(
            leafHash = leafHash,
            leafIndex = leafIndex,
            proofPath = proofPath,
            rootHash = auditLogService.getCurrentRoot(),
            generatedAt = Instant.now(),
            fileVersionId = fileVersionId
        )
    }

    override fun getLatestAnchoredCheckpoint(): DomainMerkleRootCheckpoint? {
        val anchored = merkleRootCheckpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc()
        return anchored.firstOrNull()
    }

    override fun reconstructTreeFromPersistence() {
        log.info("Reconstructing Merkle Tree from persisted file versions...")
        val allVersions = fileVersionRepository.findAll()
        auditLogService.reconstructFromFileVersions(allVersions)
        log.info("Reconstructed tree: ${auditLogService.getCurrentLeafCount()} leaves, root = ${auditLogService.getCurrentRoot()}")
    }

    override fun getAllFileVersions(): List<DomainFileVersion> {
        return fileVersionRepository.findAll()
    }

    override fun getFileVersions(fileIdentifier: String): List<DomainFileVersion> {
        return fileVersionRepository.findByFileIdentifierOrderByMerkleLeafIndexAsc(fileIdentifier)
    }
}

