@file:Suppress("ClassName")

package com.k33.platform.cms.config

@Suppress("EnumEntryName")
enum class Sync(
    val config: SyncConfig,
) {
    researchArticles(
        SyncConfig(
            ContentfulSpace.research,
            AlgoliaIndexName.articles,
        )
    ),
}

data class SyncConfig(
    val contentfulSpace: ContentfulSpace,
    val algoliaIndexName: AlgoliaIndexName,
)

@Suppress("EnumEntryName")
enum class AlgoliaIndexName {
    articles,
}

//
// Contentful
//

@Suppress("EnumEntryName")
enum class ContentfulSpace(
    val config: ContentfulSpaceConfig
) {
    research(
        ContentfulSpaceConfig(
            spaceId = System.getenv("CONTENTFUL_RESEARCH_SPACE_ID"),
            token = System.getenv("CONTENTFUL_RESEARCH_SPACE_TOKEN"),
            cmaToken = System.getenv("CONTENTFUL_RESEARCH_SPACE_CMA_TOKEN"),
        )
    ),
}

data class ContentfulSpaceConfig(
    val spaceId: String,
    val token: String,
    val cmaToken: String,
)

//
// Algolia
//

data class AlgoliaConfig(
    val applicationId: String,
    val apiKey: String,
)

val algoliaCconfig: AlgoliaConfig by lazy {
    AlgoliaConfig(
        applicationId = System.getenv("ALGOLIA_APP_ID"),
        apiKey = System.getenv("ALGOLIA_API_KEY"),
    )
}