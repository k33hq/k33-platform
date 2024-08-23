package com.k33.platform.cms.sync

import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.content.ContentFactory
import com.k33.platform.cms.events.Action
import com.k33.platform.cms.events.EventHub
import com.k33.platform.cms.events.EventPattern
import com.k33.platform.cms.events.EventType
import com.k33.platform.cms.events.Resource
import com.k33.platform.cms.objectID
import com.k33.platform.cms.objectIDString
import com.k33.platform.utils.algolia.Algolia
import com.k33.platform.utils.algolia.AlgoliaSearchClient
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.runBlocking

class ContentfulToAlgolia(
    private val sync: Sync
) {

    private val logger by getLogger()

    private val algoliaClient by lazy {
        AlgoliaSearchClient.getInstance(
            index = sync.config.algoliaIndex,
        )
    }

    private val content by lazy { ContentFactory.getContent(sync) }

    suspend fun upsert(entryId: String) {
        val record = content.fetch(entityId = entryId) ?: return
        logger.info("Exporting record with objectID: ${record.objectIDString} to algolia")
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
            .toList()
        logger.info("Exporting ${records.size} records from contentful to algolia for syncId: ${sync.name}")
        algoliaClient.batchUpsert(records)
    }

    suspend fun delete(objectID: Algolia.ObjectID) {
        logger.warn("Deleting objectID: ${objectID.value} from algolia")
        algoliaClient.delete(objectID = objectID)
    }

    companion object {
        private val logger by getLogger()

        init {
            EventHub.subscribe(
                eventPattern = EventPattern()
            ) { eventType: EventType, entityId: String ->
                val sync = when (eventType.resource) {
                    Resource.article -> Sync.researchArticles
                }
                val entityContentType = eventType.resource.name
                val contentfulToAlgolia = ContentfulToAlgolia(sync)
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
                        contentfulToAlgolia.delete(Algolia.ObjectID(entityId))
                    }
                }
            }
        }
    }
}

fun main() {
    runBlocking {
        ContentfulToAlgolia(Sync.researchArticles).upsertAll()
//        ContentfulToAlgolia(Sync.researchArticles).upsert("")
//        ContentfulToAlgolia(Sync.researchArticles).delete("")
    }
}