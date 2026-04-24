package com.integrity_shield.application

import com.integrity_shield.domain.model.AnchoringResult
import com.integrity_shield.domain.model.DomainMerkleRootCheckpoint
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import com.integrity_shield.domain.port.inbound.BlockchainAnchoringUseCase
import com.integrity_shield.domain.port.outbound.BlockchainAnchorPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BlockchainAnchoringUseCaseImpl(
    private val auditVerificationUseCase: AuditVerificationUseCase,
    private val anchorPort: BlockchainAnchorPort
) : BlockchainAnchoringUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun anchorCurrentRoot(): AnchoringResult? {
        val checkpoint = auditVerificationUseCase.createRootCheckpoint()
        log.info("Created checkpoint for root ${checkpoint.rootHash}")

        val anchorTx = anchorPort.anchorRoot(checkpoint.rootHash, checkpoint.checkpointTime)
        if (anchorTx == null) {
            log.error("Failed to anchor root ${checkpoint.rootHash}")
            return null
        }

        val anchored = auditVerificationUseCase.recordBlockchainAnchor(
            checkpointId = checkpoint.id,
            anchorId = anchorTx.anchorId,
            transactionHash = anchorTx.transactionHash,
            blockNumber = anchorTx.blockNumber,
            networkName = anchorPort.getNetworkName(),
            gasUsed = anchorTx.gasUsed
        )
        if (anchored == null) {
            log.error("Failed to record anchor for checkpoint ${checkpoint.id}")
            return null
        }

        val result = AnchoringResult(
            checkpointId = anchored.id,
            rootHash = anchored.rootHash,
            transactionHash = anchorTx.transactionHash,
            blockNumber = anchorTx.blockNumber,
            anchorId = anchorTx.anchorId,
            anchoredAt = anchored.anchoredAt ?: checkpoint.checkpointTime,
            networkName = anchorPort.getNetworkName()
        )

        log.info("Successfully anchored root ${result.rootHash} to ${result.networkName}: tx=${result.transactionHash}, block=${result.blockNumber}")
        return result
    }

    override fun getCurrentRootForAnchoring(): String {
        return auditVerificationUseCase.getCurrentRoot()
    }

    override fun getLatestAnchoredCheckpoint(): DomainMerkleRootCheckpoint? {
        return auditVerificationUseCase.getLatestAnchoredCheckpoint()
    }

    override fun verifyAnchor(rootHash: String, transactionHash: String): Boolean {
        return anchorPort.verifyAnchor(rootHash, transactionHash)
    }

    override fun getLastAnchoringResult(): AnchoringResult? {
        val checkpoint = auditVerificationUseCase.getLatestAnchoredCheckpoint() ?: return null
        val txHash = checkpoint.transactionHash ?: return null
        val blockNum = checkpoint.blockNumber ?: return null

        return AnchoringResult(
            checkpointId = checkpoint.id,
            rootHash = checkpoint.rootHash,
            transactionHash = txHash,
            blockNumber = blockNum,
            anchorId = checkpoint.blockchainAnchorId ?: txHash,
            anchoredAt = checkpoint.anchoredAt ?: checkpoint.checkpointTime,
            networkName = checkpoint.networkName ?: anchorPort.getNetworkName()
        )
    }
}

