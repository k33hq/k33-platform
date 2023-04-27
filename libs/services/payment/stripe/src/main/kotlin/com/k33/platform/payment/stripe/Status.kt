package com.k33.platform.payment.stripe

/**
 * https://stripe.com/docs/billing/subscriptions/overview#subscription-statuses
 *
 * https://stripe.com/docs/api/subscriptions/object#subscription_object-status
 *
 * For `collection_method=charge_automatically` a subscription moves into `incomplete` if the initial payment attempt fails.
 * A subscription in this state can only have metadata and default_source updated.
 * Once the first invoice is paid, the subscription moves into an active state.
 * If the first invoice is not paid within 23 hours, the subscription transitions to incomplete_expired.
 * This is a terminal state, the open invoice will be voided and no further invoices will be generated.
 *
 * A subscription that is currently in a trial period is trialing and moves to active when the trial period is over.
 *
 * If subscription `collection_method=charge_automatically` it becomes `past_due` when payment to renew it
 * fails and `canceled or unpaid` (depending on your subscriptions settings) when Stripe has exhausted all payment retry attempts.
 *
 * If subscription collection_method=send_invoice it becomes `past_due` when its invoice is not paid by the due date,
 * and `canceled` or `unpaid` if it is still not paid by an additional deadline after that.
 * Note that when a subscription has a status of unpaid, no subsequent invoices will be attempted (invoices will be created, but then immediately automatically closed).
 * After receiving updated payment information from a customer, you may choose to reopen and pay their closed invoices.
 *
 */
@Suppress("EnumEntryName")
enum class Status {
    trialing,

    /**
     * For `collection_method=charge_automatically` a subscription moves into `incomplete` if the initial payment attempt fails.
     */
    incomplete,

    /**
     * In `incomplete` state, if the first invoice is not paid within 23 hours, the subscription transitions to `incomplete_expired`.
     * This is a terminal state, the open invoice will be voided and no further invoices will be generated.
     */
    incomplete_expired,

    /**
     * Once the first invoice is paid, the subscription moves into an `active` state.
     */
    active,

    /**
     * For `collection_method=charge_automatically` a subscription moves `past_due` when payment to renew it fails.
     * If it is still not paid by an additional deadline after that, a subscription moves `canceled` or `unpaid`.
     */
    past_due,
    unpaid,
    paused,

    canceled,

    /**
     * This is never returned in Stripe's response.
     * This status is only used in request filter.
     */
    ended,
}