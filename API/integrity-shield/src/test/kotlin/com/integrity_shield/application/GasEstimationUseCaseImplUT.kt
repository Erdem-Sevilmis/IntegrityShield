package com.integrity_shield.application

import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.port.outbound.BlockchainAnchorPort
import com.integrity_shield.domain.port.outbound.GasPricePort
import com.integrity_shield.domain.port.outbound.MerkleRootCheckpointRepositoryPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

class GasEstimationUseCaseImplUT {

    private val gasPricePort: GasPricePort = mock()
    private val blockchainAnchorPort: BlockchainAnchorPort = mock()
    private val checkpointRepository: MerkleRootCheckpointRepositoryPort = mock()

    private val useCase = GasEstimationUseCaseImpl(
        gasPricePort,
        blockchainAnchorPort,
        checkpointRepository,
        DEFAULT_GAS_USED
    )

    @BeforeEach
    fun init() {
        reset(gasPricePort, blockchainAnchorPort, checkpointRepository)
    }

    @Test
    fun `GIVEN gas prices and exchange rates WHEN estimateAnchoringCost THEN returns complete estimate`() {
        whenever(gasPricePort.getTestnetGasPrice()).thenReturn(TESTNET_GAS_PRICE)
        whenever(gasPricePort.getMainnetGasPrice()).thenReturn(MAINNET_GAS_PRICE)
        whenever(gasPricePort.getEthToUsdRate()).thenReturn(ETH_USD_RATE)
        whenever(gasPricePort.getEthToEurRate()).thenReturn(ETH_EUR_RATE)
        whenever(blockchainAnchorPort.getNetworkName()).thenReturn(ANY_NETWORK)
        whenever(checkpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc()).thenReturn(emptyList())

        val result = useCase.estimateAnchoringCost()

        assertSoftly {
            it.assertThat(result.testnetGasPriceWei).isEqualTo(TESTNET_GAS_PRICE)
            it.assertThat(result.mainnetGasPriceWei).isEqualTo(MAINNET_GAS_PRICE)
            it.assertThat(result.testnetGasUsed).isEqualTo(BigInteger.valueOf(DEFAULT_GAS_USED))
            it.assertThat(result.networkName).isEqualTo(ANY_NETWORK)
            it.assertThat(result.ethToUsdRate).isEqualTo(ETH_USD_RATE)
            it.assertThat(result.ethToEurRate).isEqualTo(ETH_EUR_RATE)
            it.assertThat(result.testnetTotalCostWei).isEqualTo(TESTNET_GAS_PRICE.multiply(BigInteger.valueOf(DEFAULT_GAS_USED)))
            it.assertThat(result.mainnetEstimatedCostWei).isEqualTo(MAINNET_GAS_PRICE.multiply(BigInteger.valueOf(DEFAULT_GAS_USED)))
            it.assertThat(result.testnetTotalCostUsd).isNotNull
            it.assertThat(result.mainnetEstimatedCostUsd).isNotNull
        }
    }

    @Test
    fun `GIVEN null exchange rates WHEN estimateAnchoringCost THEN USD and EUR costs are null`() {
        whenever(gasPricePort.getTestnetGasPrice()).thenReturn(TESTNET_GAS_PRICE)
        whenever(gasPricePort.getMainnetGasPrice()).thenReturn(MAINNET_GAS_PRICE)
        whenever(gasPricePort.getEthToUsdRate()).thenReturn(null)
        whenever(gasPricePort.getEthToEurRate()).thenReturn(null)
        whenever(blockchainAnchorPort.getNetworkName()).thenReturn(ANY_NETWORK)
        whenever(checkpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc()).thenReturn(emptyList())

        val result = useCase.estimateAnchoringCost()

        assertSoftly {
            it.assertThat(result.testnetTotalCostUsd).isNull()
            it.assertThat(result.testnetTotalCostEur).isNull()
            it.assertThat(result.mainnetEstimatedCostUsd).isNull()
            it.assertThat(result.mainnetEstimatedCostEur).isNull()
        }
    }

    @Test
    fun `GIVEN previous anchored checkpoint with gasUsed WHEN estimateAnchoringCost THEN uses stored gasUsed`() {
        val checkpoint = DomainMerkleRootCheckpoint(
            id = 1L, rootHash = "root", leafCount = 1L,
            blockchainAnchorId = "anchor", anchoredAt = Instant.now(),
            gasUsed = STORED_GAS_USED
        )
        whenever(checkpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc())
            .thenReturn(listOf(checkpoint))
        whenever(gasPricePort.getTestnetGasPrice()).thenReturn(TESTNET_GAS_PRICE)
        whenever(gasPricePort.getMainnetGasPrice()).thenReturn(MAINNET_GAS_PRICE)
        whenever(gasPricePort.getEthToUsdRate()).thenReturn(ETH_USD_RATE)
        whenever(gasPricePort.getEthToEurRate()).thenReturn(ETH_EUR_RATE)
        whenever(blockchainAnchorPort.getNetworkName()).thenReturn(ANY_NETWORK)

        val result = useCase.estimateAnchoringCost()

        assertThat(result.testnetGasUsed).isEqualTo(STORED_GAS_USED)
    }

    @Test
    fun `GIVEN no anchored checkpoints WHEN getLastTransactionGasUsed THEN returns null`() {
        whenever(checkpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc()).thenReturn(emptyList())

        val result = useCase.getLastTransactionGasUsed()

        assertThat(result).isNull()
    }
}

private const val DEFAULT_GAS_USED = 48801L
private const val ANY_NETWORK = "sepolia"
private val TESTNET_GAS_PRICE = BigInteger.valueOf(1_000_000_000L)
private val MAINNET_GAS_PRICE = BigInteger.valueOf(30_000_000_000L)
private val ETH_USD_RATE = BigDecimal("2000.00")
private val ETH_EUR_RATE = BigDecimal("1800.00")
private val STORED_GAS_USED = BigInteger.valueOf(55000L)

