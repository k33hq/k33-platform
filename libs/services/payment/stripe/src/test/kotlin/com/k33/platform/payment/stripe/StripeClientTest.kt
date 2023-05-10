package com.k33.platform.payment.stripe

import io.kotest.core.spec.style.StringSpec
import java.util.UUID

class StripeClientTest: StringSpec({
    "create/fetch checkout session".config(enabled = false) {
        val session = StripeClient.createOrFetchCheckoutSession(
            customerEmail = "test@k33.com",
            priceId = "",
            successUrl = "https://dev.k33.com/research/settings",
            cancelUrl = "https://dev.k33.com/research/settings",
            webClientId = UUID.randomUUID().toString(),
            userAnalyticsId = null,
        )
        println(session.url)
    }

    "create customer portal session".config(enabled = false) {
        val url = StripeClient.createCustomerPortalSession(
            customerEmail = "test@k33.com",
            returnUrl = "https://dev.k33.com/research/settings",
        )
        println(url)
    }
})