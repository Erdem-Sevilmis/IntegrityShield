package com.integrity_shield.domain.port.inbound

import com.integrity_shield.domain.model.GasEstimate
import java.math.BigInteger

interface GasEstimationUseCase {
    fun estimateAnchoringCost(): GasEstimate
    fun getLastTransactionGasUsed(): BigInteger?
}

