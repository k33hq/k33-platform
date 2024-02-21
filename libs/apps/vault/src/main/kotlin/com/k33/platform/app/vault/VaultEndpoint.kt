package com.k33.platform.app.vault

import com.k33.platform.app.vault.VaultService.getVaultAddresses
import com.k33.platform.app.vault.VaultService.getVaultAppSettings
import com.k33.platform.app.vault.VaultService.getVaultAssets
import com.k33.platform.app.vault.VaultService.register
import com.k33.platform.app.vault.VaultService.updateVaultAppSettings
import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

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