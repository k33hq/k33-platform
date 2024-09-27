package com.k33.platform.utils.slack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SlackClientTest : StringSpec({

    "!send message to slack channel" {
        SlackClient.sendMessage(
            channel = ChannelId(System.getenv("SLACK_CHANNEL_ID")),
            message = "Testing"
        )
    }

    "!get private channel id" {
        SlackClient.getChannelId("") shouldBe ""
    }

    "!get public channel id" {
        SlackClient.getChannelId("") shouldBe ""
    }

    "!get all channel ids" {
        SlackClient.getChannelNameToIdMap().forEach { (key, value) ->
            println("$key => $value")
        }
    }
})