package com.k33.cms.contentful.admin

import com.contentful.java.cma.CMAClient
import com.contentful.java.cma.model.CMALink
import com.contentful.java.cma.model.CMAType

fun main() {
    val accessToken = ""
    val spaceId = ""

    val targetArticleEntryId = ""
    val recommendedArticlesEntryIdList = listOf("")

    val cmaClient = CMAClient.Builder()
        .setAccessToken(accessToken)
        .setSpaceId(spaceId)
        .build()

    val entry = cmaClient
        .entries()
        .fetchOne(targetArticleEntryId)

    entry.setField(
        "recommendedArticles", // key
        "en-US", // locale
        recommendedArticlesEntryIdList.map { recommendedArticlesEntryId ->
            CMALink(CMAType.Entry).apply {
                id = recommendedArticlesEntryId
            }
        }
    )

    cmaClient
        .entries()
        .update(entry)

//    val entries = cmaClient
//        .entries()
//        .fetchAll(
//            mapOf(
//                "content_type" to "article",
//                "limit" to "10",
//            )
//        )
//        .items
//
//    entries.forEach { entry ->
//        val title: String = entry.getField("title", "en-US")
//        println(title)
//    }
}