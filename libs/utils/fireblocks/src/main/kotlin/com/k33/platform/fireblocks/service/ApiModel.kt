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

data class Transaction(
    val id: String? = null,
    // https://developers.fireblocks.com/reference/primary-transaction-statuses
    val status: String? = null,
    // https://developers.fireblocks.com/reference/transaction-substatuses
    val subStatus: String? = null,
    val operation: String? = null,
    val assetId: String? = null,
    val source: Source? = null,
    val sourceAddress: String? = null,
    val tag: String? = null,
    val destination: Destination? = null,
    val destinationAddress: String? = null,
    val destinationAddressDescription: String? = null,
    val destinationTag: String? = null,
    val amountInfo: AmountInfo? = null,
    val feeInfo: FeeInfo? = null,
    val feeCurrency: String? = null,
    val createdAt: Long? = null,
    val lastUpdated: Long? = null,
    val createdBy: String? = null,
    val signedBy: List<String> = emptyList(),
    val rejectedBy: String? = null,
)

data class TxnSrcDest(
    val type: String? = null,
    val subType: String? = null,
    val id: String? = null,
    val name: String? = null,
    val walletId: String? = null,
)
typealias Source = TxnSrcDest
typealias Destination = TxnSrcDest

data class AmountInfo(
    val amount: String? = null,
    val requestedAmount: String? = null,
    val netAmount: String? = null,
    val amountUSD: String? = null,
)

data class FeeInfo(
    val networkFee: String? = null,
    val serviceFee: String? = null,
    val gasPrice: String? = null,
)

data class SupportedAsset(
    val id: String,
    val name: String,
    val type: String,
    val contractAddress: String? = null,
    val nativeAsset: String? = null,
    val decimals: Double? = null,
)
