package com.k33.platform.fireblocks.client

import com.k33.platform.utils.config.ConfigAsResourceFile
import com.k33.platform.utils.config.loadConfig
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.utils.EmptyContent
import io.ktor.http.content.TextContent
import java.security.MessageDigest
import java.time.Instant
import java.util.Date
import java.util.UUID

private val config: Config by loadConfig("fireblocks", "fireblocks")

private val rsaKey by lazy {
    JWK.parseFromPEMEncodedObjects(
        convertPemSingleLineToMultiline(config.secretKey)
    ).toRSAKey()
}

private val signer by lazy {
    RSASSASigner(rsaKey)
}

@OptIn(ExperimentalStdlibApi::class)
val FireblocksAuthPlugin = createClientPlugin("FireblocksAuthPlugin") {
    onRequest { request, content ->
        // https://developers.fireblocks.com/reference/signing-a-request-jwt-structure#jwt-structure
        val expiry = Date(Instant.now().plusSeconds(55).toEpochMilli())
        val bodyHash = run {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            // println("content: $content of type ${content::class}")
            val sha256 = when (content) {
                is EmptyContent -> messageDigest.digest("".toByteArray())
                is TextContent -> messageDigest.digest(content.text.toByteArray())
                else -> messageDigest.digest("".toByteArray())
            }
            sha256.toHexString(format = HexFormat.Default)
        }
        val uri = "/" + (request.url.buildString()
            .removePrefix("${request.url.protocol.name}://")
            .substringAfter('/'))
        // println("uri: $uri")
        val claims = JWTClaimsSet.Builder()
            .claim("uri", uri)
            .claim("nonce", UUID.randomUUID().toString())
            .issueTime(Date())
            .expirationTime(expiry)
            .subject(config.apiKey)
            .claim("bodyHash", bodyHash)
            .build()

        val jwtHeader = JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .build()
        val jwt = SignedJWT(jwtHeader, claims).apply {
            sign(signer)
        }
        request.headers.append("X-API-Key", config.apiKey)
        request.headers.append("Authorization", "Bearer ${jwt.serialize()}")
    }
}

fun convertPemSingleLineToMultiline(value: String): String {
    val (_, begin, body, end) = value.split("-----")
    return buildString {
        append("-----$begin-----")
        append(body.replace(' ', '\n'))
        append("-----$end-----")
    }
}