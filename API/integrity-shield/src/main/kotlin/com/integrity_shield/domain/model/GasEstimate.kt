package com.integrity_shield.domain.model

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

data class GasEstimate(
    val testnetGasPriceWei: BigInteger,
    val testnetGasUsed: BigInteger,
    val testnetTotalCostWei: BigInteger,
    val testnetTotalCostEth: BigDecimal,
    val testnetTotalCostUsd: BigDecimal?,
    val testnetTotalCostEur: BigDecimal?,
    val mainnetGasPriceWei: BigInteger,
    val mainnetEstimatedCostWei: BigInteger,
    val mainnetEstimatedCostEth: BigDecimal,
    val mainnetEstimatedCostUsd: BigDecimal?,
    val mainnetEstimatedCostEur: BigDecimal?,
    val ethToUsdRate: BigDecimal?,
    val ethToEurRate: BigDecimal?,
    val networkName: String,
    val estimatedAt: Instant
)

