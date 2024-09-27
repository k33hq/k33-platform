package com.k33.platform.app.vault.pdf

import com.k33.platform.app.vault.VaultApp
import com.k33.platform.app.vault.VaultService
import io.kotest.core.spec.style.StringSpec
import java.io.FileOutputStream
import java.time.LocalDate

class BalanceReportTest: StringSpec({
    "!generate balance report" {
        val vaultApp = VaultApp(
            vaultAccountId = "76",
            currency = "NOK"
        )
        val vaultAssets = VaultService.getVaultAssets(
            vaultApp = vaultApp,
            currency = null,
        )
        val reportFileContents = getBalanceReport(
            name = "Test User Name",
            address = listOf("Test Street 123", "1234 City", "Country"),
            date = LocalDate.now().minusDays(1),
            vaultAssets = vaultAssets,
        )
        FileOutputStream("balance-report.pdf").use { fos ->
            fos.write(reportFileContents)
        }
    }
})