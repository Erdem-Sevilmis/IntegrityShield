package com.integrity_shield.adapter.scheduler

import com.integrity_shield.domain.exception.AnchoringExecutionException
import com.integrity_shield.domain.port.inbound.BlockchainAnchoringUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["blockchain.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class BlockchainAnchoringScheduler(
    private val anchoringUseCase: BlockchainAnchoringUseCase
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = $$"#{${blockchain.scheduler.initial-delay-minutes:1} * 60000}",
        fixedRateString = $$"#{${blockchain.scheduler.interval-minutes:5} * 60000}"
    )
    fun scheduleAnchor() {
        try {
            log.info("Starting scheduled blockchain anchoring process...")

            val currentRoot = anchoringUseCase.getCurrentRootForAnchoring()
            if (currentRoot.isBlank()) {
                log.info("No Merkle Root available for anchoring yet")
                return
            }

            val result = anchoringUseCase.anchorCurrentRoot()
            if (result != null) {
                log.info(
                    "Scheduled anchoring successful: root={}, tx={}, block={}, network={}",
                    result.rootHash, result.transactionHash, result.blockNumber, result.networkName
                )
            } else {
                log.warn("Scheduled anchoring returned no result for current root")
            }
        } catch (e: AnchoringExecutionException) {
            log.error("Anchoring execution error in scheduled run: {}", e.message, e)
        } catch (e: IllegalStateException) {
            log.error("Illegal state during scheduled blockchain anchoring: {}", e.message, e)
        } catch (e: RuntimeException) {
            log.error("Unexpected error in scheduled blockchain anchoring: {}", e.message, e)
        }
    }

    @Scheduled(fixedDelayString = $$"#{${blockchain.scheduler.verify-delay-minutes:30} * 60000}")
    fun verifyLatestAnchor() {
        try {
            log.debug("Verifying latest anchor...")

            val checkpoint = anchoringUseCase.getLatestAnchoredCheckpoint()
            if (checkpoint != null) {
                log.info("Latest anchored checkpoint: root={}, id={}", checkpoint.rootHash, checkpoint.id)
            } else {
                log.info("No anchored checkpoint found yet")
            }
        } catch (e: IllegalStateException) {
            log.error("Illegal state during anchor verification: {}", e.message, e)
        } catch (e: RuntimeException) {
            log.error("Unexpected error verifying latest anchor: {}", e.message, e)
        }
    }
}

