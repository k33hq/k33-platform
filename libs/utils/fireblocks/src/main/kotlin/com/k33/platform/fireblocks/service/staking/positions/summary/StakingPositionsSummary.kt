package com.k33.platform.fireblocks.service.staking.positions.summary

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/positions/summary
// staking/positions/summary/vaults
//

data class StakingSummary(
    val active: List<StakingSummaryPosition>,
    val inactive: List<StakingSummaryPosition>,
    val rewardsAmount: List<StakingSummaryPosition>,
    val totalStaked: List<StakingSummaryPosition>,
)

data class StakingSummaryPosition(
    val chainDescriptor: String,
    val amount: String,
)

/**
 * Get staking summary details.
 * [staking/positions/summary](https://developers.fireblocks.com/reference/getsummary)
 */
suspend fun FireblocksStakingService.getStakingSummary(): StakingSummary? = FireblocksClient.get(
    path = "staking/positions/summary"
)

/**
 * Get staking summary details by vault.
 * [staking/positions/summary/vaults](https://developers.fireblocks.com/reference/getsummarybyvault)
 */
suspend fun FireblocksStakingService.getStakingSummaryByVault(): Map<String, StakingSummary>? = FireblocksClient.get(
    path = "staking/positions/summary/vaults"
)