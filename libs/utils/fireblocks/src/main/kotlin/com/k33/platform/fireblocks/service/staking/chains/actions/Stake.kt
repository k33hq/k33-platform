package com.k33.platform.fireblocks.service.staking.chains.actions

import com.k33.platform.fireblocks.client.FireblocksClient
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService

//
// staking/chains/$chainDescriptor/stake
//

data class StakeRequest(
    val vaultAccountId: String,
    val providerId: String,
    val stakeAmount: String,
    val txNote: String? = null,
    val fee: String? = null,
    val feeLevel: String? = null,
)

data class StakeResponse(
    val id: String,
)

/**
 * Execute a staking action - `stake`
 * [staking/chains/{chainDescriptor}/stake](https://developers.fireblocks.com/reference/executeaction)
 */
suspend fun FireblocksStakingService.stake(
    requestId: String,
    chainDescriptor: String,
    vaultAccountId: String,
    providerId: String,
    stakeAmount: String,
    txNote: String,
    stakingFee: StakingFee,
): StakeResponse? = FireblocksClient.post(
    path = "staking/chains/$chainDescriptor/stake",
    "Idempotency-Key" to requestId,
) {
    val (fee, feeLevel) = stakingFee
    StakeRequest(
        vaultAccountId = vaultAccountId,
        providerId = providerId,
        stakeAmount = stakeAmount,
        txNote = txNote,
        fee = fee?.toString(),
        feeLevel = feeLevel,
    )
}