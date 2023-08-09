package com.k33.cms.contentful.admin

import com.contentful.java.cma.CMAClient
import com.contentful.java.cma.model.CMALink
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

fun main() {
    val spaceId = System.getenv("CONTENTFUL_RESEARCH_SPACE_ID")
    val token = System.getenv("CONTENTFUL_RESEARCH_SPACE_TOKEN")
    val accessToken = System.getenv("CONTENTFUL_RESEARCH_SPACE_CMA_TOKEN")

    val cmaClient = CMAClient.Builder()
        .setAccessToken(accessToken)
        .setSpaceId(spaceId)
        .build()


    runBlocking {
        val contentfulGraphqlClient = ContentfulGraphqlReader(
            spaceId = spaceId,
            token = token,
        )

        val articles = contentfulGraphqlClient.fetch()

        articles
            .asFlow()
            .map { article ->
                try {
                    delay(150)
                    val entry = cmaClient
                        .entries()
                        .fetchOne(article.id)

                    entry.setField(
                        "articleSlug", // key
                        "en-US", // locale
                        article.slug
                    )

                    if (article.seoId != null) {
                        entry.setField(
                            "seo", // key
                            "en-US", // locale
                            article.seoId.toEntryLink(),
                        )
                    }

                    entry.setField(
                        "sections", // key
                        "en-US", // locale
                        article.sectionIds.map(String::toEntryLink)
                    )

                    // updating
                    delay(150)
                    val updatedEntry = cmaClient
                        .entries()
                        .update(entry)

                    delay(150)
                    cmaClient
                        .entries()
                        .publish(updatedEntry)

                    true
                } catch (e: Exception) {
                    println(e.message)
                    false
                }
            }
            .count { it }
            .let { count ->
                println("Updated $count of ${articles.size} articles")
            }
    }
}

fun String.toEntryLink() = mapOf(
    "sys" to mapOf(
        "type" to "Link",
        "linkType" to "Entry",
        "id" to this,
    )
)