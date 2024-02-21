package com.k33.platform.tests

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.util.UUID

class VaultAppTest : BehaviorSpec({

    suspend fun getVaultAssets(
        userId: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "apps/vault/assets")
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
        }
    }

    suspend fun getVaultAssetAddresses(
        userId: String,
        assetId: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "apps/vault/assets/$assetId/addresses")
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
        }
    }

    suspend fun getVaultAppSettings(
        userId: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "apps/vault/settings")
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
        }
    }

    suspend fun updateVaultAppSettings(
        userId: String,
        currency: String,
    ): HttpResponse {
        return apiClient.put {
            url(path = "apps/vault/settings")
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
            contentType(ContentType.Application.Json)
            setBody(VaultAppSettings(currency = currency))
        }
    }

    suspend fun registerVaultApp(
        userId: String,
        vaultAccountId: String,
        currency: String,
    ): HttpResponse {
        return adminApiClient.put {
            url(path = "apps/vault/register")
            headers {
                appendEndpointsApiUserInfoHeader(subject = userId, useEsp = false)
            }
            contentType(ContentType.Application.Json)
            setBody(
                VaultApp(
                    vaultAccountId = vaultAccountId,
                    currency = currency,
                )
            )
        }
    }

    given("User is not registered") {
        val userId = UUID.randomUUID().toString()
        `when`("GET apps/vault/assets") {
            val response = getVaultAssets(userId)
            then("Status should be 404 NOT FOUND") {
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
        `when`("GET apps/vault/settings") {
            val response = getVaultAppSettings(userId)
            then("Status should be 404 NOT FOUND") {
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
        `when`("PUT apps/vault/settings") {
            val response = updateVaultAppSettings(userId, "NOK")
            then("Status should be 404 NOT FOUND") {
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
        `when`("PUT apps/vault/register") {
            val registerResponse = registerVaultApp(userId, vaultAccountId = "76", currency = "USD")
            then("Status should be 200 OK") {
                registerResponse.status shouldBe HttpStatusCode.OK
                registerResponse.body<VaultApp>() shouldBe VaultApp(vaultAccountId = "76", currency = "USD")
            }
            and("GET apps/vault/assets") {
                val response = getVaultAssets(userId)
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                    val vaultAssets = response.body<List<VaultAsset>>()
                    vaultAssets.size shouldBeGreaterThan 0
                    for (vaultAsset in vaultAssets) {
                        println("${vaultAsset.id} : ${vaultAsset.available} @ ${vaultAsset.rate} = ${vaultAsset.fiatValue}")
                    }
                }
            }
            and("GET apps/vault/assets/{assetId}/addresses") {
                val response = getVaultAssetAddresses(userId, "BTC_TEST")
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                    val vaultAssetAddresses = response.body<List<VaultAssetAddress>>()
                    vaultAssetAddresses.size shouldBeGreaterThan 0
                    for (vaultAssetAddress in vaultAssetAddresses) {
                        println("${vaultAssetAddress.assetId} : @ ${vaultAssetAddress.address}, old @ ${vaultAssetAddress.address}, @ fmt ${vaultAssetAddress.addressFormat}, # ${vaultAssetAddress.tag}")
                    }
                }
            }
            and("GET apps/vault/settings") {
                val response = getVaultAppSettings(userId)
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<VaultAppSettings>() shouldBe VaultAppSettings(currency = "USD")
                }
            }
            and("PUT apps/vault/settings") {
                val updateSettingsResponse = updateVaultAppSettings(userId, "NOK")
                then("Status should be 200 OK") {
                    updateSettingsResponse.status shouldBe HttpStatusCode.OK
                    updateSettingsResponse.body<VaultAppSettings>() shouldBe VaultAppSettings(currency = "NOK")
                }
                and("GET apps/vault/settings") {
                    val response = getVaultAppSettings(userId)
                    then("Status should be 200 OK") {
                        response.status shouldBe HttpStatusCode.OK
                        response.body<VaultAppSettings>() shouldBe VaultAppSettings(currency = "NOK")
                    }
                }
            }
        }
    }
})

@Serializable
data class VaultAsset(
    val id: String,
    val available: Double,
    val rate: Amount?,
    val fiatValue: Amount?,
    val dailyPercentChange: Double?,
)

@Serializable
data class Amount(
    val value: Double,
    val currency: String,
) {
    override fun toString(): String = "$currency $value"
}

@Serializable
data class VaultAssetAddress(
    val assetId: String,
    val address: String,
    val addressFormat: String? = null,
    val legacyAddress: String? = null,
    val tag: String? = null,
)

@Serializable
data class VaultAppSettings(
    val currency: String
)

@Serializable
data class VaultApp(
    val vaultAccountId: String,
    val currency: String,
)