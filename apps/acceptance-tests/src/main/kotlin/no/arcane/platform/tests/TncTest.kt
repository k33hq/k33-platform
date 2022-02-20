package no.arcane.platform.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@kotlin.time.ExperimentalTime
class TncTest : StringSpec({

    val tncRequest = TncRequest(
        spaceId = "spaceId",
        entryId = "entryId",
        fieldId = "fieldId",
        version = "version",
        accepted = true
    )

    val now = ZonedDateTime.now(ZoneOffset.UTC).toString()

    val tnc = TncResponse(
        tncId = "privacy-policy",
        spaceId = "spaceId",
        entryId = "entryId",
        fieldId = "fieldId",
        version = "version",
        accepted = true,
        timestamp = now,
    )

    val userId = UUID.randomUUID().toString()

    "POST /tnc/privacy-policy -> Submit Terms and Conditions" {

        val savedTnc = apiClient.post<TncResponse>(path = "tnc/privacy-policy") {
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
            contentType(ContentType.Application.Json)
            body = tncRequest
        }
        savedTnc.copy(timestamp = now) shouldBe tnc
    }

    "GET /tnc/privacy-policy -> Check if Terms and Conditions are saved" {
        val savedTnc = apiClient.get<TncResponse>(path = "tnc/privacy-policy") {
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
        }
        savedTnc.copy(timestamp = now) shouldBe tnc
    }

    // TODO enable the test
    "POST /tnc/privacy-policy/email -> Send Terms and Conditions in email".config(enabled = false) {
        apiClient.post<Unit>(path = "tnc/privacy-policy/email") {
            headers {
                appendEndpointsApiUserInfoHeader(userId)
            }
        }
    }
})

@Serializable
data class TncRequest(
    val version: String,
    val accepted: Boolean,
    val spaceId: String,
    val entryId: String,
    val fieldId: String,
)

@Serializable
data class TncResponse(
    val tncId: String,
    val version: String,
    val accepted: Boolean,
    val spaceId: String,
    val entryId: String,
    val fieldId: String,
    val timestamp: String,
)