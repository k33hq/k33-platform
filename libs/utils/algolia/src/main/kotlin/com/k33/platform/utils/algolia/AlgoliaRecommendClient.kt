package com.k33.platform.utils.algolia

import com.algolia.client.api.RecommendClient
import com.algolia.client.model.recommend.GetRecommendationsParams
import com.algolia.client.model.recommend.RecommendHit
import com.algolia.client.model.recommend.RelatedModel
import com.algolia.client.model.recommend.RelatedQuery

class AlgoliaRecommendClient(
    applicationId: Algolia.ApplicationId,
    apiKey: Algolia.ApiKey,
    private val index: Algolia.Index,
) {
    private val client by lazy {
        RecommendClient(
            appId = applicationId.value,
            apiKey = apiKey.value,
        )
    }

    suspend fun getRelated(
        objectID: Algolia.ObjectID,
    ): List<Algolia.ObjectID> {
        return client
            .getRecommendations(
                getRecommendationsParams = GetRecommendationsParams(
                    requests = listOf(
                        RelatedQuery(
                            indexName = index.name,
                            maxRecommendations = 3,
                            threshold = 40.0,
                            model = RelatedModel.RelatedProducts,
                            objectID = objectID.value,
                        )
                    )
                )
            )
            .results
            .flatMap { recommendationsResult ->
                recommendationsResult.hits.map { hit ->
                    Algolia.ObjectID((hit as RecommendHit).objectID)
                }
            }
    }

    companion object {
        fun getInstance(
            index: Algolia.Index,
        ): AlgoliaRecommendClient {
            return AlgoliaRecommendClient(
                applicationId = algoliaConfig.applicationId,
                apiKey = algoliaConfig.apiKey,
                index = index,
            )
        }
    }
}