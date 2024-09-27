package com.k33.platform.app.vault.staking

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VaultStakingServiceTest: StringSpec({

    val vaultAccountId = ""

    var stakingPositionId: String? = null

    "!stake with provider:figment" {
        stakingPositionId = VaultStakingService.stake(
            vaultAccountId = vaultAccountId,
            vaultAssetId = "SOL_TEST",
            amount = "0.01"
        )
    }

    "!unstake with provider:figment" {
        VaultStakingService.unstake(
            vaultAssetId = "SOL_TEST",
            stakingPositionId = stakingPositionId!!,
        )
    }

    "!claim rewards with provider:figment" {
        VaultStakingService.claimRewards(
            vaultAssetId = "SOL_TEST",
            stakingPositionId = stakingPositionId!!,
        )
    }

    "!stake with provider:lido" {
        stakingPositionId = VaultStakingService.stake(
            vaultAccountId = vaultAccountId,
            vaultAssetId = "ETH_TEST6",
            amount = "0.001"
        )
    }

    "!get staking positions" {
        val stakingPositions = VaultStakingService.getStakingPositions(
            vaultAccountId = vaultAccountId,
        )
        println(stakingPositions.prettyPrint())
        // stakingPositions.map(StakingPosition::id) shouldContain stakingPositionId
    }

    "!get staking position" {
        val stakingPosition = VaultStakingService.getStakingPosition(
            vaultAccountId = vaultAccountId,
            stakingPositionId = stakingPositionId!!,
        )
        println(stakingPosition.prettyPrint())
        stakingPosition.id shouldBe stakingPositionId
    }

    "!get staking summary" {
        val stakingSummary = VaultStakingService.getStakingSummary(
            vaultAccountId = vaultAccountId,
        )!!
        println(stakingSummary.prettyPrint())
    }
})