package com.k33.platform.identity.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.UUID


fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    val rsaKey = RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .generate()

    val signer = RSASSASigner(rsaKey)

    val jwtHeader = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build()

    routing {

        get ("auth/keys") {
            call.respondText("""{ "keys": [ ${rsaKey.toPublicJWK()} ] }""", ContentType.Application.Json)
        }

        get("firebase-id-token") {
            val firebaseIdTokenPayload = call.receive<FirebaseIdTokenPayload>()

            val claims = JWTClaimsSet.Builder()
                .claim("name", firebaseIdTokenPayload.name)
                .claim("picture", firebaseIdTokenPayload.picture)
                .issuer(firebaseIdTokenPayload.issuer)
                .audience(firebaseIdTokenPayload.audience)
                .claim("user_id", firebaseIdTokenPayload.subject)
                .subject(firebaseIdTokenPayload.subject)
                .claim("email", firebaseIdTokenPayload.email)
                .claim("email_verified", firebaseIdTokenPayload.emailVerified)
                .build()

            val jwt = SignedJWT(jwtHeader, claims)
            jwt.sign(signer)

            call.respondText(jwt.serialize())
        }

        get("apple-id-token") {
            val appleIdTokenPayload = call.receive<AppleIdTokenPayload>()

            val claims = JWTClaimsSet.Builder()
                .issuer(appleIdTokenPayload.issuer)
                .subject(appleIdTokenPayload.subject)
                .audience(appleIdTokenPayload.audience)
                .claim("name", appleIdTokenPayload.name)
                .claim("email", appleIdTokenPayload.email)
                .claim("email_verified", appleIdTokenPayload.emailVerified)
                .claim("is_private_email", appleIdTokenPayload.isPrivateEmail)
                .claim("real_user_status", appleIdTokenPayload.realUserStatus)
                .build()

            val jwt = SignedJWT(jwtHeader, claims)
            jwt.sign(signer)

            call.respondText(jwt.serialize())
        }
    }
}