package com.k33.platform.utils.analytics.google

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Currency
import java.util.HexFormat
import java.util.UUID
import kotlin.random.Random

class GA4ClientTest : BehaviorSpec({

    fun Instant.toEpochMicroSeconds() = this.epochSecond * 1_000_000 + this.nano / 1_000
    val timestamp = Instant.now().toEpochMicroSeconds()

    val events = listOf(
        SignUp(
            method = "google",
            pageUrl = null,
        ),
        Login(
            method = "google",
            pageUrl = null,
        ),
        BeginCheckout(
            currency = Currency.getInstance("USD"),
            value = 50f,
            productId = "k33-research-pro",
            pageUrl = null,
        ),
        BeginSubscriptionTrial(
            productId = "k33-research-pro",
        ),
        Purchase(
            currency = Currency.getInstance("USD"),
            transactionId = UUID.randomUUID().toString(),
            value = 50f,
            productId = "k33-research-pro",
        ),
        BeginSubscription(
            productId = "k33-research-pro",
        ),
        EndSubscription(
            productId = "k33-research-pro",
        ),
    )

    given("for an app client") {
        // 32-char hex string
        val appInstanceId = HexFormat.of().formatHex(Random.nextBytes(16))

        launch {
            `when`("validate events") {
                val isValid = GA4ClientForApp
                    .validate(
                        AppRequest(
                            appInstanceId = appInstanceId,
                            userAnalyticsId = "user_id",
                            timestampInMicroseconds = timestamp,
                            events = events
                        )
                    )
                then("should be success") {
                    isValid shouldBe true
                }
            }
        }
    }

    given("for an web client") {
        `when`("validate events") {
            val isValid = GA4ClientForWeb
                .validate(
                    WebRequest(
                        webClientId = "client_id",
                        userAnalyticsId = "user_id",
                        timestampInMicroseconds = timestamp,
                        events = events
                    )
                )
            then("should be success") {
                isValid shouldBe true
            }
        }
    }
})