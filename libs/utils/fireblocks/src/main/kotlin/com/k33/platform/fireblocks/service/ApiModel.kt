package com.k33.platform.fireblocks.service

//
// Resources
//

data class VaultAccount(
    val id: String? = null,
    val name: String? = null,
    val assets: List<VaultAsset>? = null,
    val hiddenOnUI: Boolean? = null,
    val customerRefId: String? = null,
    val autoFuel: Boolean? = null,
)

data class VaultAsset(
    val id: String? = null,
    val total: String? = null,
    val available: String? = null,
    val pending: String? = null,
    val frozen: String? = null,
    val lockedAmount: String? = null,
    val staked: String? = null,
)

data class VaultAssetAddresses(
    val addresses: List<VaultAssetAddress>
)

data class VaultAssetAddress(
    val assetId: String? = null,
    val address: String? = null,
    val description: String? = null,
    val tag: String? = null,
    val type: String? = null,
    val customerRefId: String? = null,
    val addressFormat: String? = null,
    val enterpriseAddress: String? = null,
    val bip44AddressIndex: Long? = null,
    val userDefined: Boolean? = null,
)

data class SupportedAsset(
    val id: String,
    val name: String,
    val type: String,
    val contractAddress: String? = null,
    val nativeAsset: String? = null,
    val decimals: Double? = null,
)