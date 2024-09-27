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
import com.k33.platform.fireblocks.service.staking.providers.StakingProvider
import com.k33.platform.fireblocks.service.staking.providers.approveTermsOfServiceByProvider
import com.k33.platform.fireblocks.service.staking.providers.getStakingProviders
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.util.UUID

class FireblocksStakingServiceTest : StringSpec({

    val logger by getLogger()

    "list supported chains" {
        val supportedChains = FireblocksStakingService.listSupportedChains()
        logger.debug("Supported chains: {}", supportedChains)
        supportedChains shouldContainAll listOf("ETH", "ETH_TEST6", "SOL", "SOL_TEST", "MATIC")
    }

    listOf("ETH", "ETH_TEST6", "SOL", "SOL_TEST", "MATIC")
        .forEach { chain ->
            "get stacking chain summary of $chain" {
                val stakingChainSummary = FireblocksStakingService.getStackingChainSummary(chainDescriptor = chain)!!
                logger.debug("{} chain staking summary: {}", chain, stakingChainSummary.prettyPrint())
                stakingChainSummary.chainDescriptor shouldBe chain
            }
        }

    "get staking positions" {
        val stakingPositions = FireblocksStakingService.getStakingPositions()
        logger.debug("Staking positions: {}", stakingPositions.prettyPrint())
        stakingPositions.map(StakingPosition::id).forEach { stakingPositionId ->
            val stakingPosition = FireblocksStakingService.getStakingPosition(stakingPositionId = stakingPositionId)
            logger.debug("Staking position for id: {} is {}", stakingPosition?.id, stakingPosition?.prettyPrint())
        }
    }

    "list staking providers" {
        val stakingProviders = FireblocksStakingService.getStakingProviders()
        logger.debug("Supported providers: {}", stakingProviders.prettyPrint())
        stakingProviders.map(StakingProvider::id) shouldContainAll listOf("figment", "kiln", "lido")
    }

    "!approve terms of service by provider" {
        FireblocksStakingService.approveTermsOfServiceByProvider("lido")
        FireblocksStakingService.approveTermsOfServiceByProvider("figment")
    }

    var stakingPositionId = ""

    "!stake SOL" {
        val requestId = UUID.randomUUID().toString()
        val stakeResponse = FireblocksStakingService.stake(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            vaultAccountId = "",
            providerId = "",
            stakeAmount = "",
            txNote = requestId,
            stakingFee = FeeLevel.MEDIUM,
        )
        logger.debug("Stake SOL response: {}", stakeResponse?.prettyPrint())
        stakeResponse?.id?.also { stakingPositionId = it }
    }

    "!stake ETH" {
        val requestId = UUID.randomUUID().toString()
        val stakeResponse = FireblocksStakingService.stake(
            requestId = requestId,
            chainDescriptor = "ETH_TEST6",
            vaultAccountId = "",
            providerId = "lido",
            stakeAmount = "0.001",
            txNote = requestId,
            stakingFee = FeeLevel.MEDIUM,
        )
        logger.debug("Stake ETH response: {}", stakeResponse?.prettyPrint())
        stakeResponse?.id?.also { stakingPositionId = it }
    }

    "!unstake" {
        val requestId = UUID.randomUUID().toString()
        val unstakeResponse = FireblocksStakingService.unstake(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
        )
        logger.debug("Unstake response: {}", unstakeResponse?.prettyPrint())
    }

    "!withdraw" {
        val requestId = UUID.randomUUID().toString()
        val withdrawResponse = FireblocksStakingService.withdraw(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )
        logger.debug("Withdraw response: {}", withdrawResponse?.prettyPrint())
    }

    "!claimRewards" {
        val requestId = UUID.randomUUID().toString()
        val claimRewardsResponse = FireblocksStakingService.claimRewards(
            requestId = requestId,
            chainDescriptor = "SOL_TEST",
            stakingPositionId = stakingPositionId,
            stakingFee = FeeLevel.MEDIUM,
            txNote = requestId,
        )
        logger.debug("Claim Rewards response: {}", claimRewardsResponse?.prettyPrint())
    }

    "!get staking position" {
        val stakingPosition = FireblocksStakingService.getStakingPosition(stakingPositionId = stakingPositionId)
        logger.debug("Staking position: {}", stakingPosition?.prettyPrint())
    }
})
