package com.k33.platform.app.vault

import arrow.core.raise.nullable
import com.k33.platform.app.vault.coingecko.CoinGeckoClient
import com.k33.platform.fireblocks.service.FireblocksService
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import io.firestore4k.typed.FirestoreClient
import io.ktor.server.plugins.NotFoundException

object VaultService {

    private val logger by getLogger()

    private val firestoreClient by lazy { FirestoreClient() }

    suspend fun UserId.getVaultAssets(currency: String?): List<VaultAsset> {
        val vaultApp = firestoreClient.get(inVaultAppContext())
            ?: throw NotFoundException("Not registered to K33 Vault service")

        return getVaultAssets(
            vaultApp = vaultApp,
            currency = currency
        )
    }

    suspend fun UserId.getVaultAddresses(
        vaultAssetId: String
    ): List<VaultAssetAddress> {
        val vaultApp = firestoreClient.get(inVaultAppContext())
            ?: throw NotFoundException("Not registered to K33 Vault service")

        return FireblocksService.fetchVaultAssetAddresses(
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

    internal suspend fun getVaultAssets(
        vaultApp: VaultApp,
        currency: String?,
    ): List<VaultAsset> {
        val vaultAccount = FireblocksService.fetchVaultAccountById(
            vaultAccountId = vaultApp.vaultAccountId,
        )

        if (vaultAccount == null) {
            logger.warn(
                NotifySlack.ALERTS,
                "No vault account (id: ${vaultApp.vaultAccountId}) found in Fireblocks"
            )
            return emptyList()
        }

        val baseCurrency = currency ?: vaultApp.currency

        val vaultAssetMap = vaultAccount
            .assets
            ?.mapNotNull { vaultAsset ->
                nullable {
                    vaultAsset.id.bind() to vaultAsset.available.bind().toDouble()
                }
            }
            ?.toMap()
            ?: emptyMap()

        return if (vaultAssetMap.isNotEmpty()) {
            val fxMap = CoinGeckoClient.getFxRates(
                baseCurrency = baseCurrency,
                currencyList = vaultAssetMap
                    .filter { it.value > 0 }
                    .keys
                    .map {
                        it
                    }
                    .toList()
            )
            vaultAssetMap
                .map { (currency, balance) ->
                    VaultAsset(
                        id = currency,
                        available = balance,
                        rate = nullable {
                            Amount(
                                value = fxMap[currency]?.rate.bind(),
                                currency = baseCurrency,
                            )
                        },
                        fiatValue = nullable {
                            Amount(
                                value = fxMap[currency]?.rate.bind() * balance,
                                currency = baseCurrency,
                            )
                        },
                        dailyPercentChange = fxMap[currency]
                            ?.percentChangeIn24hr,
                    )
                }
                .sortedByDescending { it.fiatValue?.value }
        } else {
            emptyList()
        }
    }

    suspend fun UserId.getVaultAppSettings(): VaultAppSettings {
        val vaultApp = firestoreClient.get(inVaultAppContext())
            ?: throw NotFoundException("Not registered to K33 Vault service")
        return VaultAppSettings(
            currency = vaultApp.currency,
        )
    }

    suspend fun UserId.updateVaultAppSettings(settings: VaultAppSettings) {
        val vaultApp = firestoreClient.get(inVaultAppContext())
            ?: throw NotFoundException("Not registered to K33 Vault service")
        val updatedVaultApp = vaultApp.copy(
            currency = settings.currency,
        )
        firestoreClient.put(inVaultAppContext(), updatedVaultApp)
    }

    suspend fun UserId.register(
        vaultApp: VaultApp,
    ) {
        firestoreClient.put(inVaultAppContext(), vaultApp)
    }
}