package com.k33.platform.cms.space.research.articleWeb

import com.k33.platform.cms.ContentfulConfig
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.runBlocking

object ResearchArticleWebSlugPrefix {

    private val logger by getLogger()

    private val contentfulConfig by loadConfig<ContentfulConfig>(
        "contentful",
        "contentfulAlgoliaSync.researchArticles.contentful"
    )

    suspend fun exportWebSlugPrefixSet() {
        val researchArticleWeb = ResearchArticleWeb(
            spaceId = contentfulConfig.spaceId,
            token = contentfulConfig.token,
        )

        researchArticleWeb
            .fetchSlugPrefixList()
            .forEach(::println)
    }
}

fun main() {
    runBlocking {
        ResearchArticleWebSlugPrefix.exportWebSlugPrefixSet()
    }
}