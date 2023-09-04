package com.k33.platform.email

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class EmailServiceTest : StringSpec({

    val emailService by getEmailService()
    "send email".config(enabled = false) {
        emailService.sendEmail(
            from = Email("vihang@k33.com"),
            toList = listOf(Email("vihang@k33.com")),
            mail = MailContent(
                subject = "Test Email",
                contentType = ContentType.MONOSPACE_TEXT,
                body = "This is a test email",
            )
        )
    }

    "send email using template".config(enabled = false) {
        emailService.sendEmail(
            from = Email("vihang@k33.com"),
            toList = listOf(Email("vihang@k33.com")),
            mail = MailTemplate(
                templateId = ""
            ),
            unsubscribeSettings = UnsubscribeSettings(
                groupId = 0,
                preferencesGroupIds = listOf(),
            ),
        )
    }

    "get suppression groups".config(enabled = false) {
        val suppressionGroups = emailService.getSuppressionGroups(
            userEmail = "vihang@k33.com",
        )
        suppressionGroups!!.shouldContain(SuppressionGroup(id = 21117, name = "K33 Tech Updates", suppressed = false))
    }

    "upsert into a suppression group".config(enabled = false) {
        val result = emailService.upsertIntoSuppressionGroup(
            userEmail = "vihang@k33.com",
            suppressionGroupId = 21117
        )
        result shouldBe true
    }

    "remove from a suppression group".config(enabled = false) {
        val result = emailService.removeFromSuppressionGroup(
            userEmail = "vihang@k33.com",
            suppressionGroupId = 21117
        )
        result shouldBe true
    }
})