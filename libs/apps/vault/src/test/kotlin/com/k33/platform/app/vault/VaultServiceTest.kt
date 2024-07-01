package com.k33.platform.app.vault

import com.k33.platform.app.vault.VaultService.validate
import com.k33.platform.app.vault.stripe.StripeService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
    "validate user's stripe customer details" {
        StripeService
            .getCustomerDetails(email = "test@k33.com")
            .validate() shouldBe emptyList()
    }
})