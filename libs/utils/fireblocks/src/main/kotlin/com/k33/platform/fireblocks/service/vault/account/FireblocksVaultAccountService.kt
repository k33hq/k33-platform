package com.k33.platform.fireblocks.service.vault.account

import com.k33.platform.fireblocks.client.FireblocksClient

object FireblocksVaultAccountService {

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
}