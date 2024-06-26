package com.k33.platform.payment.stripe

import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.net.RequestOptions
import com.stripe.param.EventListParams
import com.stripe.param.SubscriptionListParams
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

fun explorePreviousAttributes() {
    val stripeApiKey = ""

    runBlocking {
        val requestOptions = RequestOptions
            .builder()
            .setApiKey(stripeApiKey)
            .setClientId("k33-stripe-admin-tool")
            .build()

        val events = mutableListOf<Event>()

        do {
            val params = EventListParams
                .builder()
                .setType("customer.subscription.updated")
                .setLimit(100)
                .apply {
                    val last = events.lastOrNull()?.id
                    if (last != null) {
                        this.setStartingAfter(last)
                    }
                }
                .build()
            val result = Event.list(params, requestOptions)
            events.addAll(result.data ?: emptyList())
        } while (result?.hasMore == true)

        events
            .groupBy { it.data.previousAttributes.keys }
            .mapValues { it.value.size }
            .forEach { (previousAttributes, size) ->
                println("$previousAttributes -> $size")
            }
    }
}

fun customerEmailsWithTooExpensiveAsCancelFeedback() {
    val stripeApiKey = ""
    val priceId = ""

    runBlocking {
        val requestOptions = RequestOptions
            .builder()
            .setApiKey(stripeApiKey)
            .setClientId("k33-stripe-admin-tool")
            .build()

        val subscriptions = mutableListOf<Subscription>()

        do {
            val params = SubscriptionListParams
                .builder()
                .setPrice(priceId)
                .setStatus(SubscriptionListParams.Status.CANCELED)
                .setCollectionMethod(SubscriptionListParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                .setLimit(100)
                .apply {
                    val last = subscriptions.lastOrNull()?.id
                    if (last != null) {
                        this.setStartingAfter(last)
                    }
                }
                .build()

            val result = Subscription.list(params, requestOptions)
            subscriptions.addAll(result.data ?: emptyList())
        } while (result?.hasMore == true)

        subscriptions
            .filter { it.cancellationDetails.feedback == "too_expensive" }
            .map { subscription ->
                async {
                    Customer.retrieve(subscription.customer, requestOptions).email to Instant.ofEpochSecond(subscription.cancelAt)
                }
            }
            .awaitAll()
            .forEach { (email, cancelAt) ->
                println("$email, $cancelAt")
            }
    }
}

fun cancellationReasonAndFeedback() {
    val stripeApiKey = ""
    val priceId = ""

    runBlocking {
        val requestOptions = RequestOptions
            .builder()
            .setApiKey(stripeApiKey)
            .setClientId("k33-stripe-admin-tool")
            .build()

        val subscriptions = mutableListOf<Subscription>()

        do {
            val params = SubscriptionListParams
                .builder()
                .setPrice(priceId)
                .setStatus(SubscriptionListParams.Status.CANCELED)
                .setCollectionMethod(SubscriptionListParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                .setLimit(100)
                .apply {
                    val last = subscriptions.lastOrNull()?.id
                    if (last != null) {
                        this.setStartingAfter(last)
                    }
                }
                .build()

            val result = Subscription.list(params, requestOptions)
            subscriptions.addAll(result.data)
        } while (result?.hasMore == true)

        subscriptions
            .groupBy { it.cancellationDetails.reason ?: "unknown" }
            .mapValues { it.value.size }
            .forEach { (reason, size) ->
                println("$reason -> $size")
            }

        println()

        subscriptions
            .groupBy { it.cancellationDetails.feedback ?: "unspecified" }
            .mapValues { it.value.size }
            .forEach { (feedback, size) ->
                println("$feedback -> $size")
            }
    }
}

fun trialExpiryHistogram() {

    val stripeApiKey = ""
    val priceId = ""

    runBlocking {
        val requestOptions = RequestOptions
            .builder()
            .setApiKey(stripeApiKey)
            .setClientId("k33-stripe-admin-tool")
            .build()

        val today = Instant.now().truncatedTo(ChronoUnit.DAYS)

        println("Expires in days,Trail,Trail Not Uncancelled,Trail Not Uncancelled and Without Payment Method")
        for (expiresInDays in 1..4) {

            val expiryGt = today.plus((expiresInDays - 1).days.toJavaDuration())
            val expiryLt = today.plus(expiresInDays.days.toJavaDuration())

            val params = SubscriptionListParams
                .builder()
                .setPrice(priceId)
                .setStatus(SubscriptionListParams.Status.TRIALING)
                .setCollectionMethod(SubscriptionListParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                .setCurrentPeriodEnd(
                    SubscriptionListParams.CurrentPeriodEnd
                        .builder()
                        .setGte(expiryGt.epochSecond)
                        .setLte(expiryLt.epochSecond)
                        .build()
                )
                .setLimit(100)
                .build()
            // trialing subscriptions
            val subscriptions = Subscription
                .list(params, requestOptions)
                .data
            val subscriptionsCount = subscriptions.size

            // trialing subscriptions not cancelled
            val notCancelled = subscriptions
                .filter { subscription ->
                    subscription.cancelAtPeriodEnd == false
                }
            val notCancelledCount = notCancelled.size

            // trialing subscriptions not cancelled and without payment method
            val withoutPaymentMethod = notCancelled
                .filter { subscription ->
                    subscription.defaultPaymentMethod == null
                }

            val withoutPaymentMethodCount = withoutPaymentMethod.size

            println("$expiresInDays,$subscriptionsCount,$notCancelledCount,$withoutPaymentMethodCount")
        }
    }
}