package com.k33.platform.app.vault.staking

import arrow.core.raise.nullable
import com.k33.platform.app.vault.VaultUserService.getVaultApp
import com.k33.platform.fireblocks.service.staking.FireblocksStakingService
import com.k33.platform.fireblocks.service.staking.chains.actions.FeeLevel
import com.k33.platform.fireblocks.service.staking.chains.actions.claimRewards
import com.k33.platform.fireblocks.service.staking.chains.actions.stake
import com.k33.platform.fireblocks.service.staking.chains.actions.unstake
import com.k33.platform.fireblocks.service.staking.chains.actions.withdraw
import com.k33.platform.fireblocks.service.staking.positions.StakingPosition
import com.k33.platform.fireblocks.service.staking.positions.StakingPositionStatus
import com.k33.platform.fireblocks.service.staking.positions.getStakingPosition
import com.k33.platform.fireblocks.service.staking.positions.getStakingPositions
import com.k33.platform.fireblocks.service.staking.providers.StakingProvider
import com.k33.platform.fireblocks.service.staking.providers.getStakingProviders
import com.k33.platform.fireblocks.service.vault.account.FireblocksVaultAccountService
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object VaultStakingService {

    private val logger by getLogger()

    suspend fun UserId.stake(
        vaultAssetId: String,
        amount: String,
        providerId: String,
    ): StakingPosition? {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            val stakingPositionId = stake(
                vaultAccountId = vaultApp.vaultAccountId,
                vaultAssetId = vaultAssetId,
                amount = amount,
                providerId = providerId,
            ) ?: return@logWithMDC null
            FireblocksStakingService.getStakingPosition(
                stakingPositionId = stakingPositionId,
            )
        }
    }

    internal suspend fun stake(
        vaultAccountId: String,
        vaultAssetId: String,
        amount: String,
        providerId: String,
    ): String? {
        // check if vault account exists in Fireblocks
        val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(
            vaultAccountId = vaultAccountId,
        )
        if (vaultAccount == null) {
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
        val allowedProviderIds = when (vaultAssetId) {
            "ETH", "ETH_TEST6" -> {
                when {
                    amount.indexOf('.') == -1 && amount.toInt() % 32 == 0  -> setOf("figment", "lido")
                    else -> setOf("lido")
                }
            }

            "SOL", "SOL_TEST" -> {
                when {
                    amount.toDouble() >= 0.01 -> setOf("figment")
                    else -> throw BadRequestException("Minimum amount has to be 0.01 SOL")
                }
            }

            else -> {
                throw BadRequestException("$vaultAssetId is currently not supported")
            }
        }
        if (!allowedProviderIds.contains(providerId)) {
            throw BadRequestException("Staking Provider: $providerId is not allowed")
        }
        // stake
        val requestId = UUID.randomUUID().toString()
        logger.info("Staking Request ID: {}", requestId)
        val stakingPositionId = FireblocksStakingService.stake(
            requestId = requestId,
            chainDescriptor = vaultAssetId,
            vaultAccountId = vaultAccountId,
            providerId = providerId,
            stakeAmount = amount,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )?.id
        logger.info("Staking Position ID: {}", stakingPositionId)
        return stakingPositionId
    }

    suspend fun UserId.unstake(
        stakingPositionId: String,
    ): StakingPosition? {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            val stakingPosition = getStakingPosition(
                vaultAccountId = vaultApp.vaultAccountId,
                stakingPositionId = stakingPositionId
            )
            unstake(
                vaultAssetId = stakingPosition.chainDescriptor,
                stakingPositionId = stakingPositionId,
            )
            FireblocksStakingService.getStakingPosition(
                stakingPositionId = stakingPositionId,
            )
        }
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

    suspend fun UserId.withdraw(
        stakingPositionId: String,
    ): StakingPosition? {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            val stakingPosition = getStakingPosition(
                vaultAccountId = vaultApp.vaultAccountId,
                stakingPositionId = stakingPositionId
            )
            withdraw(
                vaultAssetId = stakingPosition.chainDescriptor,
                stakingPositionId = stakingPositionId,
            )
            FireblocksStakingService.getStakingPosition(
                stakingPositionId = stakingPositionId,
            )
        }
    }

    internal suspend fun withdraw(
        vaultAssetId: String,
        stakingPositionId: String,
    ) {
        logger.info("Withdraw - Staking Position ID: {}", stakingPositionId)
        val requestId = UUID.randomUUID().toString()
        logger.info("Withdraw Request ID: {}", requestId)
        val withdrawResponse = FireblocksStakingService.withdraw(
            requestId = requestId,
            chainDescriptor = vaultAssetId,
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )
        logger.info("Withdraw response: {}", withdrawResponse)
    }

    suspend fun UserId.claimRewards(
        stakingPositionId: String,
    ): StakingPosition? {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            val stakingPosition = getStakingPosition(
                vaultAccountId = vaultApp.vaultAccountId,
                stakingPositionId = stakingPositionId
            )
            claimRewards(
                vaultAssetId = stakingPosition.chainDescriptor,
                stakingPositionId = stakingPositionId,
            )
            FireblocksStakingService.getStakingPosition(
                stakingPositionId = stakingPositionId,
            )
        }
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

    suspend fun getStakingPositions(
        vaultAccountId: String,
    ): List<StakingPosition> = FireblocksStakingService
        .getStakingPositions()
        .filter { it.vaultAccountId == vaultAccountId }
        .map { it.withCorrectRewardAmount() }
        .sortedByDescending { it.dateCreated }

    suspend fun UserId.getStakingPosition(
        stakingPositionId: String,
    ): StakingPosition {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return getStakingPosition(
            vaultAccountId = vaultApp.vaultAccountId,
            stakingPositionId = stakingPositionId,
        )
    }

    internal suspend fun getStakingPosition(
        vaultAccountId: String,
        stakingPositionId: String,
    ): StakingPosition {
        val stakingPosition = FireblocksStakingService.getStakingPosition(
            stakingPositionId = stakingPositionId,
        )
        if (stakingPosition == null) {
            logger.warn("Staking position not found")
            throw NotFoundException("Staking position not found")
        }
        if (vaultAccountId != stakingPosition.vaultAccountId) {
            logger.warn("Staking position does not belong to vault")
            throw BadRequestException("Staking position does not belong to your vault")
        }
        return stakingPosition.withCorrectRewardAmount()
    }

    private suspend fun StakingPosition.withCorrectRewardAmount(): StakingPosition {
        if (this.chainDescriptor.startsWith("ETH")) {
            val feePercent = getStakingProviders()
                .find { stakingProvider ->
                    stakingProvider.id == this.providerId
                }
                ?.validators
                ?.find { validator ->
                    validator.chainDescriptor == this.chainDescriptor
                }
                ?.feePercent
                ?.toBigDecimal()
                ?: BigDecimal.ZERO
            val grossRewardsAmount = this.rewardsAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val netRewardsAmount = grossRewardsAmount * (BigDecimal.ONE - feePercent / BigDecimal(100))
            return this.copy(rewardsAmount = netRewardsAmount.toPlainString())
        }
        return this
    }

    suspend fun UserId.getStakingAssets(): List<VaultStakingAsset> {
        // check if user has registered to vault app
        val vaultApp = getVaultApp()
        return getStakingAssets(
            vaultAccountId = vaultApp.vaultAccountId
        )
    }

    internal suspend fun getStakingAssets(
        vaultAccountId: String,
    ): List<VaultStakingAsset> = coroutineScope {
        fun List<VaultStakingAsset>.mergeField(
            field: (VaultStakingAsset) -> String?
        ): String? {
            return when (size) {
                0 -> null
                1 -> nullable { field(singleOrNull().bind()) }
                else -> mapNotNull { nullable { BigDecimal(field(it).bind()) } }
                    .let { it.ifEmpty { null } }
                    ?.sumOf { it }
                    // precision of 17 digits after decimal point
                    ?.setScale(17, RoundingMode.HALF_UP)
                    ?.toPlainString()
            }
        }

        val accountAssetList = async {
            FireblocksVaultAccountService
                .fetchVaultAccountById(vaultAccountId = vaultAccountId)
                ?.assets
                .let { it ?: emptyList() }
                .filter {
                    setOf(
                        // mainnet
                        "ETH",
                        "SOL",
                        "STETH_ETH",
                        // testnet
                        "ETH_TEST6",
                        "SOL_TEST",
                        "STETH_ETH_TEST6_DZFA",
                    ).contains(it.id)
                }
                .mapNotNull {
                    nullable {
                        VaultStakingAsset(
                            id = it.id.bind(),
                            available = it.available,
                            pending = null,
                            staked = null,
                        )
                    }
                }
        }
        val positionAssetList = async {
            getStakingPositionAssets(vaultAccountId = vaultAccountId)
                .map {
                    VaultStakingAsset(
                        id = it.id,
                        available = null,
                        pending = it.pending?.toPlainString(),
                        staked = it.staked?.toPlainString(),
                    )
                }
        }
        // add ETH and SOL, if absent
        val defaultList = listOf("ETH", "SOL").map {
            VaultStakingAsset(
                id = it,
                available = "0",
                pending = null,
                staked = "0",
            )
        }
        val stakedToOriginalAssetMapping = mapOf(
            "STETH_ETH_TEST6_DZFA" to "ETH_TEST6",
            "STETH_ETH" to "ETH",
        )
        return@coroutineScope (accountAssetList.await() + positionAssetList.await() + defaultList)
            .map {
                it.copy(id = stakedToOriginalAssetMapping[it.id] ?: it.id)
            }
            .groupBy(VaultStakingAsset::id)
            .mapValues { (id, list) ->
                VaultStakingAsset(
                    id = id,
                    available = list.mergeField { it.available },
                    pending = list.mergeField { it.pending },
                    staked = list.mergeField { it.staked },
                )
            }
            .values
            .toList()
    }

    enum class AssetStatus {
        STAKED,
        PROCESSING,
        DONE,
    }

    data class StakingPositionAsset(
        val id: String,
        val pending: BigDecimal?,
        val staked: BigDecimal?,
    )

    internal suspend fun getStakingPositionAssets(
        vaultAccountId: String,
    ): List<StakingPositionAsset> = getStakingPositions(vaultAccountId = vaultAccountId)
        .groupBy { it.chainDescriptor }
        .map { (chainDescriptor, stakingPositions) ->
            val assetStatusToAmountMap = stakingPositions
                .groupBy {
                    when (it.status) {
                        StakingPositionStatus.active -> AssetStatus.STAKED

                        StakingPositionStatus.pending,
                        StakingPositionStatus.creating,
                        StakingPositionStatus.activating,
                        StakingPositionStatus.withdrawing,
                        StakingPositionStatus.deactivating -> AssetStatus.PROCESSING

                        StakingPositionStatus.error,
                        StakingPositionStatus.failed,
                        StakingPositionStatus.canceled,
                        StakingPositionStatus.withdrawn,
                        StakingPositionStatus.deactivated -> AssetStatus.DONE
                    }
                }
                .mapValues { (_, positions) -> positions.sumOf { it.amount.toBigDecimal() } }
            StakingPositionAsset(
                id = chainDescriptor,
                pending = assetStatusToAmountMap[AssetStatus.PROCESSING],
                staked = assetStatusToAmountMap[AssetStatus.STAKED],
            )
        }

    suspend fun getStakingProviders(): List<StakingProvider> = FireblocksStakingService
        .getStakingProviders()
        .filter { stakingProvider ->
            stakingProvider.isTermsOfServiceApproved
                    && setOf("lido", "figment").contains(stakingProvider.id)
        }
        .map { stakingProvider ->
            stakingProvider.copy(
                validators = stakingProvider
                    .validators
                    .filter { validator ->
                        setOf("ETH", "SOL").any { prefix ->
                            validator.chainDescriptor.startsWith(prefix)
                        }
                    }
            )
        }
}