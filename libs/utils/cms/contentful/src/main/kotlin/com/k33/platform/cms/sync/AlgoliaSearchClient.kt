package com.k33.platform.cms.sync

import com.algolia.search.client.ClientSearch
import com.algolia.search.dsl.attributesToRetrieve
import com.algolia.search.dsl.query
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import com.k33.platform.cms.config.Sync
import com.k33.platform.cms.config.algoliaCconfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class AlgoliaSearchClient(
    applicationId: ApplicationID,
    apiKey: APIKey,
    indexName: IndexName,
) {

    private val index by lazy {
        val client = ClientSearch(
            applicationId,
            apiKey,
        )
        client.initIndex(indexName)
    }

    suspend fun upsert(
        objectID: ObjectID,
        record: JsonObject,
    ) {
        index.replaceObject(
            objectID,
            record,
        )
    }

    suspend fun batchUpsert(
        records: List<Pair<ObjectID, JsonObject>>
    ) {
        index.replaceObjects(records)
    }

    suspend fun delete(
        objectID: ObjectID,
    ) {
        index.deleteObject(objectID)
    }

    suspend fun getAllIds(): Map<String, String> {
        val query = query("") {
            attributesToRetrieve {
                +Algolia.Key.ObjectID
                +"publishedAt"
            }
        }
        return index.browseObjects(query)
            .flatMap {
                it.hits.map { hit ->
                    hit.json[Algolia.Key.ObjectID]!!.jsonPrimitive.content to hit.json["publishedAt"]!!.jsonPrimitive.content
                }
            }
            .toMap()
    }

    companion object {
        fun getInstance(
            sync: Sync
        ): AlgoliaSearchClient {
            return AlgoliaSearchClient(
                ApplicationID(algoliaCconfig.applicationId),
                APIKey(algoliaCconfig.apiKey),
                IndexName(sync.config.algoliaIndexName.name),
            )
        }
    }
}