package com.k33.platform.utils

import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import org.slf4j.Logger

suspend fun RoutingContext.log(logger: Logger) {
    logger.info(
        call.request.headers.entries()
            .filterNot { (name, _) -> name.equals("Authorization", ignoreCase = true) }
            .joinToString { (name, values) ->
                "$name: $values"
            }
    )
    call.respondText("pong")
}