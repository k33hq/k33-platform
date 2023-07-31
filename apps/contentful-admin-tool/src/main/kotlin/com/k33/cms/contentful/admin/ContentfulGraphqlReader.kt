package com.k33.cms.contentful.admin

import com.k33.platform.cms.clients.ContentfulGraphql
import com.k33.platform.utils.config.lazyResourceWithoutWhitespace
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class ContentfulGraphqlReader(
    spaceId: String,
    token: String,
) {
    private val client by lazy {

        ContentfulGraphql(
            spaceId = spaceId,
            token = token,
            type = "articleWeb"
        ) {
            "id" *= "article.sys.id"
            "sections" *= "sectionsCollection.items[*].name"
        }
    }

    private val query by lazyResourceWithoutWhitespace("/query.graphql")

    suspend fun fetch(): Map<String, List<String>> {
        return client
            .fetch(query)
            .mapNotNull { jsonObject ->
                (jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null) to
                        (jsonObject["sections"]?.jsonArray?.mapNotNull { section -> section.jsonPrimitive.content } ?: return@mapNotNull null)
            }
            .toMap()
    }
}