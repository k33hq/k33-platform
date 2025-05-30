package com.k33.platform.app.invest

import com.k33.platform.app.invest.InvestService.asString
import io.kotest.core.spec.style.StringSpec

class SlackNotificationTest : StringSpec({

    "!send slack notification on publish" {
        SlackNotification.notifySlack(
            FundInfoRequest(
                investorType = InvestorType.PROFESSIONAL,
                name = "Test",
                phoneNumber = PhoneNumber(
                    countryCode = "47",
                    nationalNumber = "12345678",
                ),
                countryCode = ISO3CountyCode.NOR,
                fundName = "K33 Assets I Fund Limited"
            ).asString(investorEmail = "test@k33.com"),
            testMode = true,
        )
    }
})