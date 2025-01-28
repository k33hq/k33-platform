package com.k33.platform.identity.auth.apple

import com.k33.platform.identity.auth.gcp.FirebaseAuthService
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.Credential
import io.ktor.server.auth.OAuthKey
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

private const val APPLE_OAUTH2 = "apple-oauth2"

object GcpHttpHeaders {
    const val UserInfo = "X-Endpoint-API-UserInfo"
}

class GcpEndpointsAuthProvider internal constructor(
    configuration: Config
) : AuthenticationProvider(configuration) {

    /**
     * GCP Endpoints Header Auth configuration
     */
    class Configuration internal constructor(name: String?) : Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        when (val espV2Header = context.call.request.espV2Header()) {
            null -> context.challenge(
                OAuthKey,
                AuthenticationFailedCause.NoCredentials,
            ) { challenge, call ->
                call.respond(UnauthorizedResponse())
                challenge.complete()
            }
            else -> context.principal(
                UserIdPrincipal(espV2Header.email)
            )
        }
    }
}

fun AuthenticationConfig.appleJwtAuthConfig() {
    register(GcpEndpointsAuthProvider(GcpEndpointsAuthProvider.Configuration(APPLE_OAUTH2)))
}

@Serializable
data class EspV2Header(
    val email: String,
) : Credential

fun ApplicationRequest.espV2Header(): EspV2Header? {
    val userInfo = header(GcpHttpHeaders.UserInfo) ?: return null
    val userInfoJson = String(Base64.getUrlDecoder().decode(userInfo))
    val jsonFormat = Json {
        ignoreUnknownKeys = true
    }
    return jsonFormat.decodeFromString<EspV2Header>(userInfoJson)
}

fun Application.module() {

    routing {
        authenticate(APPLE_OAUTH2) {
            get("/firebase-custom-token") {
                val email = call.principal<UserIdPrincipal>()!!.name
                val uid = FirebaseAuthService.createOrMergeUser(
                    email = email,
                    displayName = "",
                )
                logWithMDC("userId" to uid) {
                    val firebaseCustomToken = FirebaseAuthService.createCustomToken(uid = uid, email = email)
                    call.respondText(firebaseCustomToken)
                }
            }
        }
    }
}