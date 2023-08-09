package com.k33.cms.contentful.admin

import com.k33.platform.cms.clients.ContentfulGraphql
import com.k33.platform.cms.utils.optional
import com.k33.platform.utils.config.lazyResourceWithoutWhitespace
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class Article(
    val id: String,
    val slug: String,
    val sectionIds: List<String>,
    val seoId: String?,
)
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
            "slug" *= "articleSlug"
            "sectionIds" *= "sectionsCollection.items[*].sys.id"
            optional {
                "seoId" *= "seo.sys.id"
            }
        }
    }

    private val query by lazyResourceWithoutWhitespace("/query.graphql")

    suspend fun fetch(): List<Article> {
        return client
            .fetch(query)
            .map { jsonObject ->
                Article(
                    id = jsonObject["id"]!!.jsonPrimitive.content,
                    slug = jsonObject["slug"]!!.jsonPrimitive.content,
                    sectionIds = jsonObject["sectionIds"]!!.jsonArray.map { section -> section.jsonPrimitive.content },
                    seoId = jsonObject["seoId"]?.jsonPrimitive?.contentOrNull,
                )
            }
    }
}