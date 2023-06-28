package com.k33.platform.cms.sync

import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import com.k33.platform.cms.content.ContentFactory
import com.k33.platform.cms.events.Action
import com.k33.platform.cms.events.EventHub
import com.k33.platform.cms.events.EventPattern
import com.k33.platform.cms.events.EventType
import com.k33.platform.cms.events.Resource
import com.k33.platform.cms.objectID
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.runBlocking

class ContentfulToAlgolia(
    private val syncId: String
) {

    private val logger by getLogger()

    private val algoliaConfig by loadConfig<AlgoliaConfig>(
        "contentful",
        "contentfulAlgoliaSync.$syncId.algolia"
    )

    private val algoliaClient by lazy {
        with(algoliaConfig) {
            AlgoliaSearchClient(
                ApplicationID(applicationId),
                APIKey(apiKey),
                IndexName(indexName)
            )
        }
    }

    private val content by lazy { ContentFactory.getContent(syncId) }

    suspend fun upsert(entryId: String) {
        val record = content.fetch(entityId = entryId) ?: return
        logger.info("Exporting record with objectID: ${record.objectID} to algolia")
        record.objectID?.let { objectId ->
            algoliaClient.upsert(
                objectID = objectId,
                record = record
            )
        }
    }

    suspend fun upsertAll() {
        val records = content
            .fetchAll()
            .mapNotNull { jsonObject ->
                jsonObject.objectID?.let {
                    objectID -> objectID to jsonObject
                }
            }
        logger.info("Exporting ${records.size} records from contentful to algolia for syncId: $syncId")
        algoliaClient.batchUpsert(records)
    }

    suspend fun delete(entryId: String) {
        val objectID = ObjectID(entryId)
        logger.warn("Deleting objectID: $objectID from algolia")
        algoliaClient.delete(objectID)
    }

    companion object {
        private val logger by getLogger()

        init {
            EventHub.subscribe(eventPattern = EventPattern()) { eventType: EventType, entityId: String ->
                val syncId = when (eventType.resource) {
                    Resource.article -> "researchArticles"
                }
                val entityContentType = eventType.resource.name
                val contentfulToAlgolia = ContentfulToAlgolia(syncId)
                when (eventType.action) {
                    Action.publish -> {
                        try {
                            logger.info("Exporting $entityContentType: $entityId from contentful to algolia")
                            contentfulToAlgolia.upsert(entityId)
                        } catch (e: Exception) {
                            logger.error("Exporting $entityContentType: $entityId from contentful to algolia failed", e)
                        }
                    }

                    Action.unpublish -> {
                        logger.warn("Removing $entityContentType: $entityId from algolia")
                        contentfulToAlgolia.delete(entityId)
                    }
                }
            }
        }
    }
}

fun main() {
    runBlocking {
//        ContentfulToAlgolia("researchArticles").upsertAll()
//        ContentfulToAlgolia("researchArticles").upsert("")
//        ContentfulToAlgolia("researchArticles").delete("")
    }
}