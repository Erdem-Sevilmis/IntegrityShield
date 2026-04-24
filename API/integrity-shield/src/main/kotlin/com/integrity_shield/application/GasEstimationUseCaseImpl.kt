package com.integrity_shield.application

import com.integrity_shield.domain.model.GasEstimate
import com.integrity_shield.domain.port.inbound.GasEstimationUseCase
import com.integrity_shield.domain.port.outbound.BlockchainAnchorPort
import com.integrity_shield.domain.port.outbound.GasPricePort
import com.integrity_shield.domain.port.outbound.MerkleRootCheckpointRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Instant

@Service
class GasEstimationUseCaseImpl(
    private val gasPricePort: GasPricePort,
    private val blockchainAnchorPort: BlockchainAnchorPort,
    private val checkpointRepository: MerkleRootCheckpointRepositoryPort,

    @Value($$"${blockchain.estimation.default-gas-used:48801}")
    private val defaultGasUsed: Long
) : GasEstimationUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val ETH_DIVISOR = BigDecimal("1000000000000000000")
    }

    override fun estimateAnchoringCost(): GasEstimate {
        val testnetGasPrice = gasPricePort.getTestnetGasPrice()
        val mainnetGasPrice = gasPricePort.getMainnetGasPrice()
        val ethToUsdRate = gasPricePort.getEthToUsdRate()
        val ethToEurRate = gasPricePort.getEthToEurRate()

        val gasUsed = getLastTransactionGasUsed() ?: BigInteger.valueOf(defaultGasUsed)

        // --- Testnet costs ---
        val testnetTotalCostWei = gasUsed.multiply(testnetGasPrice)
        val testnetTotalCostEth = BigDecimal(testnetTotalCostWei)
            .divide(ETH_DIVISOR, 18, RoundingMode.HALF_UP)

        // Testnet ETH has no real value, but we show what the same gas would cost
        // at mainnet exchange rates for comparison purposes
        val testnetTotalCostUsd = if (ethToUsdRate != null) {
            testnetTotalCostEth.multiply(ethToUsdRate).setScale(6, RoundingMode.HALF_UP)
        } else null

        val testnetTotalCostEur = if (ethToEurRate != null) {
            testnetTotalCostEth.multiply(ethToEurRate).setScale(6, RoundingMode.HALF_UP)
        } else null

        // --- Mainnet costs ---
        val mainnetEstimatedCostWei = gasUsed.multiply(mainnetGasPrice)
        val mainnetEstimatedCostEth = BigDecimal(mainnetEstimatedCostWei)
            .divide(ETH_DIVISOR, 18, RoundingMode.HALF_UP)

        val mainnetEstimatedCostUsd = if (ethToUsdRate != null) {
            mainnetEstimatedCostEth.multiply(ethToUsdRate).setScale(4, RoundingMode.HALF_UP)
        } else null

        val mainnetEstimatedCostEur = if (ethToEurRate != null) {
            mainnetEstimatedCostEth.multiply(ethToEurRate).setScale(4, RoundingMode.HALF_UP)
        } else null

        val networkName = blockchainAnchorPort.getNetworkName()

        log.info(
            "Gas estimate: testnetPrice={}wei, mainnetPrice={}wei, gasUsed={}, " +
            "mainnetCost={}ETH, usd={}, eur={}, network={}",
            testnetGasPrice, mainnetGasPrice, gasUsed,
            mainnetEstimatedCostEth, mainnetEstimatedCostUsd, mainnetEstimatedCostEur, networkName
        )

        return GasEstimate(
            testnetGasPriceWei = testnetGasPrice,
            testnetGasUsed = gasUsed,
            testnetTotalCostWei = testnetTotalCostWei,
            testnetTotalCostEth = testnetTotalCostEth,
            testnetTotalCostUsd = testnetTotalCostUsd,
            testnetTotalCostEur = testnetTotalCostEur,
            mainnetGasPriceWei = mainnetGasPrice,
            mainnetEstimatedCostWei = mainnetEstimatedCostWei,
            mainnetEstimatedCostEth = mainnetEstimatedCostEth,
            mainnetEstimatedCostUsd = mainnetEstimatedCostUsd,
            mainnetEstimatedCostEur = mainnetEstimatedCostEur,
            ethToUsdRate = ethToUsdRate,
            ethToEurRate = ethToEurRate,
            networkName = networkName,
            estimatedAt = Instant.now()
        )
    }

    override fun getLastTransactionGasUsed(): BigInteger? {
        val anchored = checkpointRepository.findByBlockchainAnchorIdNotNullOrderByAnchoredAtDesc()
        val latest = anchored.firstOrNull() ?: return null
        // If gasUsed is stored in the checkpoint, use it; otherwise return null to fall back
        return latest.gasUsed
    }
}

