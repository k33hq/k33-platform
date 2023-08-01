package com.k33.cms.contentful.admin

import com.contentful.java.cma.CMAClient
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

        val entryIds = contentfulGraphqlClient
            .fetch()
            .mapValues { (_, sections) -> sections.any { section -> section.endsWith("reports") || section == "token-valuation/principles" } }
            .entries

        entryIds
            .asFlow()
            .map { (entryId, isVerticalThumbnail) ->
                try {
                    delay(150)
                    val entry = cmaClient
                        .entries()
                        .fetchOne(entryId)

                    // setting horizontalThumbnail
                    val horizontalThumbnail: Any = if (isVerticalThumbnail) {
                        entry.getField(
                            "image", // key
                            "en-US", // locale
                        )
                    } else {
                        entry.getField(
                            "thumbnail", // key
                            "en-US", // locale
                        )
                    }
                    entry.setField(
                        "horizontalThumbnail", // key
                        "en-US", // locale
                        horizontalThumbnail
                    )

                    // setting verticalThumbnail
                    if (isVerticalThumbnail) {
                        val verticalThumbnail: Any = entry.getField(
                            "thumbnail", // key
                            "en-US", // locale
                        )
                        entry.setField(
                            "verticalThumbnail", // key
                            "en-US", // locale
                            verticalThumbnail
                        )
                    }

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
                println("Updated $count of ${entryIds.size} articles with horizontalThumbnail")
            }
    }
}