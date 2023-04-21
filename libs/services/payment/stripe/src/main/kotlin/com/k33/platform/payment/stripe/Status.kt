package com.k33.platform.payment.stripe

@Suppress("EnumEntryName")
enum class Status {
    active,
    past_due,
    unpaid,
    canceled,
    incomplete,
    incomplete_expired,
    trialing,
    paused,
    ended,
}