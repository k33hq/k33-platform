package com.k33.platform.payment.stripe

import io.kotest.core.spec.style.StringSpec
import java.time.Instant
import java.util.UUID

class StripeClientTest: StringSpec({
    "!create/fetch checkout session" {
        val session = StripeService.createOrFetchCheckoutSession(
            customerEmail = "test@k33.com",
            priceId = "",
            successUrl = "https://dev.k33.com/research/settings",
            cancelUrl = "https://dev.k33.com/research/settings",
            webClientId = UUID.randomUUID().toString(),
            userAnalyticsId = null,
        )
        println(session.url)
    }

    "!create customer portal session" {
        val url = StripeService.createCustomerPortalSession(
            customerEmail = "test@k33.com",
            returnUrl = "https://dev.k33.com/research/settings",
        )
        println(url)
    }

    "!get customers with no subscription" {
        val createdSince = Instant.parse("2024-01-01T00:00:00Z")
        val createdBefore = Instant.parse("2024-02-06T00:00:00Z")
        val emailsOfCustomersWithNoSubscription = StripeService.getCustomersWithoutSubscription(
            createdSince = createdSince,
            createdBefore = createdBefore,
        )
        println(emailsOfCustomersWithNoSubscription)
    }
})