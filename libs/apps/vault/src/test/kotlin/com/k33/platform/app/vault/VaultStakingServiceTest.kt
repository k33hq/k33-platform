package com.k33.platform.app.vault

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec

class VaultStakingServiceTest: StringSpec({

    val vaultAccountId = "238"

    var stakingPositionId: String? = null

    "!stake with provider:figment" {
        stakingPositionId = VaultStakingService.stake(
            vaultAccountId = vaultAccountId,
            vaultAssetId = "SOL_TEST",
            amount = "0.01"
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

    "!get staking summary" {
        val stakingSummary = VaultStakingService.getStakingSummary(
            vaultAccountId = vaultAccountId,
        )!!
        println(stakingSummary.prettyPrint())
    }
})