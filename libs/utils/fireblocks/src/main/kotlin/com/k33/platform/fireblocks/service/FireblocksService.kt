package com.k33.platform.fireblocks.service

import com.k33.platform.fireblocks.client.FireblocksClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant

object FireblocksService {

    // https://developers.fireblocks.com/reference/get_vault-accounts-vaultaccountid
    suspend fun fetchVaultAccountById(vaultAccountId: String): VaultAccount? = FireblocksClient.get(
        path = "vault/accounts/$vaultAccountId",
    )

    // https://developers.fireblocks.com/reference/get_vault-accounts-vaultaccountid-assetid
    suspend fun fetchVaultAsset(
        vaultAccountId: String,
        vaultAssetId: String,
    ): VaultAsset? = FireblocksClient.get(
        path = "vault/accounts/$vaultAccountId/$vaultAssetId",
    )

    // https://developers.fireblocks.com/reference/get_vault-accounts-vaultaccountid-assetid-addresses-paginated
    suspend fun fetchVaultAssetAddresses(
        vaultAccountId: String,
        vaultAssetId: String,
    ): List<VaultAssetAddress> {
        val vaultAssetAddresses: VaultAssetAddresses? = FireblocksClient.get(
            path = "vault/accounts/${vaultAccountId}/${vaultAssetId}/addresses_paginated"
        )
        return vaultAssetAddresses?.addresses ?: emptyList()
    }

    // https://developers.fireblocks.com/reference/get_transactions
    suspend fun fetchTransactions(
        vaultAccountId: String,
        after: Instant,
        before: Instant,
    ): List<Transaction> = coroutineScope {
        // page limit. default: 200, max: 500
        val limit = 500

        // loop to get all pages
        suspend fun fetchPaginatedTransactionsAsync(
            nextPage: suspend (nextAfter: String) -> List<Transaction>?,
        ): Deferred<List<Transaction>> {
            return async {
                val transactionsList = mutableListOf<Transaction>()
                var nextAfter: String = after.toEpochMilli().toString()
                do {
                    val transactions = nextPage(nextAfter)
                    if (transactions != null) {
                        transactionsList.addAll(transactions)
                        nextAfter = transactions.lastOrNull()?.createdAt?.toString() ?: ""
                    }
                } while (transactions?.size == limit)
                transactionsList
            }
        }

        val commonQueryParams = arrayOf(
            "before" to before.toEpochMilli().toString(),
            "status" to "COMPLETED",
            "orderBy" to "createdAt",
            "sort" to "ASC",
            "limit" to limit.toString()
        )
        // list of transactions where given vault account is source
        val sourceTransactions = fetchPaginatedTransactionsAsync { nextAfter ->
            FireblocksClient.get<List<Transaction>>(
                path = "transactions",
                *commonQueryParams,
                "after" to nextAfter,
                "sourceType" to "VAULT_ACCOUNT",
                "sourceId" to vaultAccountId,
            )
        }
        // list of transactions where given vault account is destination
        val destinationTransactions = fetchPaginatedTransactionsAsync { nextAfter ->
            FireblocksClient.get<List<Transaction>>(
                path = "transactions",
                *commonQueryParams,
                "after" to nextAfter,
                "destType" to "VAULT_ACCOUNT",
                "destId" to vaultAccountId,
            )
        }
        // merge transactions and sort by created
        ((sourceTransactions.await()) + (destinationTransactions.await()))
            .sortedBy { it.createdAt }
    }

    suspend fun fetchAllSupportedAssets(): List<SupportedAsset> {
        return FireblocksClient.get<List<SupportedAsset>>(
            path = "supported_assets"
        ) ?: emptyList()
    }
}