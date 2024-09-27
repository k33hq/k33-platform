package com.k33.platform.fireblocks.service.transactions

//
// Resources
//

data class Transaction(
    val id: String? = null,
    // val externalTxId: String? = null,
    // https://developers.fireblocks.com/reference/primary-transaction-statuses
    // val status: String? = null,
    // https://developers.fireblocks.com/reference/transaction-substatuses
    // val subStatus: String? = null,
    // val txHash: String? = null,
    val operation: String? = null,
    // val note: String? = null,
    val assetId: String? = null,
    val source: Source? = null,
    // val sourceAddress: String? = null,
    // val tag: String? = null,
    val destination: Destination? = null,
    val destinations: List<Destinations>? = null,
    // val destinationAddress: String? = null,
    // val destinationAddressDescription: String? = null,
    // val destinationTag: String? = null,
    // contractCallDecodedData
    val amountInfo: AmountInfo? = null,
    // val treatAsGrossAmount: Boolean? = null,
    val feeInfo: FeeInfo? = null,
    val feeCurrency: String? = null,
    // networkRecords
    val createdAt: Long? = null,
    // val lastUpdated: Long? = null,
    // val createdBy: String? = null,
    // val signedBy: List<String> = emptyList(),
    // val rejectedBy: String? = null,
    // authorizationInfo
    // val exchangeTxId: String? = null,
    // val customerRefId: String? = null,
    // amlScreeningResult
    // extraParameters
    // signedMessages
    // val numOfConfirmations: Long? = null,
    // blockInfo
    // val index: Long? = null,
    // rewardInfo
    // systemMessages
    // val addressType: String? = null,
    // @Deprecated val requestedAmount: Long? = null,
    // @Deprecated val amount: Long? = null,
    // @Deprecated val netAmount: Long? = null,
    // @Deprecated val amountUSD: Long? = null,
    // @Deprecated val serviceFee: Long? = null,
    // @Deprecated val fee: Long? = null,
    // @Deprecated val networkFee: Long? = null,
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

data class Destinations(
    val destination: Destination? = null,
    // val destinationAddress: String? = null,
    // val destinationAddressDescription: String? = null,
    val amount: String? = null,
    val amountUSD: String? = null,
    // amlScreeningResult
    // val customerRefId: String? = null,
    // authorizationInfo
)

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