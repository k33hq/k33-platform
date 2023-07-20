package com.k33.platform.cms.sync

import com.algolia.search.client.ClientRecommend
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import com.algolia.search.model.recommend.RelatedProductsQuery
import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.config.algoliaCconfig
import com.k33.platform.cms.objectID

class AlgoliaRecommendClient(
    applicationId: ApplicationID,
    apiKey: APIKey,
    private val indexName: IndexName,
) {
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
//        println()
//        print(objectID)
        return client
            .getRelatedProducts(listOf(request))
            .flatMap { responseSearch ->
                responseSearch.hits.mapNotNull { hit ->
//                    logger.info("title: {}", hit["title"]?.jsonPrimitive?.contentOrNull)
//                    logger.info("score: {}", hit.scoreOrNull)
//                    print(",${hit.json.objectID},${hit.scoreOrNull}")
                    hit.json.objectID
                }
            }
    }

    companion object {
        fun getInstance(
            sync: Sync,
        ): AlgoliaRecommendClient {
            return AlgoliaRecommendClient(
                ApplicationID(algoliaCconfig.applicationId),
                APIKey(algoliaCconfig.apiKey),
                IndexName(sync.config.algoliaIndexName.name),
            )
        }
    }
}