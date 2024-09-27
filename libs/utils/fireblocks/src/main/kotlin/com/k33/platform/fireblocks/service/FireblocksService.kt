package com.k33.platform.fireblocks.service

import com.k33.platform.fireblocks.client.FireblocksClient

object FireblocksService {

    suspend fun fetchAllSupportedAssets(): List<SupportedAsset> {
        return FireblocksClient.get<List<SupportedAsset>>(
            path = "supported_assets"
        ) ?: emptyList()
    }
}