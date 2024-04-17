package com.k33.platform.fireblocks.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.k33.platform.utils.config.loadConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.serialization.jackson.jackson

object FireblocksClient {

    val httpClient by lazy {
        HttpClient(CIO) {
            install(FireblocksAuthPlugin)
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
            install(UserAgent) {
                agent = "k33-backend"
            }
            defaultRequest {
                url(scheme = "https", host = "api.fireblocks.io") {
                    encodedPath = "/v1/$encodedPath"
                }
            }
        }
    }

    suspend inline fun <reified E> get(
        path: String,
        vararg queryParams: Pair<String, String>
    ): E? {
        val response = httpClient.get(path) {
            url {
                queryParams
                    .forEach { parameters.append(it.first, it.second) }
            }
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body<E>()
            HttpStatusCode.NotFound -> null
            else -> throw Exception(response.body<String>())
        }
    }
}