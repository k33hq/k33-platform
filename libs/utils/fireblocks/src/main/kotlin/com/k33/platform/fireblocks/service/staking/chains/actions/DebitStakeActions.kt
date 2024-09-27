package com.k33.platform.fireblocks.service.staking.chains.actions

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/chains/$chainDescriptor/unstake
// staking/chains/$chainDescriptor/withdraw
// staking/chains/$chainDescriptor/claimRewards
//

private data class DebitStakeRequest(
    val id: String,
    val fee: String? = null,
    val feeLevel: String? = null,
    val txNote: String? = null,
)

suspend fun debitStakeAction(
    action: String,
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String? = null,
): String? = FireblocksClient.post(
    path = "staking/chains/$chainDescriptor/$action",
    "Idempotency-Key" to requestId,
) {
    val (fee, feeLevel) = stakingFee
    DebitStakeRequest(
        id = stakingPositionId,
        fee = fee?.toString(),
        feeLevel = feeLevel,
        txNote = txNote,
    )
}

/**
 * Execute a staking action - `unstake`
 * [staking/chains/{chainDescriptor}/unstake](https://developers.fireblocks.com/reference/executeaction)
 */
suspend fun FireblocksStakingService.unstake(
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String? = null,
): String? = debitStakeAction(
    action = "unstake",
    requestId = requestId,
    chainDescriptor = chainDescriptor,
    stakingPositionId = stakingPositionId,
    stakingFee = stakingFee,
    txNote = txNote,
)

/**
 * Execute a staking action - `withdraw`
 * [staking/chains/{chainDescriptor}/withdraw](https://developers.fireblocks.com/reference/executeaction)
 */
suspend fun FireblocksStakingService.withdraw(
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String? = null,
): String? = debitStakeAction(
    action = "withdraw",
    requestId = requestId,
    chainDescriptor = chainDescriptor,
    stakingPositionId = stakingPositionId,
    stakingFee = stakingFee,
    txNote = txNote,
)

/**
 * Execute a staking action - `claimRewards`
 * [staking/chains/{chainDescriptor}/claimRewards](https://developers.fireblocks.com/reference/executeaction)
 */
suspend fun FireblocksStakingService.claimRewards(
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String? = null,
): String? = debitStakeAction(
    action = "claimRewards",
    requestId = requestId,
    chainDescriptor = chainDescriptor,
    stakingPositionId = stakingPositionId,
    stakingFee = stakingFee,
    txNote = txNote,
)