package com.integrity_shield.adapter.outbound.blockchain

import com.integrity_shield.config.BlockchainProperties
import com.integrity_shield.domain.exception.RootAlreadyAnchoredException
import com.integrity_shield.domain.model.AnchorTransaction
import com.integrity_shield.domain.port.outbound.BlockchainAnchorPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.crypto.Credentials
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant
import java.util.Base64

@Service
class Web3jEthereumAnchorAdapter(
    props: BlockchainProperties
) : BlockchainAnchorPort {

    private val rpcUrl = props.network.rpcUrl
    private val networkName = props.network.name
    private val privateKey = props.wallet.privateKey
    private val contractAddress = props.contract.address
    private val gasLimit = props.transaction.gasLimit
    private val gasPriceConfig = props.transaction.gasPrice
    private val apiKeySecret = props.api.keySecret

    private val log = LoggerFactory.getLogger(javaClass)
    private val web3j: Web3j = Web3j.build(createHttpService())

    private var credentials: Credentials? = null
    private var walletAddress: String = ""

    init {
        try {
            if (privateKey.isNotBlank() && contractAddress.isNotBlank()) {
                credentials = Credentials.create(privateKey)
                walletAddress = credentials!!.address
                log.info("Ethereum Anchor Adapter initialized for network: $networkName")
                log.info("Wallet address: $walletAddress")
                log.info("Contract address: $contractAddress")
                log.info("RPC URL: $rpcUrl")
                val chainId = web3j.ethChainId().send().chainId
                log.info("Connected to chain ID: $chainId")
            } else {
                log.warn("Private key or contract address not configured. Adapter will not initialize.")
            }
        } catch (e: Exception) {
            log.error("Error initializing Ethereum Anchor Adapter: ${e.message}", e)
            throw IllegalStateException("Failed to initialize blockchain anchor adapter", e)
        }
    }

    override fun anchorRoot(rootHash: String, timestamp: Instant): AnchorTransaction? {
        return try {
            log.info("Anchoring root hash $rootHash to network $networkName")

            if (credentials == null || walletAddress.isBlank()) {
                log.warn("Credentials not properly initialized. Skipping anchoring.")
                return null
            }
            if (contractAddress.isBlank()) {
                log.warn("Contract address not configured. Skipping anchoring.")
                return null
            }

            sendAnchorTransaction(rootHash)
        } catch (e: RootAlreadyAnchoredException) {
            log.error("Failed to anchor $rootHash: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            log.error("Failed to anchor root $rootHash: ${e.message}", e)
            // Some nodes do not return revert reasons for owner-only duplicate checks.
            if (isRootAnchored(rootHash)) {
                throw RootAlreadyAnchoredException(rootHash)
            }
            null
        }
    }

    override fun verifyAnchor(rootHash: String, transactionHash: String): Boolean {
        return try {
            val receiptValid = verifyTransactionReceipt(transactionHash)
            if (!receiptValid) return false

            val anchored = isRootAnchored(rootHash)
            if (!anchored) {
                log.warn("Root $rootHash not found in contract despite valid transaction $transactionHash")
            }
            anchored
        } catch (e: Exception) {
            log.error("Error verifying transaction $transactionHash: ${e.message}")
            false
        }
    }

    override fun getNetworkName(): String = networkName

    private fun sendAnchorTransaction(rootHash: String): AnchorTransaction? {
        val nonce = getNonce() ?: return null
        val gasPrice = resolveGasPrice()
        val functionData = encodeAnchorFunction(rootHash)

        if (functionData == "0x") {
            log.error("Failed to encode anchor function for root $rootHash")
            return null
        }

        log.info("Transaction details - Nonce: $nonce, Gas Price: $gasPrice, Gas Limit: $gasLimit")

        val rawTransaction = RawTransaction.createTransaction(
            nonce, gasPrice, BigInteger.valueOf(gasLimit),
            contractAddress, BigInteger.ZERO, functionData
        )

        val txHash = sendSignedTransaction(rawTransaction, rootHash) ?: return null
        return awaitTransactionResult(txHash, rootHash)
    }

    private fun getNonce(): BigInteger? {
        return try {
            web3j.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                .send().transactionCount
        } catch (e: Exception) {
            log.error("Error fetching nonce for address $walletAddress: ${e.message}")
            null
        }
    }

    private fun resolveGasPrice(): BigInteger {
        return if (gasPriceConfig.lowercase() == "auto") {
            web3j.ethGasPrice().send().gasPrice
        } else {
            BigInteger(gasPriceConfig)
        }
    }

    private fun sendSignedTransaction(transaction: RawTransaction, rootHash: String): String? {
        return try {
            val signedMessage = TransactionEncoder.signMessage(transaction, credentials!!)
            val hexValue = Numeric.toHexString(signedMessage)
            val result = web3j.ethSendRawTransaction(hexValue).send()

            if (result.transactionHash != null) {
                log.info("Transaction sent: ${result.transactionHash}")
                result.transactionHash
            } else if (result.error != null) {
                val errorMessage = result.error.message ?: ""
                log.error("Transaction error: $errorMessage")
                if (errorMessage.contains("already anchored", ignoreCase = true)) {
                    throw RootAlreadyAnchoredException(rootHash)
                }
                null
            } else {
                null
            }
        } catch (e: RootAlreadyAnchoredException) {
            throw e
        } catch (e: Exception) {
            log.error("Error sending transaction: ${e.message}", e)
            null
        }
    }

    private fun awaitTransactionResult(txHash: String, rootHash: String): AnchorTransaction? {
        val receipt = waitForTransactionReceipt(txHash)
        if (receipt == null) {
            log.warn("Transaction receipt not found for: $txHash")
            return null
        }
        if (!receipt.isStatusOK) {
            val revertReason = receipt.revertReason ?: "unknown"
            val rootAlreadyAnchored = revertReason.contains("already anchored", ignoreCase = true) || isRootAnchored(rootHash)
            if (rootAlreadyAnchored) {
                log.warn("Root already anchored on-chain: $rootHash")
                throw RootAlreadyAnchoredException(rootHash)
            } else {
                log.error("Transaction failed on blockchain: $txHash (reason: $revertReason)")
            }
            return null
        }

        log.info("Successfully anchored root $rootHash: tx=$txHash, block=${receipt.blockNumber}")
        return AnchorTransaction(
            transactionHash = txHash,
            blockNumber = receipt.blockNumber.toLong(),
            anchorId = txHash,
            timestamp = Instant.now(),
            gasUsed = receipt.gasUsed
        )
    }

    private fun waitForTransactionReceipt(
        txHash: String,
        maxAttempts: Int = 60,
        delayMs: Long = 2000
    ): TransactionReceipt? {
        return try {
            repeat(maxAttempts) { attempt ->
                val receipt = web3j.ethGetTransactionReceipt(txHash).send()
                if (receipt.transactionReceipt.isPresent) {
                    val tx = receipt.transactionReceipt.get()
                    log.info("Transaction receipt received: block=${tx.blockNumber}, gasUsed=${tx.gasUsed}")
                    return tx
                }
                Thread.sleep(delayMs)
                if ((attempt + 1) % 10 == 0) {
                    log.debug("Still waiting for transaction receipt... (attempt ${attempt + 1}/$maxAttempts)")
                }
            }
            log.warn("Transaction receipt not found after $maxAttempts attempts (${maxAttempts * delayMs / 1000}s)")
            null
        } catch (e: Exception) {
            log.error("Error waiting for transaction receipt: ${e.message}")
            null
        }
    }

    private fun verifyTransactionReceipt(transactionHash: String): Boolean {
        val receipt = web3j.ethGetTransactionReceipt(transactionHash).send()
        if (!receipt.transactionReceipt.isPresent) {
            log.warn("Transaction receipt not found for $transactionHash")
            return false
        }

        val txReceipt = receipt.transactionReceipt.get()
        if (!txReceipt.isStatusOK) {
            log.warn("Transaction $transactionHash failed on-chain (status not OK)")
            return false
        }

        log.info("Transaction $transactionHash verified on block ${txReceipt.blockNumber}")
        return true
    }

    private fun isRootAnchored(rootHash: String): Boolean {
        return try {
            val function = Function(
                "isRootAnchored",
                listOf(toBytes32(rootHash)),
                listOf(object : TypeReference<Bool>() {})
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(walletAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            val result = FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
            val anchored = result.firstOrNull()?.value as? Boolean ?: false
            log.debug("isRootAnchored($rootHash) = $anchored")
            anchored
        } catch (e: Exception) {
            log.error("Error checking if root is anchored: ${e.message}", e)
            false
        }
    }

    private fun encodeAnchorFunction(rootHash: String): String {
        return try {
            val function = Function(
                "anchorMerkleRoot",
                listOf(toBytes32(rootHash)),
                emptyList()
            )
            FunctionEncoder.encode(function)
        } catch (e: Exception) {
            log.error("Error encoding anchor function: ${e.message}", e)
            "0x"
        }
    }

    private fun toBytes32(hexHash: String): Bytes32 =
        Bytes32(Numeric.hexStringToByteArray(hexHash))

    private fun createHttpService(): HttpService {
        return if (apiKeySecret.isNotBlank()) {
            val httpService = HttpService(rpcUrl)
            val authHeader = "Basic " + Base64.getEncoder().encodeToString(apiKeySecret.toByteArray(Charsets.UTF_8))
            httpService.addHeader("Authorization", authHeader)
            log.info("HttpService configured with API Key Secret authentication")
            httpService
        } else {
            log.info("HttpService configured without authentication")
            HttpService(rpcUrl)
        }
    }
}
