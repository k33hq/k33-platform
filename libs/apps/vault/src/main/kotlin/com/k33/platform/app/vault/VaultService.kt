package com.k33.platform.app.vault

import arrow.core.raise.nullable
import com.k33.platform.app.vault.VaultUserService.getVaultApp
import com.k33.platform.app.vault.VaultUserService.validationErrors
import com.k33.platform.app.vault.coingecko.CoinGeckoClient
import com.k33.platform.app.vault.pdf.getBalanceReport
import com.k33.platform.app.vault.staking.VaultStakingService
import com.k33.platform.app.vault.stripe.StripeService
import com.k33.platform.filestore.FileStoreService
import com.k33.platform.fireblocks.service.transactions.FireblocksTransactionService
import com.k33.platform.fireblocks.service.transactions.TxnSrcDest
import com.k33.platform.fireblocks.service.vault.account.FireblocksVaultAccountService
import com.k33.platform.identity.auth.gcp.FirebaseAuthService
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.logging.logWithMDC
import io.firestore4k.typed.FirestoreClient
import io.firestore4k.typed.div
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import com.k33.platform.fireblocks.service.vault.account.VaultAsset as FireblocksVaultAsset

object VaultService {

    private val logger by getLogger()

    private val firestoreClient by lazy { FirestoreClient() }

