package com.k33.platform.utils

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Application.module() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is BadRequestException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
                is NotFoundException -> call.respond(HttpStatusCode.NotFound, cause.message ?: "")
                else -> {
                    call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                    call.application.log.error("Internal Server Error", cause)
                }
            }
        }
    }
    install(ContentNegotiation) {
        json()
    }
    install(CallId) {
        val prefix = System.getenv("GCP_PROJECT_ID")
            ?.let { gcpProjectId -> "projects/$gcpProjectId/traces/" }
            ?: ""
        retrieve { call: ApplicationCall ->
            call.request.header("traceparent")
                ?.split("-")
                ?.getOrNull(1)
                ?.let { traceId -> prefix + traceId }
        }
    }
    routing {
        route("/ping") {
            get {
                log(application.log)
            }
            post {
                log(application.log)
            }
        }
        get("/utc") {
            call.respondText(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
        }
    }
    environment.monitor.subscribe(ApplicationStarting) {
        log.info("Application starting...")
    }
    environment.monitor.subscribe(ApplicationStarted) {
        log.info("Application started.")
    }
    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Application stopping...")
    }
    environment.monitor.subscribe(ApplicationStopped) {
        log.info("Application stopped.")
    }
}