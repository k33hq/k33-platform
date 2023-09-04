package com.k33.platform.email

interface EmailService {
    suspend fun sendEmail(
        from: Email,
        toList: List<Email>,
        ccList: List<Email> = emptyList(),
        bccList: List<Email> = emptyList(),
        mail: Mail,
        unsubscribeSettings: UnsubscribeSettings? = null,
    ): Boolean

    suspend fun upsertToMarketingContactLists(
        contactEmails: List<String>,
        contactListIds: List<String>,
    ): Boolean

    suspend fun removeFromMarketingContactLists(
        contactEmails: List<String>,
        contactListId: String,
    ): Boolean

    suspend fun getSuppressionGroups(
        userEmail: String
    ): List<SuppressionGroup>?

    suspend fun upsertIntoSuppressionGroup(
        userEmail: String,
        suppressionGroupId: Long,
    ): Boolean?

    suspend fun removeFromSuppressionGroup(
        userEmail: String,
        suppressionGroupId: Long,
    ): Boolean?
}

sealed class Mail

class MailContent(
    val subject: String,
    val contentType: ContentType,
    val body: String,
) : Mail()

class MailTemplate(
    val templateId: String,
) : Mail()

enum class ContentType {
    HTML,
    PLAIN_TEXT,
    MONOSPACE_TEXT,
}

data class Email(
    val address: String,
    val label: String? = null,
) {
    override fun toString() = "$label <$address>"
}

data class SuppressionGroup(
    val id: Long,
    val name: String,
    val suppressed: Boolean,
)