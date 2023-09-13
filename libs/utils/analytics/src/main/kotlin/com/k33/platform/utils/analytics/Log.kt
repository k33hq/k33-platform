package com.k33.platform.utils.analytics

import com.k33.platform.utils.analytics.google.BeginCheckout
import com.k33.platform.utils.analytics.google.BeginSubscription
import com.k33.platform.utils.analytics.google.BeginSubscriptionTrial
import com.k33.platform.utils.analytics.google.EndSubscription
import com.k33.platform.utils.analytics.google.GA4ClientForWeb
import com.k33.platform.utils.analytics.google.Login
import com.k33.platform.utils.analytics.google.Purchase
import com.k33.platform.utils.analytics.google.SignUp
import com.k33.platform.utils.analytics.google.WebRequest
import java.time.Instant
import java.util.Currency
import kotlin.random.Random
import kotlin.random.nextULong


object Log {

    suspend fun signUp(
        webClientId: String?,
        userAnalyticsId: String?,
        idProvider: String?,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(SignUp(method = idProvider?.lowercase()))
            )
        )
    }

    suspend fun login(
        webClientId: String?,
        userAnalyticsId: String?,
        idProvider: String?,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(Login(method = idProvider?.lowercase()))
            )
        )
    }

    suspend fun beginCheckout(
        webClientId: String?,
        userAnalyticsId: String?,
        value: Float,
        currency: String,
        productId: String,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(
                    BeginCheckout(
                        currency = Currency.getInstance(currency.uppercase()),
                        value = value,
                        productId = productId,
                    )
                )
            )
        )
    }

    suspend fun beginSubscriptionTrial(
        webClientId: String?,
        userAnalyticsId: String?,
        productId: String,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(
                    BeginSubscriptionTrial(
                        productId = productId,
                    )
                )
            )
        )
    }

    suspend fun purchase(
        webClientId: String?,
        userAnalyticsId: String?,
        transactionId: String,
        value: Float,
        currency: String,
        productId: String,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(
                    Purchase(
                        currency = Currency.getInstance(currency.uppercase()),
                        transactionId = transactionId,
                        value = value,
                        productId = productId,
                    )
                )
            )
        )
    }

    suspend fun beginSubscription(
        webClientId: String?,
        userAnalyticsId: String?,
        productId: String,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(
                    BeginSubscription(
                        productId = productId,
                    )
                )
            )
        )
    }

    suspend fun endSubscription(
        webClientId: String?,
        userAnalyticsId: String?,
        productId: String,
    ) {
        GA4ClientForWeb.submit(
            WebRequest(
                webClientId = webClientId ?: createWebClientId(),
                userAnalyticsId = userAnalyticsId,
                events = listOf(
                    EndSubscription(
                        productId = productId,
                    )
                )
            )
        )
    }

    private fun createWebClientId() = "GA1.1.${Random.nextULong(from = 1_000_000_000u, until = 9_999_999_999u)}.${Instant.now().epochSecond}"
}