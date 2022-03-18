package no.arcane.platform.cms.events

import kotlinx.coroutines.runBlocking
import no.arcane.platform.cms.ContentfulConfig
import no.arcane.platform.cms.space.research.page.Author
import no.arcane.platform.cms.space.research.page.ResearchPageForSlack
import no.arcane.platform.utils.config.loadConfig
import no.arcane.platform.utils.slack.ChannelId
import no.arcane.platform.utils.slack.ChannelName
import no.arcane.platform.utils.slack.SlackClient
import java.time.ZonedDateTime

object SlackNotification {

    init {
        EventHub.subscribe(eventPattern = EventPattern(Resource.page, Action.publish)) { _, pageId ->
            notifySlack(pageId = pageId)
        }
    }

    private val contentfulConfig by loadConfig<ContentfulConfig>(
        "contentful",
        "contentfulAlgoliaSync.researchArticles.contentful"
    )

    private val researchPageForSlack by lazy {
        ResearchPageForSlack(
            spaceId = contentfulConfig.spaceId,
            token = contentfulConfig.token,
        )
    }

    private val slackChannel by lazy {
        System.getenv("SLACK_ALERTS_CHANNEL_ID")?.let { ChannelId(it) }
            ?: System.getenv("SLACK_ALERTS_CHANNEL_NAME")?.let { ChannelName(it) }
            ?: ChannelName("gcp-alerts")
    }

    suspend fun notifySlack(pageId: String) {
        val page = researchPageForSlack.fetch(pageId) ?: return
        val url = "https://arcane.no/research/${page.slug}"
        val message = if (page.publishedAt != page.firstPublishedAt) {
            "Research article is updated and republished at $url."
        } else {
            "New research article is published at $url."
        }
        val header = page.title
        val authorsText = page.authors.joinToString(prefix = "By ", separator = ", ") { author -> "*${author.name}*" }
        val publishOnText = "Published on _${ZonedDateTime.parse(page.publishDate).toLocalDate()}_"
        val subTitle = page.subtitle
        val tags = page.tags

        val altPlainTextMessage = """
                $message
                ${page.title}
                $authorsText  
            """.trimIndent()

        SlackClient.sendRichMessage(
            slackChannel,
            altPlainTextMessage,
        ) {
            section {
                markdownText(message)
            }
            divider()
            header {
                text(header)
            }
            context {
                elements {
                    markdownText("By")
                    page.authors.forEach { author: Author ->
                        image(author.image.url, altText = author.image.title)
                        markdownText("*${author.name}*")
                    }
                    markdownText(publishOnText)
                }
            }
            // image
            image {
                val nonSvgUrl = page.image.url + if (page.image.url.endsWith(".svg", ignoreCase = true)) {
                    "?fm=jpg"
                } else {
                    ""
                }
                title(page.image.title)
                imageUrl(nonSvgUrl)
                altText(page.image.fileName)
            }
            section {
                plainText(subTitle)
            }
            section {
                fields {
                    tags.forEach { tag ->
                        plainText("🔖 $tag", true)
                    }
                }
            }
            divider()
        }
    }
}

fun main() {
    runBlocking {
        SlackNotification.notifySlack("")
    }
}