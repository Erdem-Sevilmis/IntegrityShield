package com.integrity_shield.application

import com.integrity_shield.domain.model.AnchorTransaction
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import com.integrity_shield.domain.port.outbound.BlockchainAnchorPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigInteger
import java.time.Instant

class BlockchainAnchoringUseCaseImplUT {

    private val auditVerificationUseCase: AuditVerificationUseCase = mock()
    private val anchorPort: BlockchainAnchorPort = mock()

    private val useCase = BlockchainAnchoringUseCaseImpl(
        auditVerificationUseCase,
        anchorPort
    )

    @BeforeEach
    fun init() {
        reset(auditVerificationUseCase, anchorPort)
    }

    @Test
    fun `GIVEN valid root WHEN anchorCurrentRoot THEN anchors and returns result`() {
        val checkpoint = DomainMerkleRootCheckpoint(id = 1L, rootHash = ANY_ROOT, leafCount = 2L)
        val anchorTx = AnchorTransaction(transactionHash = ANY_TX_HASH, blockNumber = ANY_BLOCK_NUMBER, gasUsed = BigInteger.valueOf(48801))
        val anchoredCheckpoint = checkpoint.copy(
            blockchainAnchorId = ANY_TX_HASH,
            transactionHash = ANY_TX_HASH,
            blockNumber = ANY_BLOCK_NUMBER,
            networkName = ANY_NETWORK,
            anchoredAt = FIXED_TIMESTAMP
        )
        whenever(auditVerificationUseCase.createRootCheckpoint()).thenReturn(checkpoint)
        whenever(anchorPort.anchorRoot(eq(ANY_ROOT), any())).thenReturn(anchorTx)
        whenever(anchorPort.getNetworkName()).thenReturn(ANY_NETWORK)
        whenever(auditVerificationUseCase.recordBlockchainAnchor(eq(1L), eq(ANY_TX_HASH), eq(ANY_TX_HASH), eq(ANY_BLOCK_NUMBER), eq(ANY_NETWORK), any()))
            .thenReturn(anchoredCheckpoint)

        val result = useCase.anchorCurrentRoot()

        assertSoftly {
            it.assertThat(result).isNotNull
            it.assertThat(result!!.rootHash).isEqualTo(ANY_ROOT)
            it.assertThat(result.transactionHash).isEqualTo(ANY_TX_HASH)
            it.assertThat(result.blockNumber).isEqualTo(ANY_BLOCK_NUMBER)
            it.assertThat(result.networkName).isEqualTo(ANY_NETWORK)
        }
    }

    @Test
    fun `GIVEN anchor port fails WHEN anchorCurrentRoot THEN returns null`() {
        val checkpoint = DomainMerkleRootCheckpoint(id = 1L, rootHash = ANY_ROOT, leafCount = 2L)
        whenever(auditVerificationUseCase.createRootCheckpoint()).thenReturn(checkpoint)
        whenever(anchorPort.anchorRoot(any(), any())).thenReturn(null)

        val result = useCase.anchorCurrentRoot()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN record anchor fails WHEN anchorCurrentRoot THEN returns null`() {
        val checkpoint = DomainMerkleRootCheckpoint(id = 1L, rootHash = ANY_ROOT, leafCount = 2L)
        val anchorTx = AnchorTransaction(transactionHash = ANY_TX_HASH, blockNumber = ANY_BLOCK_NUMBER)
        whenever(auditVerificationUseCase.createRootCheckpoint()).thenReturn(checkpoint)
        whenever(anchorPort.anchorRoot(any(), any())).thenReturn(anchorTx)
        whenever(anchorPort.getNetworkName()).thenReturn(ANY_NETWORK)
        whenever(auditVerificationUseCase.recordBlockchainAnchor(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(null)

        val result = useCase.anchorCurrentRoot()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN valid params WHEN verifyAnchor THEN delegates to port`() {
        whenever(anchorPort.verifyAnchor(ANY_ROOT, ANY_TX_HASH)).thenReturn(true)

        val result = useCase.verifyAnchor(ANY_ROOT, ANY_TX_HASH)

        assertThat(result).isTrue()
        verify(anchorPort).verifyAnchor(ANY_ROOT, ANY_TX_HASH)
    }

    @Test
    fun `GIVEN no anchored checkpoint WHEN getLastAnchoringResult THEN returns null`() {
        whenever(auditVerificationUseCase.getLatestAnchoredCheckpoint()).thenReturn(null)

        val result = useCase.getLastAnchoringResult()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN anchored checkpoint WHEN getLastAnchoringResult THEN returns result`() {
        val checkpoint = DomainMerkleRootCheckpoint(
            id = 1L, rootHash = ANY_ROOT, leafCount = 2L,
            transactionHash = ANY_TX_HASH, blockNumber = ANY_BLOCK_NUMBER,
            blockchainAnchorId = ANY_TX_HASH, networkName = ANY_NETWORK,
            anchoredAt = FIXED_TIMESTAMP
        )
        whenever(auditVerificationUseCase.getLatestAnchoredCheckpoint()).thenReturn(checkpoint)
        whenever(anchorPort.getNetworkName()).thenReturn(ANY_NETWORK)

        val result = useCase.getLastAnchoringResult()

        assertSoftly {
            it.assertThat(result).isNotNull
            it.assertThat(result!!.rootHash).isEqualTo(ANY_ROOT)
            it.assertThat(result.transactionHash).isEqualTo(ANY_TX_HASH)
        }
    }
}

private const val ANY_ROOT = "rootHash123"
private const val ANY_TX_HASH = "0xabc123"
private const val ANY_BLOCK_NUMBER = 12345L
private const val ANY_NETWORK = "sepolia"
private val FIXED_TIMESTAMP = Instant.now()

