package com.k33.platform.cms.sync

import com.algolia.search.helper.toObjectID
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import com.contentful.java.cma.model.CMALink
import com.contentful.java.cma.model.CMAType
import com.k33.platform.cms.ContentfulConfig
import com.k33.platform.cms.clients.ContentfulMgmtClient
import com.k33.platform.cms.content.ContentFactory
import com.k33.platform.cms.content.ContentField
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

class AlgoliaToContentful(
    private val syncId: String
) {

    private val logger by getLogger()

    private val algoliaConfig by loadConfig<AlgoliaConfig>(
        "contentful",
        "contentfulAlgoliaSync.$syncId.algolia"
    )

    private val algoliaClient by lazy {
        with(algoliaConfig) {
            AlgoliaRecommendClient(
                ApplicationID(applicationId),
                APIKey(apiKey),
                IndexName(indexName)
            )
        }
    }

    private val contentfulConfig by loadConfig<ContentfulConfig>(
        "contentful",
        "contentfulAlgoliaSync.$syncId.contentful"
    )

    private val contentfulMgmtClient by lazy {
        with(contentfulConfig) {
            ContentfulMgmtClient(
                spaceId = spaceId,
                token = cmaToken,
            )
        }
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

    suspend fun upsert(entryId: String) {
        val relatedEntries = algoliaClient.getRelated(entryId.toObjectID())
        contentfulMgmtClient.updateField(
            entryId = entryId,
            key = recommendedArticles.fieldId,
            value = recommendedArticles.mapValues(relatedEntries.map(ObjectID::toString))
        )
    }

    private val content by lazy { ContentFactory.getContent(syncId) }

    suspend fun upsertAll() {
        val entryIds = content
            .fetchIds()
        logger.info("Updating ${entryIds.size} articles with recommendedArticles in contentful from algolia for syncId: $syncId")
        entryIds
            .asFlow()
            .map { entryId ->
                    try {
                        upsert(entryId)
                    } catch (e: Exception) {
                        logger.error("Failed to update recommendedArticles for entry: {}", entryId)
                    }
                // setting rate limit of 7 calls / second.
                // we make 3 Contentful CMA calls per update.
                // so setting delay of 500ms ensures 6 CMA req / sec
                delay(500.milliseconds)
            }
            .count()
            .let { count ->
                logger.info("Updated $count of ${entryIds.size} articles with recommendedArticles in contentful from algolia for syncId: $syncId")
            }
    }

    suspend fun delete(entryId: String) {
        contentfulMgmtClient.updateField(
            entryId = entryId,
            key = recommendedArticles.fieldId,
            value = recommendedArticles.mapValues(emptyList())
        )
    }
}

fun main() {
    runBlocking {
//        AlgoliaToContentful("researchArticles").upsertAll()
//        AlgoliaToContentful("researchArticles").upsert("")
//        AlgoliaToContentful("researchArticles").delete("")
    }
}