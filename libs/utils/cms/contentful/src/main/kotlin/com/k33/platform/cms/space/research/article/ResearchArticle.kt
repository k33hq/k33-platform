package com.k33.platform.cms.space.research.article

import com.k33.platform.cms.clients.ContentfulGraphql
import com.k33.platform.cms.content.Content
import com.k33.platform.cms.objectIDString
import com.k33.platform.cms.sync.Algolia
import com.k33.platform.cms.utils.optional
import com.k33.platform.cms.utils.richToPlainText
import com.k33.platform.utils.config.lazyResourceWithoutWhitespace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ResearchArticle(
    spaceId: String,
    token: String,
) : Content {
    private val client by lazy {
        ContentfulGraphql(
            spaceId = spaceId,
            token = token,
            type = "article"
        ) {
            Algolia.Key.ObjectID *= "sys.id"
            "title" *= "title"
            "subtitle" *= "subtitle"
            "authors" *= "authorsCollection.items[*].name"
            "tags" *= "tagsCollection.items[*].title"
            "publishedDate" *= "publishedDate"
            "publishedAt" *= "sys.publishedAt"
            "horizontalThumbnail" *= "horizontalThumbnail"
            optional {
                "body" *= { richToPlainText("body") }
                "slug" *= "linkedFrom.articleWebCollection.items[0].articleSlug"
                "sections" *= "linkedFrom.articleWebCollection.items[0].sectionsCollection.items[*].name"
                "summary" *= { richToPlainText("summary") }
                "keyPoints" *= "keyPoints"
            }
        }
    }

    private val queryOne by lazyResourceWithoutWhitespace("/research/article/queryOne.graphql")

    override suspend fun fetch(entityId: String): JsonObject? {
        val ids = fetchIdToModifiedMap().keys
        return client.fetch(queryOne, "articleId" to entityId)
            .singleOrNull()
            ?.let { jsonObject ->
                if (ids.contains(jsonObject.objectIDString)) {
                    jsonObject
                } else {
                    null
                }
            }
    }

    private val queryMany by lazyResourceWithoutWhitespace("/research/article/queryMany.graphql")

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

    private val queryIds by lazyResourceWithoutWhitespace("/research/article/queryIds.graphql")

    override suspend fun fetchIds(): List<String> {
        return clientForIds
            .fetch(queryIds)
            .mapNotNull(JsonObject::objectIDString)
    }

    override suspend fun fetchIdToModifiedMap(): Map<String, String> {
        return clientForIds
            .fetch(queryIds)
            .mapNotNull {
                (it.objectIDString ?: return@mapNotNull null) to
                        (it["publishedAt"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null)
            }
            .toMap()
    }

    companion object
}