    suspend fun UserId.getVaultAssets(currency: String?): List<VaultAsset> {
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            getVaultAssets(
                vaultApp = vaultApp,
                currency = currency
            )
        }
    }

    internal suspend fun getVaultAssets(
        vaultApp: VaultApp,
        currency: String?,
    ): List<VaultAsset> {

        val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(
            vaultAccountId = vaultApp.vaultAccountId,
        )

        if (vaultAccount == null) {
            logger.warn(
                NotifySlack.ALERTS,
                "No vault account found in Fireblocks"
            )
            return emptyList()
        }

        val stakingAssetsMap = VaultStakingService
            .getStakingPositionAssets(vaultAccountId = vaultApp.vaultAccountId)
            .associateBy { it.id }

        val vaultAssetsMap = vaultAccount
            .assets
            .let { it ?: emptyList() }
            .mapNotNull {
                nullable {
                    it.id.bind() to it.copy(
                        pending = stakingAssetsMap[it.id]?.pending?.toPlainString(),
                        staked = stakingAssetsMap[it.id]?.staked?.toPlainString(),
                    )
                }
            }
            .toMap()
            .toMutableMap()
            .apply {
                stakingAssetsMap.forEach { (id, asset) ->
                    putIfAbsent(
                        id,
                        FireblocksVaultAsset(
                            id = id,
                            pending = asset.pending?.toPlainString(),
                            staked = asset.staked?.toPlainString(),
                        ),
                    )
                }
            }
            .toMap()

        val baseCurrency = currency ?: vaultApp.currency

        val cryptoCurrencyToTotalMap: Map<String, Double> = vaultAssetsMap
            .mapValues { (_, vaultAsset) ->
                (vaultAsset.available?.toDoubleOrNull() ?: 0.0) +
                        (@Suppress("DEPRECATION") vaultAsset.pending?.toDoubleOrNull() ?: 0.0) +
                        (@Suppress("DEPRECATION") vaultAsset.staked?.toDoubleOrNull() ?: 0.0)
            }
            .filterValues { it > 0.0 }

        val cryptoCurrencyList = cryptoCurrencyToTotalMap
            .keys
            .toList()

        val fxMap = CoinGeckoClient.getFxRates(
            baseCurrency = baseCurrency,
            currencyList = cryptoCurrencyList
        )

        return vaultAssetsMap
            .map { (cryptocurrency, vaultAsset) ->
                VaultAsset(
                    id = cryptocurrency,
                    available = vaultAsset.available?.toDoubleOrNull(),
                    pending = @Suppress("DEPRECATION") vaultAsset.pending?.toDoubleOrNull(),
                    staked = @Suppress("DEPRECATION") vaultAsset.staked?.toDoubleOrNull(),
                    total = cryptoCurrencyToTotalMap[cryptocurrency] ?: 0.0,
                    rate = nullable {
                        Amount(
                            value = fxMap[cryptocurrency]?.rate.bind(),
                            currency = baseCurrency,
                        )
                    },
                    fiatValue = nullable {
                        Amount(
                            value = (cryptoCurrencyToTotalMap[cryptocurrency].bind() * fxMap[cryptocurrency]?.rate.bind()),
                            currency = baseCurrency,
                        )
                    },
                    dailyPercentChange = fxMap[cryptocurrency]
                        ?.percentChangeIn24hr,
                )
            }
            .sortedByDescending { it.fiatValue?.value }
    }

    suspend fun UserId.getVaultAddresses(
        vaultAssetId: String
    ): List<VaultAssetAddress> {
        val vaultApp = getVaultApp()
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            FireblocksVaultAccountService.fetchVaultAssetAddresses(
                vaultAccountId = vaultApp.vaultAccountId,
                vaultAssetId = vaultAssetId,
            ).mapNotNull {
                nullable {
                    VaultAssetAddress(
                        assetId = vaultAssetId,
                        address = it.address.bind(),
                        addressFormat = it.addressFormat,
                        legacyAddress = it.address,
                        tag = it.tag
                    )
                }
            }
        }
    }

    suspend fun UserId.getTransactions(
        dateRange: Pair<Instant, Instant>,
        zoneId: ZoneId,
    ): List<Transaction> {
        val vaultApp = getVaultApp()
        val vaultAccountId = vaultApp.vaultAccountId
        return logWithMDC("vaultAccountId" to vaultApp.vaultAccountId) {
            val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(
                vaultAccountId = vaultAccountId,
            )

            if (vaultAccount == null) {
                logger.warn(
                    NotifySlack.ALERTS,
                    "No vault account found in Fireblocks"
                )
                return@logWithMDC emptyList()
            }

            return@logWithMDC getTransactions(
                vaultAccountId = vaultAccountId,
                dateRange = dateRange,
                zoneId = zoneId
            )
        }
    }

    suspend fun getTransactions(
        vaultAccountId: String,
        dateRange: Pair<Instant, Instant>,
        zoneId: ZoneId
    ): List<Transaction> {
        val (after, before) = dateRange
        return FireblocksTransactionService.fetchTransactions(
            vaultAccountId = vaultAccountId,
            after = after,
            before = before,
        ).map { transaction ->
            val createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(transaction.createdAt!!), zoneId)
            val fee = transaction.feeInfo?.let {
                (it.serviceFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) +
                        (it.networkFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO) +
                        (it.gasPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            } ?: BigDecimal.ZERO

            // this will be overwritten by transaction.destinations.* if transaction.destinations != null
            var amount = transaction.amountInfo?.amount ?: ""
            var amountUSD = transaction.amountInfo?.amountUSD ?: ""

            fun TxnSrcDest?.isVaultAccountWith(id: String): Boolean = this?.type == "VAULT_ACCOUNT" && this.id == id

            // needed only if transaction source and destination are not the vault account
            val destinations by lazy {
                transaction.destinations?.find {
                    it.destination?.type == "VAULT_ACCOUNT" && it.destination?.id == vaultAccountId
                }
            }

            val direction = when {
                transaction.source.isVaultAccountWith(id = vaultAccountId) -> "DEBIT"
                transaction.destination.isVaultAccountWith(id = vaultAccountId) -> "CREDIT"
                destinations != null -> {
                    amount = destinations?.amount ?: ""
                    amountUSD = destinations?.amountUSD ?: ""
                    "CREDIT"
                }

                else -> ""
            }

            Transaction(
                id = transaction.id ?: "",
                createdAt = createdAt.toString(),
                operation = transaction.operation ?: "",
                direction = direction,
                assetId = transaction.assetId ?: "",
                amount = amount,
                netAmount = transaction.amountInfo?.netAmount ?: "",
                amountUSD = amountUSD,
                feeCurrency = transaction.feeCurrency ?: "",
                fee = fee.toPlainString(),
            )
        }
    }

    suspend fun generateVaultAccountBalanceReports(
        date: LocalDate,
        mode: Mode,
        email: String? = null, // if null, then execute for all users in Stripe
    ) {
        val (userIdToDetailsMap, userIdToVaultAssetBalanceListMap) = coroutineScope {
            val customerDetailsList = if (email != null) {
                StripeService.getCustomerDetails(email)
            } else {
                StripeService.getAllCustomerDetails()
            }
            val userIdToDetailsMap = customerDetailsList
                .mapNotNull { customerDetails ->
                    val userIdString = FirebaseAuthService.findUserIdOrNull(customerDetails.email)
                    if (userIdString != null) {
                        UserId(userIdString) to customerDetails
                    } else {
                        logger.warn("User (email: ${customerDetails.email}) not registered to K33 Platform")
                        null
                    }
                }
                .toMap()
            val userIdToVaultAccountIdMap = userIdToDetailsMap
                .mapValues { (_, customerDetails) -> customerDetails.email }
                .mapNotNull { (userId, email) ->
                    val vaultApp = firestoreClient.get(userId.inVaultAppContext())
                    if (vaultApp == null) {
                        logger.warn("User (email: $email) not registered to K33 Vault App")
                        null
                    } else {
                        userId to vaultApp.vaultAccountId
                    }
                }
                .toMap()
            val userIdToVaultAssetBalanceListMap = userIdToVaultAccountIdMap
                .map { (userId, vaultAccountId) ->
                    async {
                        logWithMDC("userId" to userId.value, "vaultAccountId" to vaultAccountId) {
                            if (mode.fetch) {
                                // fetch
                                userId to fetchFireblocksVaultAssets(vaultAccountId).also {
                                    if (mode.store) {
                                        // store
                                        userId.storeVaultAssetBalanceList(it, date)
                                    }
                                }
                            } else {
                                // mode.load == true
                                userId to userId.fetchVaultAssetBalanceList(date)
                            }
                        }
                    }
                }
                .awaitAll()
                .toMap()
            userIdToDetailsMap to userIdToVaultAssetBalanceListMap
        }
        coroutineScope {
            val assetIdList = userIdToVaultAssetBalanceListMap
                .values
                .flatMap { it.map(VaultAssetBalance::asset) }
                .distinct()
            val assetIdToRateMap = if (mode.useCurrentFxRate) {
                CoinGeckoClient
                    .getFxRates(
                        baseCurrency = "nok",
                        currencyList = assetIdList,
                    )
                    .mapValues { (_, fxRate) -> fxRate.rate }
            } else {
                assetIdList.map { assetId ->
                    async {
                        assetId to CoinGeckoClient.getHistoricalFxRates(
                            cryptoCurrency = assetId,
                            date = date.plusDays(1),
                            fiatCurrency = "nok",
                        )
                    }
                }.awaitAll()
                    .toMap()
            }
            userIdToVaultAssetBalanceListMap
                .mapValues { (_, vaultAssetBalanceList) ->
                    vaultAssetBalanceList
                        .mapNotNull { vaultAssetBalance ->
                            nullable {
                                val rate = assetIdToRateMap[vaultAssetBalance.asset].bind()
                                val total = (vaultAssetBalance.available?.toDoubleOrNull() ?: 0.0) +
                                        (vaultAssetBalance.pending?.toDoubleOrNull() ?: 0.0) +
                                        (vaultAssetBalance.staked?.toDoubleOrNull() ?: 0.0)
                                VaultAsset(
                                    id = vaultAssetBalance.asset,
                                    available = vaultAssetBalance.available?.toDoubleOrNull(),
                                    pending = vaultAssetBalance.pending?.toDoubleOrNull(),
                                    staked = vaultAssetBalance.staked?.toDoubleOrNull(),
                                    total = total,
                                    rate = Amount(rate, "NOK"),
                                    fiatValue = Amount((rate * total), "NOK"),
                                    dailyPercentChange = null
                                )
                            }
                        }
                }
                .map { (userId, vaultAssets) ->
                    logWithMDC("userId" to userId.value) {
                        val validationErrors = userIdToDetailsMap[userId]
                            ?.address
                            ?.validationErrors()
                            ?: listOf("address")
                        if (validationErrors.isNotEmpty()) {
                            logger.warn(
                                NotifySlack.ALERTS,
                                "Missing stripe address field(s) for user: {}, field(s): {}",
                                userId,
                                validationErrors
                            )
                        }
                        val reportFileContents = getBalanceReport(
                            name = userIdToDetailsMap[userId]?.name ?: "",
                            address = userIdToDetailsMap[userId]?.address?.let { address ->
                                listOfNotNull(
                                    address.line1,
                                    address.line2,
                                    "${address.postalCode?.plus(" ") ?: ""}${address.city ?: ""}",
                                    address.state,
                                    nullable { Locale.of("", address.country.bind()).displayName }
                                )
                            } ?: emptyList(),
                            date = date,
                            vaultAssets = vaultAssets,
                        )
                        saveBalanceReport(
                            userId = userId,
                            fileName = "${date}_v2.pdf",
                            contents = reportFileContents,
                        )
                    }
                }
        }
    }


    private suspend fun fetchFireblocksVaultAssets(
        vaultAccountId: String,
    ): List<VaultAssetBalance> = coroutineScope {
        val vaultAccount = FireblocksVaultAccountService.fetchVaultAccountById(
            vaultAccountId = vaultAccountId,
        )
        when {
            vaultAccount == null -> {
                logger.warn("No vault account (id: $vaultAccountId) found in Fireblocks")
                emptyList()
            }

            vaultAccount.assets.isNullOrEmpty() -> {
                logger.warn("No assets found in vault account (id: $vaultAccountId) in Fireblocks")
                emptyList()
            }

            else -> {
                val stakingAssetsMap = VaultStakingService
                    .getStakingPositionAssets(vaultAccountId = vaultAccountId)
                    .associateBy { it.id }

                vaultAccount
                    .assets
                    ?.mapNotNull { fireblocksVaultAsset ->
                        nullable {
                            val assetId = fireblocksVaultAsset.id.bind()
                            VaultAssetBalance(
                                asset = assetId,
                                total = fireblocksVaultAsset.total,
                                available = fireblocksVaultAsset.available,
                                pending = stakingAssetsMap[assetId]?.pending?.toPlainString(),
                                frozen = fireblocksVaultAsset.frozen,
                                lockedAmount = fireblocksVaultAsset.lockedAmount,
                                staked = stakingAssetsMap[assetId]?.staked?.toPlainString(),
                            )
                        }
                    }
                    ?: emptyList()
            }
        }
    }

    private suspend fun saveBalanceReport(
        userId: UserId,
        fileName: String,
        contents: ByteArray,
    ) {
        FileStoreService.upload(
            bucketConfigId = "firebase",
            filePath = "users/$userId/balanceReports/$fileName",
            contents = contents,
        )
    }

    private suspend fun UserId.fetchVaultAssetBalanceList(
        date: LocalDate,
    ) = firestoreClient.getAll(
        inVaultAppContext() / balanceRecords / date.toString() / assets
    )

    private suspend fun UserId.storeVaultAssetBalanceList(
        vaultAssetBalanceList: List<VaultAssetBalance>,
        date: LocalDate,
    ) = coroutineScope {
        vaultAssetBalanceList
            .map { vaultAssetBalance ->
                async {
                    firestoreClient.put(
                        inVaultAppContext() / balanceRecords / date.toString() / assets / vaultAssetBalance.asset,
                        vaultAssetBalance
                    )
                }
            }
            .awaitAll()
    }
}

fun main() {
    runBlocking {
        VaultService.generateVaultAccountBalanceReports(
            date = LocalDate.of(2024, 12, 31),
            mode = Mode.LOAD,
            email = ""
        )
    }
}