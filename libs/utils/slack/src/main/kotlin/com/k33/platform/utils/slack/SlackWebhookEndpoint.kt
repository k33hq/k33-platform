package com.k33.platform.utils.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.request.Request
import com.slack.api.bolt.request.RequestHeaders
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.util.QueryStringParser
import com.slack.api.bolt.util.SlackRequestParser
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.contentType
import io.ktor.server.request.path
import io.ktor.server.request.queryString
import io.ktor.server.request.receiveStream
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

//
// FIXME: Workaround until com.slack.api:bolt-ktor upgrades ktor dependency from 2 to 3.
// Ref: https://github.com/slackapi/java-slack-sdk/blob/main/bolt-ktor/src/main/kotlin/com/slack/api/bolt/ktor/package.kt
//

private suspend fun toBoltRequest(call: ApplicationCall, parser: SlackRequestParser): Request<*>? {
    val requestBody = call.receiveTextWithCorrectEncoding()
    val queryString = QueryStringParser.toMap(call.request.queryString())
    val headers = RequestHeaders(call.request.headers.toMap())
    val rawRequest = SlackRequestParser.HttpRequest.builder()
        .requestUri(call.request.path())
        .queryString(queryString)
        .requestBody(requestBody)
        .headers(headers)
        .remoteAddress(call.request.origin.remoteHost)
        .build()
    return parser.parse(rawRequest)
}

private suspend fun respond(call: ApplicationCall, boltResponse: Response) {
    for (header in boltResponse.headers) {
        for (value in header.value) {
            call.response.header(header.key, value)
        }
    }
    val status = HttpStatusCode.fromValue(boltResponse.statusCode)
    if (boltResponse.body != null) {
        val message = TextContent(
            boltResponse.body,
            ContentType.parse(boltResponse.contentType),
            status
        )
        call.respond(message)
    } else call.respond(status)
}

private suspend fun ApplicationCall.receiveTextWithCorrectEncoding(): String = withContext(Dispatchers.IO) {
    fun ContentType.defaultCharset(): Charset = when (this) {
        ContentType.Application.Json -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }
    val contentType = request.contentType()
    val suitableCharset = contentType.charset() ?: contentType.defaultCharset()
    receiveStream().bufferedReader(charset = suitableCharset).readText()
}