@file:Suppress("ClassName")

package com.k33.platform.cms.config

import com.k33.platform.utils.algolia.Algolia

@Suppress("EnumEntryName")
enum class Sync(
    val config: SyncConfig,
) {
    researchArticles(
        SyncConfig(
            ContentfulSpace.research,
            Algolia.Index.articles,
        )
    ),
}

data class SyncConfig(
    val contentfulSpace: ContentfulSpace,
    val algoliaIndex: Algolia.Index,
)

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
