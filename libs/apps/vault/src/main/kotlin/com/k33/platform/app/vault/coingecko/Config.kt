package com.k33.platform.app.vault.coingecko

data class Config(
    val apiKey: String,
    val symbolToIdMap: Map<String, String> = emptyMap(),
)