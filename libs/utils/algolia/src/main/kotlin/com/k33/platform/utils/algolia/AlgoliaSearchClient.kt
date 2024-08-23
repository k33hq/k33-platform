package com.k33.platform.utils.algolia

import com.algolia.client.api.SearchClient
import com.algolia.client.extensions.replaceAllObjects
import com.algolia.client.model.search.BrowseParamsObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class AlgoliaSearchClient(
    applicationId: Algolia.ApplicationId,
    apiKey: Algolia.ApiKey,
    private val index: Algolia.Index,
) {

    private val client by lazy {
        SearchClient(
            appId = applicationId.value,
            apiKey = apiKey.value,
        )
    }

    suspend fun upsert(
        objectID: Algolia.ObjectID,
        record: JsonObject,
    ) {
        client.addOrUpdateObject(
            indexName = index.name,
            objectID = objectID.value,
            body = record,
        )
    }

    suspend fun batchUpsert(
        records: List<JsonObject>
    ) {
        client.replaceAllObjects(
            indexName = index.name,
            objects = records,
        )
    }

    suspend fun delete(
        objectID: Algolia.ObjectID,
    ) {
        client.deleteObject(
            indexName = index.name,
            objectID = objectID.value,
        )
    }

    suspend fun getAllIds(): Map<Algolia.ObjectID, String> {
        return client
            .browse(
                indexName = index.name,
                browseParams = BrowseParamsObject(
                    attributesToRetrieve = listOf("publishedAt"),
                )
            )
            .hits
            .associate {
                Algolia.ObjectID(it.objectID) to it.additionalProperties!!["publishedAt"]!!.jsonPrimitive.content
            }
    }

    companion object {
        fun getInstance(
            index: Algolia.Index,
        ): AlgoliaSearchClient {
            return AlgoliaSearchClient(
                applicationId = algoliaConfig.applicationId,
                apiKey = algoliaConfig.apiKey,
                index = index,
            )
        }
    }
}