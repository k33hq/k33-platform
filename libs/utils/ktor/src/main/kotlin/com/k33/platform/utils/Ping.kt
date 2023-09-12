package com.k33.platform.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.Logger

suspend fun PipelineContext<Unit, ApplicationCall>.log(logger: Logger) {
    logger.info(
        call.request.headers.entries()
            .filterNot { (name, _) -> name.equals("Authorization", ignoreCase = true) }
            .joinToString { (name, values) ->
                "$name: $values"
            }
    )
    call.respondText("pong")
}