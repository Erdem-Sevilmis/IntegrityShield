package com.integrity_shield.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for blockchain anchoring
 */
@Configuration
@EnableConfigurationProperties(BlockchainProperties::class)
class BlockchainConfiguration

@ConfigurationProperties(prefix = "blockchain")
data class BlockchainProperties(
    var api: ApiConfig = ApiConfig(),
    var network: NetworkConfig = NetworkConfig(),
    var contract: ContractConfig = ContractConfig(),
    var transaction: TransactionConfig = TransactionConfig(),
    var wallet: WalletConfig = WalletConfig(),
    var scheduler: SchedulerConfig = SchedulerConfig(),
    var mainnet: MainnetConfig = MainnetConfig()
)

data class ApiConfig(
    var keySecret: String = ""
)

data class NetworkConfig(
    var name: String = "ethereum",
    var rpcUrl: String = ""
)

data class ContractConfig(
    var address: String = "",
    var abiPath: String = "classpath:abi/AnchorContract.json"
)

data class TransactionConfig(
    var gasLimit: Long = 100000,
    var gasPrice: String = "auto"
)

data class WalletConfig(
    var privateKey: String = "",
    var keystorePath: String = "",
    var keystorePassword: String = ""
)

data class SchedulerConfig(
    var enabled: Boolean = false,
    var intervalMinutes: Long = 5,
    var initialDelayMinutes: Long = 1,
    var verifyDelayMinutes: Long = 30
)

data class MainnetConfig(
    var rpcUrl: String = ""
)

