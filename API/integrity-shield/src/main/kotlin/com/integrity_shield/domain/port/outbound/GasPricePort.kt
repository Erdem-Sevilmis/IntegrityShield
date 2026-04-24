package com.integrity_shield.domain.port.outbound

import java.math.BigDecimal
import java.math.BigInteger

interface GasPricePort {
    fun getTestnetGasPrice(): BigInteger
    fun getMainnetGasPrice(): BigInteger
    fun getEthToUsdRate(): BigDecimal?
    fun getEthToEurRate(): BigDecimal?
}

