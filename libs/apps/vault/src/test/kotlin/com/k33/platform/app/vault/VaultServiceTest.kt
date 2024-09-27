package com.k33.platform.app.vault

import io.kotest.core.spec.style.StringSpec
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class VaultServiceTest : StringSpec({

    val vaultAccountId = "238"

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
                println("$id $available $pending $staked $total $rate $fiatValue")
            }
        }
    }
    "!generate vault account balance reports" {
        VaultService.generateVaultAccountBalanceReports(
            date = LocalDate.now().minusDays(1),
            mode = Mode.FETCH,
        )
    }
    "!generate transactions csv" {
        val transactions = VaultService.getTransactions(
            vaultAccountId = vaultAccountId,
            dateRange = Instant.now().minus(7, ChronoUnit.DAYS) to Instant.now(),
            zoneId = ZoneId.of("Europe/London"),
        )
        println(toCsv(transactions))
    }
})
