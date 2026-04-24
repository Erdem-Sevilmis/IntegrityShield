package com.integrity_shield.application

import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.port.outbound.FileVersionRepositoryPort
import com.integrity_shield.domain.port.outbound.MerkleProofRepositoryPort
import com.integrity_shield.domain.port.outbound.MerkleRootCheckpointRepositoryPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class AuditVerificationUseCaseImplUT {

    private val fileVersionRepository: FileVersionRepositoryPort = mock()
    private val merkleProofRepository: MerkleProofRepositoryPort = mock()
    private val merkleRootCheckpointRepository: MerkleRootCheckpointRepositoryPort = mock()

    private val useCase = AuditVerificationUseCaseImpl(
        fileVersionRepository,
        merkleProofRepository,
        merkleRootCheckpointRepository
    )

    @BeforeEach
    fun init() {
        reset(fileVersionRepository, merkleProofRepository, merkleRootCheckpointRepository)
    }

    @Test
    fun `GIVEN new file WHEN appendFileVersion THEN saves file version and merkle proof`() {
        whenever(fileVersionRepository.findByContentHash(ANY_CONTENT_HASH)).thenReturn(null)
        whenever(fileVersionRepository.findByFileIdentifierAndStorageVersionId(ANY_FILE_ID, ANY_VERSION_ID)).thenReturn(null)
        whenever(fileVersionRepository.save(any())).thenAnswer { it.arguments[0] as DomainFileVersion }
        whenever(merkleProofRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = useCase.appendFileVersion(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        assertSoftly {
            it.assertThat(result).isNotNull
            it.assertThat(result!!.fileIdentifier).isEqualTo(ANY_FILE_ID)
            it.assertThat(result.merkleLeafIndex).isEqualTo(0L)
        }
        verify(fileVersionRepository).save(any())
        verify(merkleProofRepository).save(any())
    }

    @Test
    fun `GIVEN duplicate content hash WHEN appendFileVersion THEN returns null`() {
        whenever(fileVersionRepository.findByContentHash(ANY_CONTENT_HASH)).thenReturn(ANY_DOMAIN_FILE_VERSION)

        val result = useCase.appendFileVersion(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        assertThat(result).isNull()
        verify(fileVersionRepository, never()).save(any())
    }

    @Test
    fun `GIVEN duplicate file identifier and version WHEN appendFileVersion THEN returns null`() {
        whenever(fileVersionRepository.findByContentHash(ANY_CONTENT_HASH)).thenReturn(null)
        whenever(fileVersionRepository.findByFileIdentifierAndStorageVersionId(ANY_FILE_ID, ANY_VERSION_ID))
            .thenReturn(ANY_DOMAIN_FILE_VERSION)

        val result = useCase.appendFileVersion(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN empty tree WHEN createRootCheckpoint THEN saves checkpoint with current root`() {
        whenever(merkleRootCheckpointRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = useCase.createRootCheckpoint()

        assertSoftly {
            it.assertThat(result.rootHash).isNotBlank()
            it.assertThat(result.leafCount).isEqualTo(0L)
        }
        verify(merkleRootCheckpointRepository).save(any())
    }

    @Test
    fun `GIVEN valid checkpoint WHEN recordBlockchainAnchor THEN updates checkpoint with anchor data`() {
        val checkpoint = DomainMerkleRootCheckpoint(id = 1L, rootHash = ANY_ROOT, leafCount = 1L)
        whenever(merkleRootCheckpointRepository.findById(1L)).thenReturn(checkpoint)
        whenever(merkleRootCheckpointRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = useCase.recordBlockchainAnchor(
            checkpointId = 1L,
            anchorId = ANY_ANCHOR_ID,
            transactionHash = ANY_TX_HASH,
            blockNumber = ANY_BLOCK_NUMBER,
            networkName = ANY_NETWORK
        )

        assertSoftly {
            it.assertThat(result).isNotNull
            it.assertThat(result!!.blockchainAnchorId).isEqualTo(ANY_ANCHOR_ID)
            it.assertThat(result.transactionHash).isEqualTo(ANY_TX_HASH)
            it.assertThat(result.blockNumber).isEqualTo(ANY_BLOCK_NUMBER)
        }
    }

    @Test
    fun `GIVEN unknown checkpoint WHEN recordBlockchainAnchor THEN returns null`() {
        whenever(merkleRootCheckpointRepository.findById(999L)).thenReturn(null)

        val result = useCase.recordBlockchainAnchor(checkpointId = 999L, anchorId = ANY_ANCHOR_ID)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN no anchored checkpoints WHEN getLatestAnchoredCheckpoint THEN returns null`() {
        whenever(merkleRootCheckpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc())
            .thenReturn(emptyList())

        val result = useCase.getLatestAnchoredCheckpoint()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN persisted versions WHEN getAllFileVersions THEN returns all`() {
        whenever(fileVersionRepository.findAll()).thenReturn(listOf(ANY_DOMAIN_FILE_VERSION))

        val result = useCase.getAllFileVersions()

        assertThat(result).hasSize(1)
    }
}

private const val ANY_FILE_ID = "document.pdf"
private const val ANY_VERSION_ID = "v1-abc123"
private const val ANY_CONTENT_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
private const val ANY_ROOT = "rootHash123"
private const val ANY_ANCHOR_ID = "anchor-001"
private const val ANY_TX_HASH = "0xabc123"
private const val ANY_BLOCK_NUMBER = 12345L
private const val ANY_NETWORK = "sepolia"
private val FIXED_TIMESTAMP = Instant.now()
private val ANY_DOMAIN_FILE_VERSION = DomainFileVersion(
    id = 1L,
    fileIdentifier = ANY_FILE_ID,
    storageVersionId = ANY_VERSION_ID,
    contentHash = ANY_CONTENT_HASH,
    uploadTimestamp = FIXED_TIMESTAMP,
    leafHash = "leafHash",
    merkleLeafIndex = 0L
)

