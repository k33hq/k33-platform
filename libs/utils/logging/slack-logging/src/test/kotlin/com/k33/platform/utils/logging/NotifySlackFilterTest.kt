package com.k33.platform.utils.logging

import io.kotest.core.spec.style.StringSpec

class NotifySlackFilterTest : StringSpec({
    val logger by getLogger()
    "!send log message with NOTIFY_SLACK marker to slack" {
        logWithMDC(
            "userId" to "test-user",
            "env" to "test"
        ) {
            logger.info(NotifySlack.ALERTS, "This is {} message", "information")
            logger.warn(NotifySlack.ALERTS, "This is {} message", "warning")
            logger.error(NotifySlack.ALERTS, "This is {} message", "error")
        }
    }
})