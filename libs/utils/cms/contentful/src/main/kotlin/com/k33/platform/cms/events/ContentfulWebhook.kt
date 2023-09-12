package com.k33.platform.cms.events

import com.k33.platform.cms.sync.ContentfulToAlgolia
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {

    val logger by getLogger()

    // trigger init to subscribe to EventHub
    // SlackNotification
    // ResearchPageValidation
    ContentfulToAlgolia

    routing {
        post("/webhooks/contentful") {
            val topic = call.request.header("X-Contentful-Topic")!!
            val entityContentType = call.request.header("X-CTFL-Content-Type") ?: return@post
            val entityId = call.request.header("X-CTFL-Entity-ID") ?: return@post
            val (type, action) = Regex("^ContentManagement.([a-zA-Z]+).([a-zA-Z]+)$").find(topic)!!.destructured
            logWithMDC("${entityContentType}Id" to entityId) {
                logger.info("Received contentful event: $type.$action for $entityContentType/$entityId")
                if (type == "Entry") {
                    val eventType = EventType(
                        resource = Resource.valueOf(entityContentType),
                        action = Action.valueOf(action)
                    )
                    EventHub.notify(
                        eventType = eventType,
                        id = entityId,
                    )
                } else if (type == "ContentType") {
                    logger.warn("Content type is modified")
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

