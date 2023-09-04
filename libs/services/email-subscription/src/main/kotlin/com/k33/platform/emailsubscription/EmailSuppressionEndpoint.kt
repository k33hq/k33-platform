package com.k33.platform.emailsubscription

import com.k33.platform.email.getEmailService
import com.k33.platform.identity.auth.gcp.UserInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.module() {

    val emailService by getEmailService()

    routing {
        authenticate("esp-v2-header") {
            route("/suppression-groups") {
                get {
                    val userEmail = call.principal<UserInfo>()!!.email
                    val suppressionGroupList = emailService.getSuppressionGroups(
                        userEmail = userEmail,
                    )
                    if (suppressionGroupList == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(
                            suppressionGroupList.map {
                                SuppressionGroup(
                                    id = it.id,
                                    name = it.name,
                                    suppressed = it.suppressed,
                                )
                            }
                        )
                    }
                }
                route("/{suppressionGroupId}") {
                    put {
                        val userEmail = call.principal<UserInfo>()!!.email
                        val suppressionGroupIdString: String = call.parameters["suppressionGroupId"]
                            ?: throw BadRequestException("Path param: suppressionGroupId is mandatory")
                        val suppressionGroupId = suppressionGroupIdString.toLongOrNull()
                            ?: throw BadRequestException("Invalid path param: subscriptionGroupId")
                        val success = emailService.upsertIntoSuppressionGroup(
                            userEmail = userEmail,
                            suppressionGroupId = suppressionGroupId,
                        )
                        when (success) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            true -> call.respond(HttpStatusCode.NoContent)
                            false -> call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    delete {
                        val userEmail = call.principal<UserInfo>()!!.email
                        val suppressionGroupIdString: String = call.parameters["suppressionGroupId"]
                            ?: throw BadRequestException("Path param: suppressionGroupId is mandatory")
                        val suppressionGroupId = suppressionGroupIdString.toLongOrNull()
                            ?: throw BadRequestException("Invalid path param: subscriptionGroupId")
                        val success = emailService.removeFromSuppressionGroup(
                            userEmail = userEmail,
                            suppressionGroupId = suppressionGroupId,
                        )
                        when (success) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            true -> call.respond(HttpStatusCode.NoContent)
                            false -> call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class SuppressionGroup(
    val id: Long,
    val name: String,
    val suppressed: Boolean,
)