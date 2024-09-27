package com.k33.platform.tests

import com.k33.platform.utils.logging.prettyPrint
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import java.time.LocalDate
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

    suspend fun getTransactions(
        userId: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "apps/vault/transactions") {
                parameters.append("afterDate", LocalDate.now().minusDays(7).toString())
                parameters.append("beforeDate", LocalDate.now().toString())
            }
            accept(ContentType.Application.Json)
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
            then("Status should be 403 FORBIDDEN") {
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        `when`("GET apps/vault/transactions") {
            val response = getTransactions(userId)
            then("Status should be 403 FORBIDDEN") {
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        `when`("GET apps/vault/settings") {
            val response = getVaultAppSettings(userId)
            then("Status should be 403 FORBIDDEN") {
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        `when`("PUT apps/vault/settings") {
            val response = updateVaultAppSettings(userId, "NOK")
            then("Status should be 404 FORBIDDEN") {
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        `when`("PUT apps/vault/register") {
            val registerResponse = registerVaultApp(userId, vaultAccountId = "238", currency = "USD")
            then("Status should be 200 OK") {
                registerResponse.status shouldBe HttpStatusCode.OK
                registerResponse.body<VaultApp>() shouldBe VaultApp(vaultAccountId = "238", currency = "USD")
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
                val response = getVaultAssetAddresses(userId, "SOL_TEST")
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                    val vaultAssetAddresses = response.body<List<VaultAssetAddress>>()
                    vaultAssetAddresses.size shouldBeGreaterThan 0
                    for (vaultAssetAddress in vaultAssetAddresses) {
                        println("${vaultAssetAddress.assetId} : @ ${vaultAssetAddress.address}, old @ ${vaultAssetAddress.address}, @ fmt ${vaultAssetAddress.addressFormat}, # ${vaultAssetAddress.tag}")
                    }
                }
            }
            and("GET apps/vault/transactions") {
                val response = getTransactions(userId)
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                    val transactions = response.body<List<Transaction>>()
                    for (transaction in transactions) {
                        println(transaction.prettyPrint())
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

    suspend fun generateVaultAccountsBalanceReports(): HttpResponse {
        return adminApiClient.put {
            url(path = "/admin/jobs/generate-vault-accounts-balance-reports") {
                parameters.append("mode", "FETCH")
            }
        }
    }

    xgiven("User is registered in Stripe") {
        val userId = ""
        and("User is register in Vault app") {
            registerVaultApp(
                userId = userId,
                vaultAccountId = "238",
                currency = "NOK",
            ).status shouldBe HttpStatusCode.OK
            `when`("PUT /admin/jobs/generate-vault-accounts-balance-reports") {
                val response = generateVaultAccountsBalanceReports()
                then("Status should be 200 OK") {
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    }

    suspend fun getVaultUserStatus(): HttpResponse {
        return adminApiClient.get {
            url(path = "/apps/vault/admin/user") {
                parameters.append("email", "test@k33.com")
            }
        }
    }

    suspend fun registerVaultUser(vaultAccountId: String): HttpResponse {
        return adminApiClient.put {
            url(path = "/apps/vault/admin/user") {
                parameters.append("email", "test@k33.com")
                parameters.append("vaultAccountId", vaultAccountId)
                parameters.append("currency", "NOK")
            }
        }
    }

    given("For vault admin, user is not registered") {
        `when`("GET /apps/vault/admin/user") {
            val response = getVaultUserStatus()
            then("Status should be 404 Not Found") {
                response.status shouldBe HttpStatusCode.NotFound
                response.body<VaultUserStatus>() shouldBe VaultUserStatus(
                    platformRegistered = true,
                    vaultAccountId = null,
                    currency = null,
                    stripeErrors = emptyList(),
                )
            }
        }
        `when`("Register vault user - PUT /apps/vault/admin/user") {
            val response = registerVaultUser(vaultAccountId = "238")
            then("Status should be 200") {
                response.status shouldBe HttpStatusCode.OK
                response.body<VaultUserStatus>() shouldBe VaultUserStatus(
                    platformRegistered = true,
                    vaultAccountId = "238",
                    currency = "NOK",
                    stripeErrors = emptyList(),
                )
            }
            and("GET /apps/vault/admin/user") {
                val updatedResponse = getVaultUserStatus()
                then("Status should be 200") {
                    updatedResponse.status shouldBe HttpStatusCode.OK
                    response.body<VaultUserStatus>() shouldBe VaultUserStatus(
                        platformRegistered = true,
                        vaultAccountId = "238",
                        currency = "NOK",
                        stripeErrors = emptyList(),
                    )
                }
            }
        }
    }
})

@Serializable
data class VaultAsset(
    val id: String,
    val available: Double?,
    val pending: Double?,
    val staked: Double?,
    val total: Double,
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
data class Transaction(
    val id: String,
    val createdAt: String,
    val operation: String,
    val direction: String,
    val assetId: String,
    val amount: String,
    val netAmount: String,
    val amountUSD: String,
    val feeCurrency: String,
    val fee: String,
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

@Serializable
data class VaultUserStatus(
    val platformRegistered: Boolean,
    val vaultAccountId: String?,
    val currency: String?,
    val stripeErrors: List<String>,
)