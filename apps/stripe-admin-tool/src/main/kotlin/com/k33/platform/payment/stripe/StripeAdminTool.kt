package com.k33.platform.payment.stripe

import com.stripe.model.Subscription
import com.stripe.net.RequestOptions
import com.stripe.param.SubscriptionListParams
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

fun main() {

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
        for (expiresInDays in 1 .. 4) {

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