package com.k33.platform.app.vault

import com.k33.platform.app.vault.VaultUserService.getVaultApp
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService
import com.k33.platform.fireblocks.service.staking.chains.actions.FeeLevel
import com.k33.platform.fireblocks.service.staking.chains.actions.claimRewards
import com.k33.platform.fireblocks.service.staking.chains.actions.stake
import com.k33.platform.fireblocks.service.staking.chains.actions.unstake
import com.k33.platform.fireblocks.service.staking.positions.StakingPosition
import com.k33.platform.fireblocks.service.staking.positions.getStakingPositions
import com.k33.platform.fireblocks.service.staking.positions.summary.StakingSummary
import com.k33.platform.fireblocks.service.staking.positions.summary.getStakingSummaryByVault
import com.k33.platform.fireblocks.service.vault.account.FireblocksVaultAccountService
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import io.ktor.server.plugins.BadRequestException
import java.util.UUID

object VaultStakingService {

    private val logger by getLogger()

    suspend fun UserId.stake(
        vaultAssetId: String,
        amount: String,
    ) {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        stake(
            vaultAccountId = vaultApp.vaultAccountId,
            vaultAssetId = vaultAssetId,
            amount = amount,
        )
    }

    internal suspend fun stake(
        vaultAccountId: String,
        vaultAssetId: String,
        amount: String,
    ): String? {
        // check if vault account exists in Fireblocks
        val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(
            vaultAccountId = vaultAccountId,
        ) ?: run {
            logger.warn(
                NotifySlack.ALERTS,
                "No vault account (id: $vaultAccountId) found in Fireblocks"
            )
            throw BadRequestException("No vault account found in Fireblocks")
        }
        // check if vault asset exists in vault account in Fireblocks
        val vaultAsset = vaultAccount
            .assets
            ?.find { vaultAssetId == it.id }
            ?: run {
                logger.warn(
                    NotifySlack.ALERTS,
                    "No vault asset (id: $vaultAssetId) found in Fireblocks"
                )
                throw BadRequestException("No vault asset found in Fireblocks")
            }
        // check if minimum balance required will be maintained after stake operation
        val minimumAvailableBalanceRequired = mapOf(
            "ETH" to 0.01,
            "ETH_TEST6" to 0.01,
            "SOL" to 0.01,
            "SOL_TEST" to 0.01,
        )
        val minimumBalanceRequired = minimumAvailableBalanceRequired[vaultAssetId.lowercase()] ?: 0.01
        val availableBalance = vaultAsset.available?.toDouble() ?: 0.0
        if (availableBalance - amount.toDouble() < minimumBalanceRequired) {
            throw BadRequestException("Insufficient balance available")
        }
        // choose best provider
        val provider = when (vaultAssetId) {
            "ETH", "ETH_TEST6" -> {
                when {
                    amount.toDouble() < 32 -> "lido"
                    amount.toInt() % 32 == 0 && amount.indexOf('.') == -1 -> "figment"
                    else -> "lido" // with warning for low rewards
                }
            }

            "SOL", "SOL_TEST" -> {
                when {
                    amount.toDouble() >= 0.01 -> "figment"
                    else -> throw BadRequestException("Minimum amount has to be 0.01 SOL")
                }
            }

            else -> {
                throw BadRequestException("$vaultAssetId is currently not supported")
            }
        }
        // stake
        val requestId = UUID.randomUUID().toString()
        logger.info("Staking Request ID: {}", requestId)
        val stakingPositionId = FireblocksStakingService.stake(
            requestId = requestId,
            chainDescriptor = vaultAssetId,
            vaultAccountId = vaultAccountId,
            providerId = provider,
            stakeAmount = amount,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )?.id
        logger.info("Staking Position ID: {}", stakingPositionId)
        return stakingPositionId
    }

    internal suspend fun unstake(
        vaultAssetId: String,
        stakingPositionId: String,
    ) {
        logger.info("Unstaking - Staking Position ID: {}", stakingPositionId)
        val requestId = UUID.randomUUID().toString()
        logger.info("Unstaking Request ID: {}", requestId)
        val unstakeResponse = FireblocksStakingService.unstake(
            requestId = requestId,
            chainDescriptor = vaultAssetId,
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )
        logger.info("Unstake response: {}", unstakeResponse)
    }

    internal suspend fun claimRewards(
        vaultAssetId: String,
        stakingPositionId: String,
    ) {
        logger.info("Claiming rewards - Staking Position ID: {}", stakingPositionId)
        val requestId = UUID.randomUUID().toString()
        logger.info("Claiming rewards Request ID: {}", requestId)
        val claimRewardsResponse = FireblocksStakingService.claimRewards(
            requestId = requestId,
            chainDescriptor = vaultAssetId,
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )
        logger.info("Claim rewards response: {}", claimRewardsResponse)
    }

    suspend fun UserId.getStakingPositions(): List<StakingPosition> {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return getStakingPositions(
            vaultAccountId = vaultApp.vaultAccountId
        )
    }

    internal suspend fun getStakingPositions(
        vaultAccountId: String,
    ): List<StakingPosition> = FireblocksStakingService
        .getStakingPositions()
        .filter { it.vaultAccountId == vaultAccountId }

    suspend fun UserId.getStakingSummary(): StakingSummary? {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return getStakingSummary(
            vaultAccountId = vaultApp.vaultAccountId
        )
    }

    internal suspend fun getStakingSummary(
        vaultAccountId: String,
    ): StakingSummary? = FireblocksStakingService
        .getStakingSummaryByVault()
        ?.get(vaultAccountId)
}