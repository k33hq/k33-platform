package com.k33.platform.email

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EmailSuppressionTest : BehaviorSpec({
    val userEmail = "test@k33.com"
    val suppressionGroupId = 21117L
    val emailService by getEmailService()
    xgiven("user does not exist in suppression group") {
        `when`("get user's suppression group list") {
            val suppressionGroups = emailService.getSuppressionGroups(userEmail)
            then("suppressionGroups should not be null") {
                suppressionGroups shouldNotBe null
            }
            then("suppressionGroups should not empty") {
                suppressionGroups shouldNotBe emptyList<SuppressionGroup>()
            }
            then("suppressionGroups should have suppressed = false") {
                suppressionGroups!!.all { it.suppressed.not() } shouldBe true
            }
        }
        `when`("upsert into suppression group") {
            val result = emailService.upsertIntoSuppressionGroup(userEmail, suppressionGroupId)
            then("result should be true") {
                result shouldBe true
            }
            then("suppressed = true for that suppression group") {
                val suppressionGroups = emailService.getSuppressionGroups(userEmail)
                val suppressed = suppressionGroups!!.filter { it.suppressed }
                suppressed.size shouldBe 1
            }
            `when`("remove from suppression group") {
                val result = emailService.removeFromSuppressionGroup(userEmail, suppressionGroupId)
                then("result should be true") {
                    result shouldBe true
                }
                then("suppressionGroups should have suppressed = false") {
                    val suppressionGroups = emailService.getSuppressionGroups(userEmail)
                    suppressionGroups!!.all { it.suppressed.not() } shouldBe true
                }
            }
        }
        `when`("remove from suppression group") {
            val result = emailService.removeFromSuppressionGroup(userEmail, suppressionGroupId)
            then("result should be true") {
                result shouldBe true
            }
            then("suppressionGroups should have suppressed = false") {
                val suppressionGroups = emailService.getSuppressionGroups(userEmail)
                suppressionGroups!!.all { it.suppressed.not() } shouldBe true
            }
        }
    }
})