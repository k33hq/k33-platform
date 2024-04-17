package com.k33.platform.fireblocks.service

import com.k33.platform.fireblocks.client.FireblocksClient
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
        val sourceTransactions = async {
            FireblocksClient.get<List<Transaction>>(
                path = "transactions",
                "after" to after.toEpochMilli().toString(),
                "before" to before.toEpochMilli().toString(),
                "status" to "COMPLETED",
                "orderBy" to "createdAt",
                "sort" to "ASC",
                "sourceType" to "VAULT_ACCOUNT",
                "sourceId" to vaultAccountId,
                "limit" to "1"
            ) ?: emptyList()
        }
        val destinationTransactions = async {
            FireblocksClient.get<List<Transaction>>(
                path = "transactions",
                "after" to after.toEpochMilli().toString(),
                "before" to before.toEpochMilli().toString(),
                "status" to "COMPLETED",
                "orderBy" to "createdAt",
                "sort" to "ASC",
                "destType" to "VAULT_ACCOUNT",
                "destId" to vaultAccountId,
                "limit" to "10"
            ) ?: emptyList()
        }
        ((sourceTransactions.await()) + (destinationTransactions.await()))
            .sortedBy { it.createdAt }
    }

    suspend fun fetchAllSupportedAssets(): List<SupportedAsset> {
        return FireblocksClient.get<List<SupportedAsset>>(
            path = "supported_assets"
        ) ?: emptyList()
    }
}