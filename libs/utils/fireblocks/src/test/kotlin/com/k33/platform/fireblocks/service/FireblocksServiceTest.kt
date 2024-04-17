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
    "fetch fireblocks vault account by id".config(enabled = false) {
        FireblocksService.fetchVaultAccountById("76") shouldNotBe null
    }
    "get vault asset addresses".config(enabled = false) {
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
    "fetch transactions".config(enabled = false) {
        val zoneId = ZoneId.of("Europe/Oslo")
        val zoneOffset = ZoneOffset.of("+01:00")
        val after = ZonedDateTime.ofLocal(LocalDateTime.parse("2023-01-01T00:00:00"), zoneId, zoneOffset).toInstant()
            .minusMillis(1)
        println("after: $after")
        val before = ZonedDateTime.ofLocal(LocalDateTime.parse("2024-01-01T00:00:00"), zoneId, zoneOffset).toInstant()
        println("before: $before")
        val transactions = FireblocksService.fetchTransactions(
            vaultAccountId = vaultAccountId,
            after = after,
            before = before,
        )
        transactions.forEach(::println)
        println("CreatedAt,Operation,Direction,Asset,Amount,Fee")
        transactions.forEach { transaction ->
            val createdAt =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(transaction.createdAt!!), zoneId).toLocalDateTime()
            val amount = transaction.amountInfo?.amountUSD!!
            val fee = transaction.feeInfo?.let {
                (it.serviceFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) +
                        (it.networkFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) +
                        (it.gasPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            } ?: BigDecimal.ZERO
            val direction = when {
                transaction.source?.type == "VAULT_ACCOUNT" && transaction.source?.id == vaultAccountId -> "DEBIT"
                transaction.destination?.type == "VAULT_ACCOUNT" && transaction.destination?.id == vaultAccountId -> "CREDIT"
                else -> null
            }
            println("$createdAt,${transaction.operation},$direction,${transaction.assetId},USD $amount,${transaction.feeCurrency} ${fee.toPlainString()}")
        }
    }
})