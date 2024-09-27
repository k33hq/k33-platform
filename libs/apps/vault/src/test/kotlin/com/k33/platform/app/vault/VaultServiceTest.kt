package com.k33.platform.app.vault

import io.kotest.core.spec.style.StringSpec
import java.time.LocalDate

class VaultServiceTest : StringSpec({

    val vaultAccountId = "76"

    "!get vault assets" {
        val vaultApp = VaultApp(
            vaultAccountId = vaultAccountId,
            currency = "USD"
        )
        val vaultAssets = VaultService.getVaultAssets(
            vaultApp = vaultApp,
            currency = null,
        )
        vaultAssets.forEach { vaultAsset: VaultAsset ->
            with(vaultAsset) {
                println("$id $available $rate $fiatValue")
            }
        }
    }
    "!generate vault account balance reports" {
        VaultService.generateVaultAccountBalanceReports(
            date = LocalDate.now().minusDays(1),
            mode = Mode.FETCH,
        )
    }
})
