package com.k33.platform.app.vault.currencybeacon

import arrow.core.mapNotNull
import arrow.core.raise.nullable
import com.k33.platform.utils.config.loadConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

private object CurrencyBeaconClient {

    private val httpClient by lazy {
        val config: Config by loadConfig("vault", "currencyBeacon")
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
            }
            install(UserAgent) {
                agent = "k33-backend"
            }
            defaultRequest {
                url(scheme = "https", "api.currencybeacon.com") {
                    parameters.append("api_key", config.apiKey)
                    encodedPath = "/v1/$encodedPath"
                }
            }
        }
    }

    suspend fun getFxRate(
        baseCurrency: String,
        currencyList: List<String>,
    ): Map<String, Double> {
        val symbols = currencyList.joinToString(",") {
            it.toMainNetCurrency()
        }
        return httpClient.get("latest") {
            url {
                parameters.append("base", baseCurrency)
                parameters.append("symbols", symbols)
            }
        }.body<CurrencyRates>()
            .rates
            .mapNotNull { (_, value) ->
                nullable {
                    try {
                        1.0 / value.bind()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun getYesterdayFxRate(
        baseCurrency: String,
        currencyList: List<String>,
    ): Map<String, Double> {
        val symbols = currencyList.joinToString(",") {
            it.toMainNetCurrency()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
        return httpClient.get("historical") {
            url {
                parameters.append("base", baseCurrency)
                parameters.append("date", formatter.format(Instant.now().minus(24.hours.toJavaDuration())))
                parameters.append("symbols", symbols)
            }
        }.body<CurrencyRates>()
            .rates
            .mapNotNull { (_, value) ->
                nullable {
                    try {
                        1.0 / value.bind()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
}

@Serializable
data class CurrencyRates(
    val rates: Map<String, Double?>
)

private fun String.toMainNetCurrency() = substringBefore('_').removeSuffix("TEST")
