package com.k33.platform.fireblocks.service.vault.account

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FireblocksVaultAccountServiceTest : StringSpec({
    val vaultAccountId = "76"
    "!fetch fireblocks vault account by id" {
        val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(vaultAccountId)
        vaultAccount?.id shouldBe vaultAccountId
        vaultAccount?.assets?.forEach { asset ->
            println(asset.prettyPrint())
        }
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