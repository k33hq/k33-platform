package com.k33.platform.utils.algolia

data class AlgoliaConfig(
    val applicationId: Algolia.ApplicationId,
    val apiKey: Algolia.ApiKey,
)

val algoliaConfig: AlgoliaConfig by lazy {
    AlgoliaConfig(
        applicationId = Algolia.ApplicationId(System.getenv("ALGOLIA_APP_ID")),
        apiKey = Algolia.ApiKey(System.getenv("ALGOLIA_API_KEY")),
    )
}