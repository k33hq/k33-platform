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
    val fee: String?,
    val feeLevel: String?,
    val txNote: String?,
    // used by action == unstake only.
    val amount: String?,
)

private suspend fun debitStakeAction(
    action: String,
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String?,
    amount: String?,
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
        amount = amount,
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
    amount = null,
)

/**
 * Execute a staking action - `unstake`
 * [staking/chains/{chainDescriptor}/unstake](https://developers.fireblocks.com/reference/executeaction)
 */
suspend fun FireblocksStakingService.partialUnstake(
    requestId: String,
    chainDescriptor: String,
    stakingPositionId: String,
    stakingFee: StakingFee,
    txNote: String? = null,
    amount: String,
): String? = debitStakeAction(
    action = "unstake",
    requestId = requestId,
    chainDescriptor = chainDescriptor,
    stakingPositionId = stakingPositionId,
    stakingFee = stakingFee,
    txNote = txNote,
    amount = amount,
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
    amount = null,
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
    amount = null,
)