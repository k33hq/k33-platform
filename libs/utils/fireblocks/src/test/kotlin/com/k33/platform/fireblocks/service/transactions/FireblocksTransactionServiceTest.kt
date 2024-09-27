package com.k33.platform.fireblocks.service.transactions

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.StringSpec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class FireblocksTransactionServiceTest : StringSpec({
    "get transactions" {
        val today = LocalDate.now()
        val transactions = FireblocksTransactionService.fetchTransactions(
            vaultAccountId = "238",
            after = Instant.ofEpochSecond(
                today.minusDays(7).toEpochSecond(LocalTime.MIN, ZoneOffset.UTC)
            ),
            before = Instant.ofEpochSecond(
                today.toEpochSecond(LocalTime.MAX, ZoneOffset.UTC)
            ),
        )
        transactions.forEach { transaction -> println(transaction.prettyPrint()) }
    }
})