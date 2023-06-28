package com.k33.platform.cms.space.research.articleWeb

import com.k33.platform.cms.clients.ContentfulGraphql
import com.k33.platform.cms.content.Content
import com.k33.platform.cms.objectIDString
import com.k33.platform.cms.sync.Algolia
import com.k33.platform.utils.config.lazyResourceWithoutWhitespace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ResearchArticleWeb(
    spaceId: String,
    token: String,
) : Content {
    private val client by lazy {
        ContentfulGraphql(
            spaceId = spaceId,
            token = token,
            type = "articleWeb"
        ) {
            Algolia.Key.ObjectID *= "sys.id"
            "articleSlug" *= "articleSlug"
            "productSlug" *= "product.productSlug"
            "categorySlug" *= "category.categorySlug"
        }
    }

    private val queryOne by lazyResourceWithoutWhitespace("/research/articleWeb/queryOne.graphql")

    override suspend fun fetch(entityId: String): JsonObject? {
        val ids = fetchIdToModifiedMap().keys
        return client.fetch(queryOne, "articleWebId" to entityId)
            .singleOrNull()
            ?.let { jsonObject ->
                if (ids.contains(jsonObject.objectIDString)) {
                    jsonObject
                } else {
                    null
                }
            }
    }

    private val queryMany by lazyResourceWithoutWhitespace("/research/articleWeb/queryMany.graphql")

    override suspend fun fetchAll(): Collection<JsonObject> = client
        .fetch(queryMany)

    private val clientForIds by lazy {

        ContentfulGraphql(
            spaceId = spaceId,
            token = token,
            type = "article"
        ) {
            Algolia.Key.ObjectID *= "sys.id"
            "publishedAt" *= "sys.publishedAt"
        }
    }

    private val queryIds by lazyResourceWithoutWhitespace("/research/articleWeb/queryIds.graphql")

    override suspend fun fetchIds(): List<String> {
        return clientForIds
            .fetch(queryIds)
            .mapNotNull(JsonObject::objectIDString)
    }

    override suspend fun fetchIdToModifiedMap(): Map<String, String> {
        return clientForIds
            .fetch(queryIds)
            .mapNotNull {
                (it[Algolia.Key.ObjectID]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null) to
                        (it["publishedAt"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null)
            }
            .toMap()
    }

    suspend fun fetchSlugPrefixList(): List<String> {
        return fetchAll()
            .mapNotNull {
                val productSlug = (it["productSlug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null)
                val categorySlug = (it["categorySlug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null)
                "$categorySlug/$productSlug"
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .onEach { (path, count) ->
                println("$path: $count")
            }
            .map { it.key }
    }
}