package com.k33.platform.app.vault.coingecko

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CoinGeckoClientTest : StringSpec({
    "get fx rates".config(enabled = false) {
        CoinGeckoClient.getFxRates(
            baseCurrency = "USD",
            currencyList = listOf(
                "BTC",
                "ETH",
                "SOL",
                "MATIC_POLYGON",
                "XRP",
                "LINK",
                "SAND",
                "GALA",
                "ETHW",
            )
        ).forEach { (symbol, fxRate) ->
            println("$symbol: USD ${fxRate.rate}, ${fxRate.percentChangeIn24hr}%")
        }
    }

    "map: main -> main" {
        "BTC".toMainNetCurrency() shouldBe "BTC"
    }
    "map: main_TEST -> main" {
        "BTC_TEST".toMainNetCurrency() shouldBe "BTC"
    }
    "map: main_TEST[0-9] -> main" {
        "ETH_TEST3".toMainNetCurrency() shouldBe "ETH"
    }
    "map: mainTEST -> main" {
        "AVAXTEST".toMainNetCurrency() shouldBe "AVAX"
    }
    "map: MATIC_POLYGON_MUMBAI -> MATIC" {
        "MATIC_POLYGON_MUMBAI".toMainNetCurrency() shouldBe "MATIC"
    }
    "map: MATIC_POLYGON -> MATIC" {
        "MATIC_POLYGON".toMainNetCurrency() shouldBe "MATIC"
    }
})