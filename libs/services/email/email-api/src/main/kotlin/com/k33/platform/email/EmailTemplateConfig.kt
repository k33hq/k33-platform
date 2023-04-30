package com.k33.platform.email

data class EmailConfig(
    val email: String,
    val label: String,
)

data class UnsubscribeSettings(
    val groupId: Int,
    val preferencesGroupIds: List<Int>,
)

data class EmailTemplateConfig(
    val from: EmailConfig,
    val sendgridTemplateId: String,
    val unsubscribeSettings: UnsubscribeSettings? = null,
)