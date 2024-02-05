package com.k33.platform.user

import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.user.UserService.createUser
import com.k33.platform.user.UserService.fetchUser
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.module() {

    routing {
        authenticate("esp-v2-header") {
            route("/user") {
                get {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        val user = userId.fetchUser()
                        if (user != null) {
                            call.application.log.info("Found user")
                            call.respond(HttpStatusCode.OK, user)
                        } else {
                            call.application.log.info("User does not exists")
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                post {
                    val userInfo = call.principal<UserInfo>()!!
                    val userId = UserId(userInfo.userId)
                    logWithMDC("userId" to userId.value) {
                        call.application.log.info("Creating user")
                        val webClientId = call.request.header("x-client-id")
                        val idProvider = call.request.queryParameters["id-provider"]
                        val pageUrl = call.request.queryParameters["source-url"]
                        val user = userId.createUser(
                            email = userInfo.email,
                            webClientId = webClientId,
                            idProvider = idProvider,
                            pageUrl = pageUrl,
                        )
                        if (user == null) {
                            call.application.log.error("Failed to create a user")
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(HttpStatusCode.OK, user)
                        }
                    }
                }
            }
        }
    }
}