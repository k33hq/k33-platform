package com.k33.platform.payment.stripe

import com.k33.platform.email.EmailTemplateConfig

const val TRIAL_PERIOD_DAYS = 30

data class ProductConfig(
    val name: String,
    val stripeProductId: String,
    val sendgridContactListId: String,
    val welcomeEmailForTrial: EmailTemplateConfig,
    val welcomeEmail: EmailTemplateConfig,
)
