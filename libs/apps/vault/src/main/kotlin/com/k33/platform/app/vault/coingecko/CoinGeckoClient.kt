package com.k33.platform.app.vault.coingecko

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
import io.ktor.client.request.header
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CoinGeckoClient {
    private val config: Config by loadConfig("vault", "coinGecko")

    private val httpClient by lazy {
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
                level = LogLevel.INFO
                sanitizeHeader { it.lowercase() == "x-cg-pro-api-key" }
            }
            install(UserAgent) {
                agent = "k33-backend"
            }
            defaultRequest {
                url(scheme = "https", "pro-api.coingecko.com") {
                    encodedPath = "/api/v3/$encodedPath"
                }
                header("x-cg-pro-api-key", config.apiKey)
            }
        }
    }

    suspend fun getFxRates(
        baseCurrency: String,
        currencyList: List<String>,
    ): Map<String, FxRate> {
        val idToSymbolsMap = mutableMapOf<String, MutableSet<String>>()
        val ids = currencyList
            .joinToString(separator = ",") { symbol ->
                val id = config.symbolToIdMap[symbol.toMainNetCurrency()]
                    ?: symbol.toMainNetCurrency()
                    ?: symbol
                idToSymbolsMap.getOrPut(id) { mutableSetOf() }.add(symbol)
                id
            }
        return httpClient.get("simple/price") {
            url {
                parameters.append("ids", ids)
                parameters.append("vs_currencies", baseCurrency)
                parameters.append("include_24hr_change", "true")
            }
        }.body<Map<String, Map<String, Double>>>()
            .entries
            .flatMap { (coinId, map) ->
                idToSymbolsMap.getOrDefault(coinId, emptySet())
                    .map { symbol ->
                        symbol to FxRate(
                            rate = map[baseCurrency.lowercase()],
                            percentChangeIn24hr = map["${baseCurrency.lowercase()}_24h_change"],
                        )
                    }
            }
            .toMap()
    }

    internal suspend fun getCoinList(): List<Coin> {
        return httpClient.get("coins/list").body<List<Coin>>()
    }
}

data class FxRate(
    val rate: Double?,
    val percentChangeIn24hr: Double?,
)

@Serializable
data class Coin(
    val id: String,
    val symbol: String,
)

fun main() {
    runBlocking {
//        FireblocksService
//            .fetchAllSupportedAssets()
//            .forEach {
//                println("${it.id}, ${it.name}, ${it.type}")
//            }
        val symbolRegex = Regex("^[a-z][a-z0-9]+$")
        val idRegex = Regex("^[a-z][a-z0-9-]+$")
        val symbolToCoinMap = mutableMapOf<String, String>()
        CoinGeckoClient
            .getCoinList()
            .filter { coin ->
                symbolRegex.matches(coin.symbol) && idRegex.matches(coin.id)
            }
            .forEach { coin ->
                val value = symbolToCoinMap[coin.symbol]
                if (value == null || value.length > coin.id.length) {
                    symbolToCoinMap[coin.symbol] = coin.id
                }
            }
        symbolToCoinMap
            .entries
            .filter { (symbol, id) -> symbol != id }
            .sortedBy { it.key }
            .forEach { (symbol, id) ->
                println("${symbol}: $id")
            }
    }
}

internal fun String.toMainNetCurrency(): String? {
    return this
        .substringBefore("_TEST")
        .substringBefore("TEST")
        .substringBefore("_POLYGON")
}