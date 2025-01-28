package com.k33.platform.analytics

import com.k33.platform.filestore.FileStoreService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.math.roundToInt

fun Application.module() {

    routing {
        route("/admin/jobs") {
            post("update-firebase-users-stats") {
                updateFirebaseUsersStats()
                call.respond(HttpStatusCode.OK)
            }
            put("sync-sendgrid-contacts/{contactListId}") {
                val contactListId = call.parameters["contactListId"] ?: throw BadRequestException("Missing path parameter: contactListId")
                SendgridContactsSync.syncSendgridContacts(
                    contactListId = contactListId,
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

suspend fun updateFirebaseUsersStats() {
    val userAnalytics = UserAnalytics()
    val csvFileContents = buildString {
        appendLine("""DATE, USER_CREATED_COUNT, MOVING_AVG_LAST_SEVEN""")
        val userCountList = mutableListOf<Int>()
        userAnalytics.usersCreatedTimeline().forEach { (time, count) ->
            userCountList += count
            appendLine(""""$time", $count, ${userCountList.takeLast(7).average().roundToInt()}""")
        }
    }
    FileStoreService.upload(
        bucketConfigId = "user-created-timeline",
        filePath = "user-created-timeline.csv",
        contents = csvFileContents.toByteArray()
    )
}