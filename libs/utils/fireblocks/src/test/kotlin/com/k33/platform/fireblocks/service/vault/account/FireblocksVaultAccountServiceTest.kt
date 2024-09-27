package com.k33.platform.fireblocks.service.vault.account

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe

class FireblocksVaultAccountServiceTest : StringSpec({
    val vaultAccountId = "76"
    "!fetch fireblocks vault account by id" {
        FireblocksVaultAccountService.fetchVaultAccountById("76") shouldNotBe null
    }
    "!get vault asset addresses" {
        val vaultAssets = FireblocksVaultAccountService.fetchVaultAccountById(
            vaultAccountId = vaultAccountId,
        )!!.assets!!
        vaultAssets.flatMap { vaultAsset: VaultAsset ->
            FireblocksVaultAccountService.fetchVaultAssetAddresses(
                vaultAccountId = vaultAccountId,
                vaultAssetId = vaultAsset.id!!
            )
        } shouldNotBe emptyList<VaultAssetAddress>()
    }
})