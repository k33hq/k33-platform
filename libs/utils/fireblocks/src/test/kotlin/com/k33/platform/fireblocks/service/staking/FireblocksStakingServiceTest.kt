package com.k33.platform.fireblocks.service.staking

import com.k33.platform.fireblocks.service.staking.chains.actions.FeeLevel
import com.k33.platform.fireblocks.service.staking.chains.actions.claimRewards
import com.k33.platform.fireblocks.service.staking.chains.actions.stake
import com.k33.platform.fireblocks.service.staking.chains.actions.unstake
import com.k33.platform.fireblocks.service.staking.chains.actions.withdraw
import com.k33.platform.fireblocks.service.staking.chains.getStackingChainSummary
import com.k33.platform.fireblocks.service.staking.chains.listSupportedChains
import com.k33.platform.fireblocks.service.staking.positions.StakingPosition
import com.k33.platform.fireblocks.service.staking.positions.getStakingPosition
import com.k33.platform.fireblocks.service.staking.positions.getStakingPositions
import com.k33.platform.fireblocks.service.staking.positions.summary.getStakingSummary
import com.k33.platform.fireblocks.service.staking.positions.summary.getStakingSummaryByVault
import com.k33.platform.fireblocks.service.staking.providers.StakingProvider
import com.k33.platform.fireblocks.service.staking.providers.approveTermsOfServiceByProvider
import com.k33.platform.fireblocks.service.staking.providers.listStakingProviders
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.util.UUID

class FireblocksStakingServiceTest : StringSpec({

    val logger by getLogger()

    "!list supported chains" {
        val supportedChains = FireblocksStakingService.listSupportedChains()
        logger.debug("Supported chains: {}", supportedChains)
        supportedChains shouldContainAll listOf("ETH", "ETH_TEST6", "SOL", "SOL_TEST", "MATIC")
    }

    listOf("ETH", "ETH_TEST6", "SOL", "SOL_TEST", "MATIC")
        .forEach { chain ->
            "!get stacking chain summary of $chain" {
                val stakingChainSummary = FireblocksStakingService.getStackingChainSummary(chainDescriptor = chain)!!
                logger.debug("{} chain staking summary: {}", chain, stakingChainSummary.prettyPrint())
                stakingChainSummary.chainDescriptor shouldBe chain
            }
        }

    "!get staking positions" {
        val stakingPositions = FireblocksStakingService.getStakingPositions()
        logger.debug("Staking positions: {}", stakingPositions.prettyPrint())
        stakingPositions.map(StakingPosition::id).forEach { stakingPositionId ->
            val stakingPosition = FireblocksStakingService.getStakingPosition(id = stakingPositionId)!!
            logger.debug("Staking position for id: {} is {}", stakingPosition.id, stakingPosition.prettyPrint())
        }
    }

    "!get staking summary" {
        val stakingSummary = FireblocksStakingService.getStakingSummary()!!
        logger.debug("Staking summary: {}", stakingSummary.prettyPrint())
    }

    "!get staking summary by vault" {
        val stakingSummary = FireblocksStakingService.getStakingSummaryByVault()!!
        logger.debug("Staking summary by vault: {}", stakingSummary.prettyPrint())
    }

    "!list staking providers" {
        val stakingProviders = FireblocksStakingService.listStakingProviders()
        logger.debug("Supported providers: {}", stakingProviders.prettyPrint())
        stakingProviders.map(StakingProvider::id) shouldContainAll listOf("figment", "kiln", "lido")
    }

    "!approve terms of service by provider" {
        FireblocksStakingService.approveTermsOfServiceByProvider("lido")
        FireblocksStakingService.approveTermsOfServiceByProvider("figment")
    }

    var stakingPositionId = ""
    "!stake" {
        val requestId = UUID.randomUUID().toString()
        val stakeResponse = FireblocksStakingService.stake(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            vaultAccountId = "",
            providerId = "",
            stakeAmount = "",
            txNote = "",
            stakingFee = FeeLevel.MEDIUM,
        )!!
        logger.debug("Stake response: {}", stakeResponse.prettyPrint())
        stakingPositionId = stakeResponse.id
    }

    "!unstake" {
        val requestId = UUID.randomUUID().toString()
        val unstakeResponse = FireblocksStakingService.unstake(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
//            txNote = "",
        )!!
        logger.debug("Unstake response: {}", unstakeResponse.prettyPrint())
    }

    "!withdraw" {
        val requestId = UUID.randomUUID().toString()
        val withdrawResponse = FireblocksStakingService.withdraw(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
//            txNote = "",
        )!!
        logger.debug("Withdraw response: {}", withdrawResponse.prettyPrint())
    }

    "!claimRewards" {
        val requestId = UUID.randomUUID().toString()
        val claimRewardsResponse = FireblocksStakingService.claimRewards(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
//            txNote = "",
        )!!
        logger.debug("Claim Rewards response: {}", claimRewardsResponse.prettyPrint())
    }
})
