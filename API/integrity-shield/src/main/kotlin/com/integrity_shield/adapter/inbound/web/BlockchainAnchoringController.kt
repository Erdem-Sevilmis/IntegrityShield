package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.adapter.inbound.web.dto.*
import com.integrity_shield.domain.exception.RootAlreadyAnchoredException
import com.integrity_shield.domain.port.inbound.BlockchainAnchoringUseCase
import com.integrity_shield.domain.port.inbound.GasEstimationUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/audit")
class BlockchainAnchoringController(
    private val anchoringUseCase: BlockchainAnchoringUseCase,
    private val gasEstimationUseCase: GasEstimationUseCase
) {

    @PostMapping("/anchor-root")
    fun anchorCurrentRoot(): ResponseEntity<Any> {
        val result = try {
            anchoringUseCase.anchorCurrentRoot()
        } catch (_: RootAlreadyAnchoredException) {
            return ResponseEntity.status(409).body(ErrorResponseDto(
                error = "root_already_anchored",
                message = "This Merkle root has already been anchored on-chain and cannot be anchored again"
            ))
        } ?: return ResponseEntity.status(500).body(ErrorResponseDto(
            error = "anchoring_failed",
            message = "Failed to anchor root on blockchain"
        ))

        return ResponseEntity.ok(AnchoringResultDto(
            checkpointId = result.checkpointId,
            rootHash = result.rootHash,
            transactionHash = result.transactionHash,
            blockNumber = result.blockNumber,
            anchoredAt = result.anchoredAt,
            networkName = result.networkName
        ))
    }

    @GetMapping("/latest-anchor")
    fun getLatestAnchor(): ResponseEntity<AnchoringResultDto> {
        val result = anchoringUseCase.getLastAnchoringResult()
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(AnchoringResultDto(
            checkpointId = result.checkpointId,
            rootHash = result.rootHash,
            transactionHash = result.transactionHash,
            blockNumber = result.blockNumber,
            anchoredAt = result.anchoredAt,
            networkName = result.networkName
        ))
    }

    @GetMapping("/anchors/verify")
    fun verifyAnchor(
        @RequestParam rootHash: String,
        @RequestParam transactionHash: String
    ): ResponseEntity<AnchorVerificationResponseDto> {
        val isValid = anchoringUseCase.verifyAnchor(rootHash, transactionHash)
        return ResponseEntity.ok(AnchorVerificationResponseDto(
            rootHash = rootHash,
            transactionHash = transactionHash,
            isValid = isValid,
            message = if (isValid) "Anchor verified successfully on-chain" else "Anchor verification failed"
        ))
    }

    @GetMapping("/gas-estimate")
    fun getGasEstimate(): ResponseEntity<GasEstimateResponseDto> {
        val estimate = gasEstimationUseCase.estimateAnchoringCost()
        return ResponseEntity.ok(GasEstimateResponseDto(
            testnet = TestnetEstimateDto(
                networkName = estimate.networkName,
                gasPriceWei = estimate.testnetGasPriceWei,
                gasUsed = estimate.testnetGasUsed,
                totalCostWei = estimate.testnetTotalCostWei,
                totalCostEth = estimate.testnetTotalCostEth,
                totalCostUsd = estimate.testnetTotalCostUsd,
                totalCostEur = estimate.testnetTotalCostEur
            ),
            mainnetEstimate = MainnetEstimateDto(
                gasPriceWei = estimate.mainnetGasPriceWei,
                gasUsed = estimate.testnetGasUsed,
                estimatedCostWei = estimate.mainnetEstimatedCostWei,
                estimatedCostEth = estimate.mainnetEstimatedCostEth,
                estimatedCostUsd = estimate.mainnetEstimatedCostUsd,
                estimatedCostEur = estimate.mainnetEstimatedCostEur
            ),
            ethToUsdRate = estimate.ethToUsdRate,
            ethToEurRate = estimate.ethToEurRate,
            estimatedAt = estimate.estimatedAt
        ))
    }
}

