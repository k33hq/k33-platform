package com.k33.platform.fireblocks.service

//
// Resources
//

data class SupportedAsset(
    val id: String,
    val name: String,
    val type: String,
    val contractAddress: String? = null,
    val nativeAsset: String? = null,
    val decimals: Double? = null,
)
