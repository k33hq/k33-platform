package com.k33.platform.fireblocks.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class FireblocksServiceTest : StringSpec({
    val vaultAccountId = "76"
    "!fetch fireblocks vault account by id" {
        FireblocksService.fetchVaultAccountById("76") shouldNotBe null
    }
    "!get vault asset addresses" {
        val vaultAssets = FireblocksService.fetchVaultAccountById(
            vaultAccountId = vaultAccountId,
        )!!.assets!!
        vaultAssets.flatMap { vaultAsset: VaultAsset ->
            FireblocksService.fetchVaultAssetAddresses(
                vaultAccountId = vaultAccountId,
                vaultAssetId = vaultAsset.id!!
            )
        } shouldNotBe emptyList<VaultAssetAddress>()
    }
})