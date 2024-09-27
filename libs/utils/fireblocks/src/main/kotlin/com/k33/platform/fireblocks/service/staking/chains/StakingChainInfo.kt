package com.k33.platform.fireblocks.service.staking.chains

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/chains/$chainDescriptor/chainInfo
//

data class StakingChainSummary(
    val chainDescriptor: String,
    val currentEpoch: Long,
    val epochElapsed: Long,
    val epochDuration: Long,
    val lastUpdated: Long,
    val additionalInfo: AdditionalInfo,
)

data class AdditionalInfo(
    val estimatedAnnualReward: Long,
    val lockupPeriod: Long,
    val activationPeriod: Long,
)

/**
 * Get chain-specific staking summary.
 * [staking/chains/{chainDescriptor}/chainInfo](https://developers.fireblocks.com/reference/getchaininfo)
 */
suspend fun FireblocksStakingService.getStackingChainSummary(
    chainDescriptor: String,
): StakingChainSummary? = FireblocksClient.get(
    path = "staking/chains/$chainDescriptor/chainInfo"
)