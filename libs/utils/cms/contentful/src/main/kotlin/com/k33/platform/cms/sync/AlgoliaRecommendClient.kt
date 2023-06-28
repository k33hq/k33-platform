package com.k33.platform.cms.sync

import com.algolia.search.client.ClientRecommend
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import com.algolia.search.model.recommend.RelatedProductsQuery
import com.k33.platform.cms.objectID
import com.k33.platform.utils.logging.getLogger
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AlgoliaRecommendClient(
    applicationId: ApplicationID,
    apiKey: APIKey,
    private val indexName: IndexName,
) {
    private val logger by getLogger()

    private val client by lazy {
        ClientRecommend(
            applicationId,
            apiKey,
        )
    }

    suspend fun getRelated(
        objectID: ObjectID,
    ): List<ObjectID> {
        val request = RelatedProductsQuery(
            indexName = indexName,
            objectID = objectID,
            maxRecommendations = 3,
        )
        return client
            .getRelatedProducts(listOf(request))
            .flatMap { responseSearch ->
                responseSearch.hits.mapNotNull { hit ->
                    logger.info("title: {}", hit["title"]?.jsonPrimitive?.contentOrNull)
                    logger.info("score: {}", hit.scoreOrNull)
                    hit.json.objectID
                }
            }
    }
}