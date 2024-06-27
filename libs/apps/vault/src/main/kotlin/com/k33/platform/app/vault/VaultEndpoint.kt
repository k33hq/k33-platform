package com.k33.platform.app.vault

import com.k33.platform.app.vault.VaultService.getTransactions
import com.k33.platform.app.vault.VaultService.getVaultAddresses
import com.k33.platform.app.vault.VaultService.getVaultAppSettings
import com.k33.platform.app.vault.VaultService.getVaultAssets
import com.k33.platform.app.vault.VaultService.register
import com.k33.platform.app.vault.VaultService.updateVaultAppSettings
import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HeaderValueParam
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

fun Application.module() {
    routing {
        authenticate("esp-v2-header") {
            route("/apps/vault") {

                // get status map for all funds
                get("assets") {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        val currency: String? = call.request.queryParameters["currency"]
                        call.respond(userId.getVaultAssets(currency))
                    }
                }

                get("assets/{assetId}/addresses") {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        val assetId = call.parameters["assetId"]!!
                        call.respond(userId.getVaultAddresses(vaultAssetId = assetId))
                    }
                }

                get("transactions") {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        val (afterDate, beforeDate, zoneId) = call.request.queryParameters.getDateRange()
                        val transactions = userId.getTransactions(afterDate to beforeDate, zoneId)
                        call.respondTransactionsAsCsv(transactions)
                    }
                }

                route("settings") {
                    get {
                        val userId = UserId(call.principal<UserInfo>()!!.userId)
                        logWithMDC("userId" to userId.value) {
                            call.respond(userId.getVaultAppSettings())
                        }
                    }
                    put {
                        val userId = UserId(call.principal<UserInfo>()!!.userId)
                        logWithMDC("userId" to userId.value) {
                            val settings = call.receive<VaultAppSettings>()
                            userId.updateVaultAppSettings(settings)
                            call.respond(settings)
                        }
                    }
                }

                // not exposed
                put("register") {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        val vaultApp = call.receive<VaultApp>()
                        userId.register(vaultApp)
                        call.respond(vaultApp)
                    }
                }
            }
        }

        route("/apps/vault/admin/user") {
            get {
                val email = call.request.queryParameters["email"]
                    ?: throw BadRequestException("Missing query parameter: email")
                logWithMDC("vaultUserEmail" to email) {
                    val vaultUserStatus = VaultService.getUserStatus(email = email)
                    val httpStatusCode = if (vaultUserStatus.platformRegistered
                        && vaultUserStatus.vaultAccountId != null
                        && vaultUserStatus.stripeErrors.isEmpty()
                    ) {
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.NotFound
                    }
                    call.respond(httpStatusCode, vaultUserStatus)
                }
            }
            put {
                val email = call.request.queryParameters["email"]
                    ?: throw BadRequestException("Missing query parameter: email")
                val vaultAccountId = call.request.queryParameters["vaultAccountId"]
                    ?: throw BadRequestException("Missing query parameter: vaultAccountId")
                val currency = call.request.queryParameters["currency"] ?: "USD"
                logWithMDC("vaultUserEmail" to email) {
                    val vaultUserStatus = VaultService.register(
                        email = email,
                        vaultAccountId = vaultAccountId,
                        currency = currency,
                    )
                    val httpStatusCode = if (vaultUserStatus.platformRegistered
                        && vaultUserStatus.vaultAccountId != null
                        && vaultUserStatus.stripeErrors.isEmpty()
                    ) {
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.NotFound
                    }
                    call.respond(httpStatusCode, vaultUserStatus)
                }
            }
            delete {
                val email = call.request.queryParameters["email"]
                    ?: throw BadRequestException("Missing query parameter: email")
                logWithMDC("vaultUserEmail" to email) {
                    val vaultUserStatus = VaultService.deregister(email = email)
                    val httpStatusCode = if (vaultUserStatus.platformRegistered) {
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.NotFound
                    }
                    call.respond(httpStatusCode, vaultUserStatus)
                }
            }
        }

        get("/apps/vault/admin/transactions") {
            val vaultAccountId = call.request.queryParameters["vaultAccountId"]
                ?: throw BadRequestException("Missing query parameter: vaultAccountId")
            logWithMDC("vaultAccountId" to vaultAccountId) {
                val (afterDate, beforeDate, zoneId) = call.request.queryParameters.getDateRange()
                val transactions = getTransactions(vaultAccountId, afterDate to beforeDate, zoneId)
                call.respondTransactionsAsCsv(transactions)
            }
        }

        put("/admin/jobs/generate-vault-accounts-balance-reports/{date?}") {
            val mode = call.request.queryParameters.getEnum("mode") ?: Mode.FETCH_AND_STORE
            val date = call.parameters.getLocalDate("date")
                ?: LocalDate.now(ZoneId.of("Europe/Oslo")).minusDays(1)
            VaultService.generateVaultAccountBalanceReports(
                date = date,
                mode = mode,
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}

private inline fun <reified E : Enum<E>> Parameters.getEnum(
    name: String
): E? = get(name)
    ?.let {
        try {
            enumValueOf<E>(it)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid enum value: $it of $name")
        }
    }

private fun Parameters.getLocalDate(
    name: String
): LocalDate? = get(name)
    ?.let {
        try {
            LocalDate.parse(it)
        } catch (e: DateTimeParseException) {
            throw BadRequestException("Invalid local date value: $it of $name")
        }
    }

private fun Parameters.getDateRange(): Triple<Instant, Instant, ZoneId> {
    val zoneId = this["zoneId"]?.let(ZoneId::of) ?: ZoneId.of("Europe/Oslo")
    val afterDate = ZonedDateTime.of(getLocalDate("afterDate"), LocalTime.MIN, zoneId).toInstant()
    val beforeDate = ZonedDateTime.of(getLocalDate("beforeDate"), LocalTime.MAX, zoneId).toInstant()
    return Triple(afterDate, beforeDate, zoneId)
}

private suspend fun ApplicationCall.respondTransactionsAsCsv(
    transactions: List<Transaction>,
) {
    val csvBytes = toCsv(transactions).toByteArray(Charsets.UTF_8)
    this.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameters(
            listOf(
                HeaderValueParam(ContentDisposition.Parameters.FileName, "transactions.csv"),
                HeaderValueParam(ContentDisposition.Parameters.Size, csvBytes.size.toString()),
            )
        ).toString()
    )
    this.respondBytes(
        bytes = csvBytes,
        contentType = ContentType.Text.CSV,
        status = HttpStatusCode.OK,
    )
}

private fun toCsv(
    transactions: List<Transaction>
): String = buildString {
    appendLine("Id,CreatedAt,Operation,Direction,Asset,Amount,NetAmount,AmountUSD,FeeCurrency,Fee")
    transactions.forEach { transaction ->
        with(transaction) {
            appendLine(""""$id","$createdAt","$operation","$direction","$assetId",$amount,$netAmount,$amountUSD,"$feeCurrency",$fee""")
        }
    }
}

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

enum class Mode(
    // fetch from Fireblocks
    val fetch: Boolean,
    // store to DB
    val store: Boolean,
) {
    // running on time to store a snapshot of balances and generate PDFs
    FETCH_AND_STORE(fetch = true, store = true),

    // testing to generate PDFs only
    FETCH(fetch = true, store = false),

    // running later to regenerate PDFs only
    LOAD(fetch = false, store = false);

    // load from DB
    val load: Boolean
        get() = !fetch

    val useCurrentFxRate: Boolean
        get() = store
}

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
data class VaultUserStatus(
    val platformRegistered: Boolean,
    val vaultAccountId: String?,
    val stripeErrors: List<String>,
)