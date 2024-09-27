package com.k33.platform.utils

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
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
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Application.module() {
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is BadRequestException -> call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        code = "BAD_REQUEST",
                        message = throwable.message ?: "",
                    ),
                )
                is NotFoundException -> call.respond(
                    status = HttpStatusCode.NotFound,
                    message = ErrorResponse(
                        code = "NOT_FOUND",
                        message = throwable.message ?: "",
                    ),
                )
                is RestApiException -> call.respond(
                    status = throwable.status,
                    message = ErrorResponse(
                        code = throwable.code,
                        message = throwable.message,
                    )
                )
                else -> {
                    call.application.log.error("Internal Server Error", throwable)
                    call.respond(status = HttpStatusCode.InternalServerError,
                        message = ErrorResponse(
                            code = "INTERNAL_SERVER_ERROR",
                            message = "Internal Server Error",
                        ),
                    )
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
    monitor.subscribe(ApplicationStarting) {
        log.info("Application starting...")
    }
    monitor.subscribe(ApplicationStarted) {
        log.info("Application started.")
    }
    monitor.subscribe(ApplicationStopping) {
        log.info("Application stopping...")
    }
    monitor.subscribe(ApplicationStopped) {
        log.info("Application stopped.")
    }
}