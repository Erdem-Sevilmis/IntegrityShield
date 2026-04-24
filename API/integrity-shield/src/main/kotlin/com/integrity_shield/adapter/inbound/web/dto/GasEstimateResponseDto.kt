package com.integrity_shield.adapter.inbound.web.dto

import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

data class GasEstimateResponseDto(
    val testnet: TestnetEstimateDto,
    val mainnetEstimate: MainnetEstimateDto,
    val ethToUsdRate: BigDecimal?,
    val ethToEurRate: BigDecimal?,
    val estimatedAt: Instant
)

data class TestnetEstimateDto(
    val networkName: String,
    val gasPriceWei: BigInteger,
    val gasUsed: BigInteger,
    val totalCostWei: BigInteger,
    val totalCostEth: BigDecimal,
    val totalCostUsd: BigDecimal?,
    val totalCostEur: BigDecimal?
)

data class MainnetEstimateDto(
    val gasPriceWei: BigInteger,
    val gasUsed: BigInteger,
    val estimatedCostWei: BigInteger,
    val estimatedCostEth: BigDecimal,
    val estimatedCostUsd: BigDecimal?,
    val estimatedCostEur: BigDecimal?
)

