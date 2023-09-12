package com.k33.platform.utils.analytics.google

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Currency

sealed class Event(
    val name: String,
    val params: Param?,
)

sealed interface Param

// https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference/events#sign_up
class SignUp(
    method: String? = null
) : Event(
    name = "sign_up",
    params = method?.let { AuthParam(method = method) }
)

// https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference/events#login
class Login(
    method: String? = null
) : Event(
    name = "login",
    params = method?.let { AuthParam(method = method) }
)

data class AuthParam(
    val method: String
) : Param

// https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference/events#begin_checkout
class BeginCheckout(
    currency: Currency,
    value: Float,
    productId: String,
) : Event(
    name = "begin_checkout",
    params = BeginCheckoutParam(
        currency = currency.currencyCode,
        value = value,
        items = listOf(
            Item(
                itemId = productId,
                price = value,
            )
        )
    )
)

data class BeginCheckoutParam(
    val currency: String,
    val value: Float,
    val items: List<Item>,
) : Param

// custom event
class BeginSubscriptionTrial(
    productId: String,
) : Event(
    name = "begin_subscription_trial",
    params = SubscriptionParam(
        productId = productId,
    )
)

// custom event
class BeginSubscription(
    productId: String,
) : Event(
    name = "begin_subscription",
    params = SubscriptionParam(
        productId = productId,
    )
)

// custom event
class EndSubscription(
    productId: String,
) : Event(
    name = "end_subscription",
    params = SubscriptionParam(
        productId = productId,
    )
)

data class SubscriptionParam(
    val productId: String,
) : Param

// https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference/events#purchase
class Purchase(
    currency: Currency,
    transactionId: String,
    value: Float,
    productId: String,
) : Event(
    name = "purchase",
    params = PurchaseParam(
        currency = currency.currencyCode,
        transactionId = transactionId,
        value = value,
        items = listOf(
            Item(
                itemId = productId,
                price = value,
            )
        )
    )
)

data class PurchaseParam(
    val currency: String,
    @JsonProperty("transaction_id")
    val transactionId: String,
    val value: Float,
    val items: List<Item>,
) : Param

data class Item(
    @JsonProperty("item_id")
    val itemId: String,
    val price: Float,
)

