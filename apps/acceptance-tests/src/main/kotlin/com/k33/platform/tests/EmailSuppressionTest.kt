package com.k33.platform.tests

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class EmailSuppressionTest : BehaviorSpec({

    val email = "test@k33.com"
    val suppressionGroupId = 21117L
    suspend fun getSuppressionGroups(): HttpResponse {
        return apiClient.get {
            url(path = "/suppression-groups")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
            }
        }
    }
    suspend fun upsertIntoSuppressionGroup(): HttpResponse {
        return apiClient.put {
            url(path = "/suppression-groups/$suppressionGroupId")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
            }
        }
    }
    suspend fun removeFromSuppressionGroup(): HttpResponse {
        return apiClient.delete {
            url(path = "/suppression-groups/$suppressionGroupId")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
            }
        }
    }

    given("user does not exist in suppression group") {
        `when`("get user's suppression group list") {
            val response = getSuppressionGroups()
            then("response should be 200 OK") {
                response.status shouldBe HttpStatusCode.OK
            }
            val suppressionGroups = response.body<List<SuppressionGroup>>()
            then("suppressionGroups should not empty") {
                suppressionGroups shouldNotBe emptyList<SuppressionGroup>()
            }
            then("suppressionGroups should have suppressed = false") {
                suppressionGroups.all { it.suppressed.not() } shouldBe true
            }
        }
        `when`("upsert into suppression group") {
            then("response should be 204 No Content") {
                upsertIntoSuppressionGroup().status shouldBe HttpStatusCode.NoContent
            }
            then("suppressed = true for that suppression group") {
                val suppressionGroups = getSuppressionGroups().body<List<SuppressionGroup>>()
                val suppressed = suppressionGroups.filter { it.suppressed }
                suppressed.size shouldBe 1
            }
            `when`("remove from suppression group") {
                val response = removeFromSuppressionGroup()
                then("response should be 204 No Content") {
                    response.status shouldBe HttpStatusCode.NoContent
                }
                then("suppressionGroups should have suppressed = false") {
                    val suppressionGroups = getSuppressionGroups().body<List<SuppressionGroup>>()
                    suppressionGroups.all { it.suppressed.not() } shouldBe true
                }
            }
        }
        `when`("remove from suppression group") {
            then("result should be true") {
                removeFromSuppressionGroup().status shouldBe HttpStatusCode.NoContent
            }
            then("suppressionGroups should have suppressed = false") {
                val suppressionGroups = getSuppressionGroups().body<List<SuppressionGroup>>()
                suppressionGroups.all { it.suppressed.not() } shouldBe true
            }
        }
    }
})

@Serializable
data class SuppressionGroup(
    val id: Long,
    val name: String,
    val suppressed: Boolean,
)