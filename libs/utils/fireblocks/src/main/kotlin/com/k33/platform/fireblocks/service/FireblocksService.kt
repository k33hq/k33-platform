package com.k33.platform.fireblocks.service

import com.k33.platform.fireblocks.client.FireblocksClient

object FireblocksService {

    // https://developers.fireblocks.com/reference/list-supported-assets
    suspend fun fetchAllSupportedAssets(): List<SupportedAsset> {
        return FireblocksClient.get<List<SupportedAsset>>(
            path = "supported_assets"
        ) ?: emptyList()
    }
}