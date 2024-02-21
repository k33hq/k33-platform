package com.k33.platform.tests

import com.k33.platform.identity.auth.AppleIdTokenPayload
import com.k33.platform.identity.auth.FirebaseIdTokenPayload
import io.kotest.common.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.UUID

private val jsonPrinter = Json {
    prettyPrint = true
    encodeDefaults = true
}

private val oauthProviderEmulatorClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    defaultRequest {
        host = oauth2ProviderEmulator.host
        port = oauth2ProviderEmulator.port
    }
}

fun HeadersBuilder.appendEndpointsApiUserInfoHeader(
    subject: String = UUID.randomUUID().toString(),
    email: String = "test@k33.com",
    useEsp: Boolean = true,
) {
    val firebaseIdTokenPayload = FirebaseIdTokenPayload(
        subject = subject,
        email = email,
    )
    if (useEsp && usingEsp) {
        val idToken: String = runBlocking {
            oauthProviderEmulatorClient.get {
                url(path = "firebase-id-token")
                contentType(ContentType.Application.Json)
                setBody(firebaseIdTokenPayload)
            }.bodyAsText()
        }
        append("Authorization", "Bearer $idToken")
    } else {
        append("X-Endpoint-API-UserInfo",
            jsonPrinter.encodeToString(firebaseIdTokenPayload)
                .toByteArray()
                .let(Base64.getEncoder()::encodeToString)
        )
    }
}

fun HeadersBuilder.appendAppleIdToken(subject: String) {
    val appleIdTokenPayload = AppleIdTokenPayload(subject = subject)
    if (usingEsp) {
        val idToken: String = runBlocking {
            oauthProviderEmulatorClient.get {
                url(path = "apple-id-token")
                contentType(ContentType.Application.Json)
                setBody(appleIdTokenPayload)
            }.bodyAsText()
        }
        append("Authorization", "Bearer $idToken")
    } else {
        append("X-Endpoint-API-UserInfo",
            jsonPrinter.encodeToString(appleIdTokenPayload)
                .toByteArray()
                .let(Base64.getEncoder()::encodeToString)
        )
    }
}