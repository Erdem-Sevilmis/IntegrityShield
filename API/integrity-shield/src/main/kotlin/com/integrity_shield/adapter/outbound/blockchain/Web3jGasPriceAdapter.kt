package com.integrity_shield.adapter.outbound.blockchain

import com.integrity_shield.config.BlockchainProperties
import com.integrity_shield.domain.port.outbound.GasPricePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * CoinGecko API response model.
 * Expected format: {"ethereum":{"usd":1234.56,"eur":1100.23}}
 */
private data class CoinGeckoResponse(
    val ethereum: CurrencyRates = CurrencyRates()
)

private data class CurrencyRates(
    val usd: BigDecimal? = null,
    val eur: BigDecimal? = null
)

@Service
class Web3jGasPriceAdapter(
    props: BlockchainProperties
) : GasPricePort {

    private val testnetRpcUrl = props.network.rpcUrl
    private val mainnetRpcUrl = props.mainnet.rpcUrl.ifBlank { "https://mainnet.infura.io/v3/placeholder" }
    private val apiKeySecret = props.api.keySecret

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    private val testnetWeb3j: Web3j = Web3j.build(createHttpService(testnetRpcUrl))
    private val mainnetWeb3j: Web3j by lazy { Web3j.build(createHttpService(mainnetRpcUrl)) }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    // Simple cache: rates + timestamp
    @Volatile private var cachedUsdRate: BigDecimal? = null
    @Volatile private var cachedEurRate: BigDecimal? = null
    @Volatile private var cacheTimestamp: Long = 0L
    private val cacheTtlMs = 60_000L // 60 seconds

    override fun getTestnetGasPrice(): BigInteger {
        return try {
            val gasPrice = testnetWeb3j.ethGasPrice().send().gasPrice
            log.debug("Testnet gas price: {} wei", gasPrice)
            gasPrice
        } catch (e: Exception) {
            log.warn("Failed to fetch testnet gas price: {}, returning default", e.message)
            BigInteger.valueOf(20_000_000_000L) // 20 gwei default
        }
    }

    override fun getMainnetGasPrice(): BigInteger {
        return try {
            val gasPrice = mainnetWeb3j.ethGasPrice().send().gasPrice
            log.debug("Mainnet gas price: {} wei", gasPrice)
            gasPrice
        } catch (e: Exception) {
            log.warn("Failed to fetch mainnet gas price: {}, returning default", e.message)
            BigInteger.valueOf(30_000_000_000L) // 30 gwei default
        }
    }

    override fun getEthToUsdRate(): BigDecimal? {
        refreshExchangeRatesIfNeeded()
        return cachedUsdRate
    }

    override fun getEthToEurRate(): BigDecimal? {
        refreshExchangeRatesIfNeeded()
        return cachedEurRate
    }

    private fun refreshExchangeRatesIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - cacheTimestamp < cacheTtlMs && cachedUsdRate != null) {
            return // cache still valid
        }

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd,eur"))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val coinGeckoResponse: CoinGeckoResponse = objectMapper.readValue(response.body())
                val rates = coinGeckoResponse.ethereum

                rates.usd?.let {
                    cachedUsdRate = it
                    log.info("ETH/USD rate from CoinGecko: {}", it)
                }
                rates.eur?.let {
                    cachedEurRate = it
                    log.info("ETH/EUR rate from CoinGecko: {}", it)
                }
                cacheTimestamp = now
            } else {
                log.warn("CoinGecko API returned status {}: {}", response.statusCode(), response.body())
            }
        } catch (e: Exception) {
            log.warn("Failed to fetch exchange rates from CoinGecko: {}", e.message)
        }
    }


    private fun createHttpService(rpcUrl: String): HttpService {
        return if (apiKeySecret.isNotBlank()) {
            val httpService = HttpService(rpcUrl)
            val authHeader = "Basic " + Base64.getEncoder().encodeToString(apiKeySecret.toByteArray(Charsets.UTF_8))
            httpService.addHeader("Authorization", authHeader)
            httpService
        } else {
            HttpService(rpcUrl)
        }
    }
}

