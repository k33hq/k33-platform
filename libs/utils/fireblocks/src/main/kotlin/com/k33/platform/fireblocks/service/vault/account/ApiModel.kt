package com.k33.platform.fireblocks.service.vault.account

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
    @Deprecated(message = "Returns incorrect data in Fireblocks API")
    val pending: String? = null,
    val frozen: String? = null,
    val lockedAmount: String? = null,
    @Deprecated(message = "Deprecated in Fireblocks API")
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