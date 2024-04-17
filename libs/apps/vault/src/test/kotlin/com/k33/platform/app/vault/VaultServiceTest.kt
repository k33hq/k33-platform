package com.k33.platform.app.vault

import io.kotest.core.spec.style.StringSpec
import java.time.LocalDate

class VaultServiceTest : StringSpec({
    "get vault assets".config(enabled = false) {
        val vaultApp = VaultApp(
            vaultAccountId = "76",
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
    "generate vault account balance reports".config(enabled = false) {
        VaultService.generateVaultAccountBalanceReports(
            date = LocalDate.now().minusDays(1),
            mode = Mode.FETCH,
        )
    }
})