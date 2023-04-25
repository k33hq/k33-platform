package com.k33.platform.utils.logging

import io.kotest.core.spec.style.StringSpec

class NotifySlackFilterTest : StringSpec({
    val logger by getLogger()
    "send log message with NOTIFY_SLACK marker to slack".config(enabled = false) {
        logWithMDC(
            "userId" to "test-user",
            "env" to "test"
        ) {
            logger.info(NotifySlack.ALERTS, "This is information message")
            logger.warn(NotifySlack.ALERTS, "This is warning message")
            logger.error(NotifySlack.ALERTS, "This is error message")
        }
    }
})