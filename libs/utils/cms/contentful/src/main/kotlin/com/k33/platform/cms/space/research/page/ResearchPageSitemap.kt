package com.k33.platform.cms.space.research.page

import com.k33.platform.cms.config.ContentfulSpace
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.runBlocking
import java.io.File

object ResearchPageSitemap {

    private val logger by getLogger()

    suspend fun exportSitemap() {
        val researchPagesMetadata = ResearchPage(
            spaceId = ContentfulSpace.research.config.spaceId,
            token = ContentfulSpace.research.config.token,
        )

        val sitemap = buildString {
            appendLine("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """.trimIndent())
            researchPagesMetadata
                .fetchSitemap()
                .map { (slug, publishedAt) ->
                    appendLine(
                        """
                        <url>
                          <loc>https://k33.com/research/archive/articles/${slug}</loc>
                          <lastmod>${publishedAt}</lastmod>
                        </url>
                        """.trimIndent()
                    )
                }
            appendLine("""</urlset>""")
        }
        File("sitemap.xml").writeText(sitemap)
    }
}

fun main() {
    runBlocking {
        ResearchPageSitemap.exportSitemap()
    }
}