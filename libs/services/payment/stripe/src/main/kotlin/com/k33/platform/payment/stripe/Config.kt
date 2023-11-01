package com.k33.platform.payment.stripe

import com.k33.platform.email.EmailTemplateConfig

const val TRIAL_PERIOD_DAYS = 30

data class ProductConfig(
    val name: String,
    val stripeProduct: StripeProduct,
    val sendgridContactListId: String,
    val welcomeEmailForTrial: EmailTemplateConfig,
    val welcomeEmail: EmailTemplateConfig,
)

data class StripeProduct(
    val id: String,
    val enableTrial: Boolean,
)