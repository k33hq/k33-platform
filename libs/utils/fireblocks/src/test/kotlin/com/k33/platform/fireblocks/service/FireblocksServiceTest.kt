package com.k33.platform.fireblocks.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe

class FireblocksServiceTest : StringSpec({
    "fetch fireblocks vault account by id".config(enabled = false) {
        FireblocksService.fetchVaultAccountById("76") shouldNotBe null
    }
    "get vault asset addresses".config(enabled = false) {
        val vaultAssets = FireblocksService.fetchVaultAccountById(
            vaultAccountId = "76",
        )!!.assets!!
        vaultAssets.flatMap { vaultAsset: VaultAsset ->
            FireblocksService.fetchVaultAssetAddresses(
                vaultAccountId = "76",
                vaultAssetId = vaultAsset.id!!
            )
        } shouldNotBe emptyList<VaultAssetAddress>()
    }
})