package com.k33.platform.app.vault.staking

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VaultStakingServiceTest: StringSpec({

    val vaultAccountId = "238"

    var stakingPositionId: String? = null

    "!stake with provider:figment" {
        stakingPositionId = VaultStakingService.stake(
            vaultAccountId = vaultAccountId,
            vaultAssetId = "SOL_TEST",
            amount = "0.01",
            providerId = "figment",
        )
    }

    "!unstake with provider:figment" {
        VaultStakingService.unstake(
            vaultAssetId = "SOL_TEST",
            stakingPositionId = stakingPositionId!!,
        )
    }

    "!withdraw with provider:figment" {
        VaultStakingService.withdraw(
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
            amount = "0.001",
            providerId = "lido",
        )
    }

    "!unstake with provider:lido" {
        VaultStakingService.unstake(
            vaultAssetId = "ETH_TEST6",
            stakingPositionId = stakingPositionId!!,
        )
    }

    //
    "get staking positions" {
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

    "get staking position assets" {
        val stakingPositionAssets = VaultStakingService.getStakingPositionAssets(
            vaultAccountId = vaultAccountId,
        )
        println(stakingPositionAssets.prettyPrint())
    }

    "get staking assets" {
        val stakingAssets = VaultStakingService.getStakingAssets(
            vaultAccountId = vaultAccountId,
        )
        println(stakingAssets.prettyPrint())
    }
})