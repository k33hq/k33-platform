package com.k33.platform.cms.sync

import com.contentful.java.cma.model.CMALink
import com.contentful.java.cma.model.CMAType
import com.k33.platform.cms.clients.ContentfulMgmtClient
import com.k33.platform.cms.config.ContentfulSpace
import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.content.ContentFactory
import com.k33.platform.cms.content.ContentField
import com.k33.platform.utils.algolia.Algolia
import com.k33.platform.utils.algolia.AlgoliaRecommendClient
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

class AlgoliaToContentful(
    private val sync: Sync
) {
    private val logger by getLogger()

    private val algoliaClient by lazy {
        AlgoliaRecommendClient.getInstance(
            index = sync.config.algoliaIndex,
        )
    }

    private val contentfulMgmtClient by lazy {
        ContentfulMgmtClient(
            spaceId = ContentfulSpace.research.config.spaceId,
            token = ContentfulSpace.research.config.cmaToken,
        )
    }

    private val recommendedArticles by lazy {
        ContentField(
            fieldId = "recommendedArticles",
            mapValues = { recommendedArticlesEntryIdList ->
                recommendedArticlesEntryIdList.map { recommendedArticlesEntryId ->
                    CMALink(CMAType.Entry).apply {
                        id = recommendedArticlesEntryId
                    }
                }
            }
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun upsert(
        entryId: String,
    ): Boolean {
        val relatedEntries = algoliaClient
            .getRelated(Algolia.ObjectID(entryId))
            .map(Algolia.ObjectID::value)
        return contentfulMgmtClient.updateField(
            entryId = entryId,
            key = recommendedArticles.fieldId,
            value = recommendedArticles.mapValues(relatedEntries)
        )
    }

    private val content by lazy { ContentFactory.getContent(sync) }

    suspend fun upsertAll() {
        val entryIds = content
            .fetchIds()
        logger.info("Updating ${entryIds.size} articles with recommendedArticles in contentful from algolia for syncId: ${sync.name}")
        entryIds
            .asFlow()
            .map { entryId ->
                val result = try {
                    upsert(entryId)
                } catch (e: Exception) {
                    logger.error("Failed to update recommendedArticles for entry: {}", entryId)
                    false
                }
                // setting rate limit of 7 calls / second.
                // we make 3 Contentful CMA calls per update.
                // so setting delay of 500ms ensures 6 CMA req / sec
                delay(500.milliseconds)
                result
            }
            .count { it }
            .let { count ->
                logger.info("Updated $count of ${entryIds.size} articles with recommendedArticles in contentful from algolia for syncId: ${sync.name}")
            }
    }

    suspend fun delete(
        entryId: String
    ): Boolean = contentfulMgmtClient.updateField(
        entryId = entryId,
        key = recommendedArticles.fieldId,
        value = recommendedArticles.mapValues(emptyList())
    )
}

fun main() {
    runBlocking {
        AlgoliaToContentful(Sync.researchArticles).upsertAll()
//        AlgoliaToContentful(Sync.researchArticles).upsert("")
//        AlgoliaToContentful(Sync.researchArticles).delete("")
    }
}