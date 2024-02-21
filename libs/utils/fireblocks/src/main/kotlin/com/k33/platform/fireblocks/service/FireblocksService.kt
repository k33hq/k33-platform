package com.k33.platform.fireblocks.service

import com.k33.platform.fireblocks.client.FireblocksClient

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

    suspend fun fetchAllSupportedAssets(): List<SupportedAsset> {
        return FireblocksClient.get<List<SupportedAsset>>(
            path = "supported_assets"
        ) ?: emptyList()
    }
}