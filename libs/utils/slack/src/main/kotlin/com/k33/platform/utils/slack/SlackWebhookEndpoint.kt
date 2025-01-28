package com.k33.platform.utils.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.ktor.respond
import com.slack.api.bolt.ktor.toBoltRequest
import com.slack.api.bolt.util.SlackRequestParser
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

val app by lazy { App() }

private val slackRequestParser by lazy { SlackRequestParser(app.config()) }

fun Application.module() {
    routing {
        post("/webhooks/slack") {
            val request = toBoltRequest(call, slackRequestParser)
            respond(call, app.run(request))
        }
    }
}