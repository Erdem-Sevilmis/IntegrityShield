package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.domain.model.AnchoringResult
import com.integrity_shield.domain.port.inbound.BlockchainAnchoringUseCase
import com.integrity_shield.domain.port.inbound.GasEstimationUseCase
import com.integrity_shield.domain.model.GasEstimate
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@WebMvcTest(BlockchainAnchoringController::class)
class BlockchainAnchoringControllerIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var anchoringUseCase: BlockchainAnchoringUseCase

    @Autowired
    private lateinit var gasEstimationUseCase: GasEstimationUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        fun anchoringUseCase(): BlockchainAnchoringUseCase = mock()

        @Bean
        fun gasEstimationUseCase(): GasEstimationUseCase = mock()
    }

    @Test
    fun `GIVEN valid root WHEN POST anchor-root THEN returns anchoring result`() {
        val result = AnchoringResult(
            checkpointId = 1L, rootHash = ANY_ROOT, transactionHash = ANY_TX_HASH,
            blockNumber = ANY_BLOCK_NUMBER, anchorId = ANY_TX_HASH,
            anchoredAt = FIXED_TIMESTAMP, networkName = ANY_NETWORK
        )
        whenever(anchoringUseCase.anchorCurrentRoot()).thenReturn(result)

        mockMvc.perform(post("/api/v1/audit/anchor-root"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rootHash").value(ANY_ROOT))
            .andExpect(jsonPath("$.transactionHash").value(ANY_TX_HASH))
            .andExpect(jsonPath("$.networkName").value(ANY_NETWORK))
    }

    @Test
    fun `GIVEN anchoring fails WHEN POST anchor-root THEN returns 500`() {
        whenever(anchoringUseCase.anchorCurrentRoot()).thenReturn(null)

        mockMvc.perform(post("/api/v1/audit/anchor-root"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("anchoring_failed"))
    }

    @Test
    fun `GIVEN no latest anchor WHEN GET latest-anchor THEN returns 404`() {
        whenever(anchoringUseCase.getLastAnchoringResult()).thenReturn(null)

        mockMvc.perform(get("/api/v1/audit/latest-anchor"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GIVEN valid anchor WHEN GET anchors verify THEN returns verification`() {
        whenever(anchoringUseCase.verifyAnchor(ANY_ROOT, ANY_TX_HASH)).thenReturn(true)

        mockMvc.perform(
            get("/api/v1/audit/anchors/verify")
                .param("rootHash", ANY_ROOT)
                .param("transactionHash", ANY_TX_HASH)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isValid").value(true))
    }

    @Test
    fun `GIVEN gas data WHEN GET gas-estimate THEN returns estimate`() {
        val estimate = GasEstimate(
            testnetGasPriceWei = BigInteger.valueOf(1_000_000_000),
            testnetGasUsed = BigInteger.valueOf(48801),
            testnetTotalCostWei = BigInteger.valueOf(48801_000_000_000),
            testnetTotalCostEth = BigDecimal("0.000048801000000000"),
            testnetTotalCostUsd = BigDecimal("0.097602"),
            testnetTotalCostEur = BigDecimal("0.087842"),
            mainnetGasPriceWei = BigInteger.valueOf(30_000_000_000),
            mainnetEstimatedCostWei = BigInteger.valueOf(1464030_000_000_000),
            mainnetEstimatedCostEth = BigDecimal("0.001464030000000000"),
            mainnetEstimatedCostUsd = BigDecimal("2.9281"),
            mainnetEstimatedCostEur = BigDecimal("2.6353"),
            ethToUsdRate = BigDecimal("2000.00"),
            ethToEurRate = BigDecimal("1800.00"),
            networkName = ANY_NETWORK,
            estimatedAt = FIXED_TIMESTAMP
        )
        whenever(gasEstimationUseCase.estimateAnchoringCost()).thenReturn(estimate)

        mockMvc.perform(get("/api/v1/audit/gas-estimate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.testnet.networkName").value(ANY_NETWORK))
            .andExpect(jsonPath("$.ethToUsdRate").value(2000.00))
    }
}

private const val ANY_ROOT = "rootHash123"
private const val ANY_TX_HASH = "0xabc123"
private const val ANY_BLOCK_NUMBER = 12345L
private const val ANY_NETWORK = "sepolia"
private val FIXED_TIMESTAMP = Instant.now()

