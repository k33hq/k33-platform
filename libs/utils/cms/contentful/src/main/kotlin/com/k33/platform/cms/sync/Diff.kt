package com.k33.platform.cms.sync

import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.content.ContentFactory
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.runBlocking

object Diff {

    private val logger by getLogger()

    suspend fun records(sync: Sync) {
        val contentfulCollectionMetadata = ContentFactory.getContent(sync)
        val entryIdMap = contentfulCollectionMetadata
            .fetchIdToModifiedMap()
        logger.info("Found in contentful: ${entryIdMap.size}")

        val algoliaClient = AlgoliaSearchClient.getInstance(sync)

        val indices = algoliaClient.getAllIds()
        logger.info("Found in algolia: ${indices.size}")

        val newInContentful = entryIdMap.keys - indices.keys
        newInContentful.logAsErrorIfNotEmpty("New in contentful: ${newInContentful.size}")

        val onlyInAlgolia = indices.keys - entryIdMap.keys
        onlyInAlgolia.logAsErrorIfNotEmpty("Only in algolia: ${onlyInAlgolia.size}")

        val common = entryIdMap.keys.intersect(indices.keys)
        val updatedInContentful = common.filter { id ->
            entryIdMap[id]!! > indices[id]!!
        }
        updatedInContentful.logAsErrorIfNotEmpty("Updated in contentful: ${updatedInContentful.size}")

        val upsertIds = newInContentful + updatedInContentful
        logger.info("Upsert id list (${upsertIds.size}) = $upsertIds")
        logger.info("Delete id list (${onlyInAlgolia.size}) = $onlyInAlgolia")
    }

    private fun <E> Collection<E>.logAsErrorIfNotEmpty(message: String) {
        if (this.isEmpty()) {
            logger.info(message)
        } else {
            logger.error(message)
        }
    }
}

fun main() {
    runBlocking {
        Diff.records(Sync.researchArticles)
    }
}