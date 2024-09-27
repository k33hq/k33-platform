package com.k33.platform.fireblocks.service.staking.positions

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/positions
// staking/positions/$id
//

data class StakingPosition(
    val id: String,
    val vaultAccountId: String,
    val validatorName: String,
    val providerName: String,
    val chainDescriptor: String,
    val amount: String,
    val rewardsAmount: String,
    val dateCreated: String,
    val status: String,
    val relatedTransactions: List<RelatedTransaction>,
    val validatorAddress: String,
    val providerId: String,
    val availableActions: List<String>,
    val inProgress: Boolean,
    val inProgressTxId: String? = null,
    val blockchainPositionInfo: BlockchainPositionInfo,
)

data class RelatedTransaction(
    val txId: String,
    val completed: Boolean,
)

data class BlockchainPositionInfo(
    val stakeAccountAddress: String,
)

/**
 * List staking positions details.
 * [staking/positions](https://developers.fireblocks.com/reference/getalldelegations)
 */
suspend fun FireblocksStakingService.getStakingPositions(): List<StakingPosition> = FireblocksClient.get(
    "staking/positions"
) ?: emptyList()

/**
 * Get staking position details.
 * [staking/positions/{id}](https://developers.fireblocks.com/reference/getdelegationbyid)
 */
suspend fun FireblocksStakingService.getStakingPosition(id: String): StakingPosition? = FireblocksClient.get(
    "staking/positions/$id"
)