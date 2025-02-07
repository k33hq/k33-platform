package com.k33.platform.app.vault.staking

import com.k33.platform.app.vault.staking.VaultStakingService.claimRewards
import com.k33.platform.app.vault.staking.VaultStakingService.getStakingAssets
import com.k33.platform.app.vault.staking.VaultStakingService.getStakingPosition
import com.k33.platform.app.vault.staking.VaultStakingService.getStakingPositions
import com.k33.platform.app.vault.staking.VaultStakingService.partialUnstake
import com.k33.platform.app.vault.staking.VaultStakingService.stake
import com.k33.platform.app.vault.staking.VaultStakingService.unstake
import com.k33.platform.app.vault.staking.VaultStakingService.withdraw
import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

fun Route.vaultStakingEndpoint() {

    get("assets") {
        val userId = UserId(call.principal<UserInfo>()!!.userId)
        logWithMDC("userId" to userId.value) {
            call.respond(userId.getStakingAssets())
        }
    }

    get("providers") {
        call.respond(VaultStakingService.getStakingProviders())
    }

    route("positions") {
        get {
            val userId = UserId(call.principal<UserInfo>()!!.userId)
            logWithMDC("userId" to userId.value) {
                call.respond(userId.getStakingPositions())
            }
        }

        // stake
        post {
            val userId = UserId(call.principal<UserInfo>()!!.userId)
            logWithMDC("userId" to userId.value) {
                val request = call.receive<StakeRequest>()
                val stakingPosition = userId.stake(
                    vaultAssetId = request.vaultAssetId,
                    amount = request.amount,
                    providerId = request.providerId,
                )
                if (stakingPosition != null) {
                    call.respond(HttpStatusCode.Created, stakingPosition)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        route("{stakingPositionId}") {
            get {
                val userId = UserId(call.principal<UserInfo>()!!.userId)
                logWithMDC("userId" to userId.value) {
                    val stakingPositionId = call.parameters["stakingPositionId"]!!
                    call.respond(userId.getStakingPosition(stakingPositionId))
                }
            }
            put {
                val userId = UserId(call.principal<UserInfo>()!!.userId)
                logWithMDC("userId" to userId.value) {
                    val stakingPositionId = call.parameters["stakingPositionId"]!!
                    val stakingPosition = when (
                        val action = call.request.queryParameters["action"]
                    ) {
                        "unstake" -> userId.unstake(stakingPositionId = stakingPositionId)
                        "partialUnstake" -> {
                            val amount = call.request.queryParameters["amount"]
                                ?: throw BadRequestException("Missing query  parameter: amount")
                            userId.partialUnstake(stakingPositionId = stakingPositionId, amount = amount)
                        }

                        "withdraw" -> userId.withdraw(stakingPositionId = stakingPositionId)
                        "claimRewards" -> userId.claimRewards(stakingPositionId = stakingPositionId)
                        else -> throw BadRequestException("Unrecognized action $action")
                    }
                    if (stakingPosition != null) {
                        call.respond(HttpStatusCode.OK, stakingPosition)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
    }
}

@Serializable
data class VaultStakingAsset(
    val id: String,
    val available: String?,
    val pending: String?,
    val staked: String?,
)

@Serializable
data class StakeRequest(
    val vaultAssetId: String,
    val amount: String,
    val providerId: String,
)
