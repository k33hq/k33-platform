package com.k33.platform.email

import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.getLogger
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.ASM
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.MailSettings
import com.sendgrid.helpers.mail.objects.Personalization
import com.sendgrid.helpers.mail.objects.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


typealias K33Email = com.k33.platform.email.Email

private fun K33Email.toSendgridEmail(): Email {
    return if (label.isNullOrBlank()) {
        Email(address)
    } else {
        Email(address, label)
    }
}


object SendGridService : EmailService {

    private val logger by getLogger()

    private val sendGridConfig by loadConfig<SendGridConfig>(name = "sendgrid", path = "sendgrid")

    override suspend fun sendEmail(
        from: K33Email,
        toList: List<K33Email>,
        ccList: List<K33Email>,
        bccList: List<K33Email>,
        mail: com.k33.platform.email.Mail,
        unsubscribeSettings: UnsubscribeSettings?,
    ): Boolean {

        val sendgridMail = Mail().apply {
            this.from = from.toSendgridEmail()
            addPersonalization(
                Personalization().apply {
                    toList.map(K33Email::toSendgridEmail).forEach(::addTo)
                    ccList.map(K33Email::toSendgridEmail).forEach(::addCc)
                    bccList.map(K33Email::toSendgridEmail).forEach(::addBcc)
                }
            )
            when (mail) {
                is MailContent -> {
                    logger.debug(
                        """
    
                        from: $from
                        to: ${toList.joinToString()}
                        cc: ${ccList.joinToString()}
                        bcc: ${bccList.joinToString()}
                        subject: $subject
            
                        """.trimIndent() + mail.body
                    )
                    subject = mail.subject
                    addContent(
                        when (mail.contentType) {
                            ContentType.HTML -> Content("text/html", mail.body)
                            ContentType.PLAIN_TEXT -> Content("text/plain", mail.body)
                            ContentType.MONOSPACE_TEXT -> Content(
                                "text/html",
                                buildString {
                                    appendHTML().div {
                                        style = "font-family: monospace;"
                                        for (line in mail.body.lines()) {
                                            +line
                                            br()
                                        }
                                    }
                                }
                            )
                        }
                    )
                }

                is MailTemplate -> {
                    setTemplateId(mail.templateId)
                }
            }
            if (unsubscribeSettings != null) {
                asm = ASM().apply {
                    groupId = unsubscribeSettings.groupId
                    groupsToDisplay = unsubscribeSettings.preferencesGroupIds.toIntArray()
                }
            }
            if (sendGridConfig.enabled.not()) {
                mailSettings = MailSettings().apply {
                    setSandboxMode(
                        Setting().apply {
                            enable = true
                        }
                    )
                }
            }
        }

        try {
            val response = withContext(Dispatchers.IO) {
                SendGrid(sendGridConfig.apiKey).api(
                    Request().apply {
                        method = Method.POST
                        endpoint = "mail/send"
                        body = sendgridMail.build()
                    }
                )
            }
            return (response.statusCode in 200..299)

        } catch (e: Exception) {
            logger.error("Failed to send email", e)
            return false
        }
    }

    // https://docs.sendgrid.com/api-reference/contacts/add-or-update-a-contact
    override suspend fun upsertToMarketingContactLists(
        contactEmails: List<String>,
        contactListIds: List<String>,
    ): Boolean {

        @Serializable
        data class Contact(
            val email: String,
        )

        @Serializable
        data class UpsertContactsRequest(
            @SerialName("list_ids") val listIds: List<String>,
            val contacts: List<Contact>
        )

        return coroutineScope {
            val sendGrid = SendGrid(sendGridConfig.apiKey)
            contactEmails
                // max limit of 30k contacts in email request
                .chunked(30_000)
                .map { emailStringList ->
                    async {
                        try {
                            val upsertContactsRequest = UpsertContactsRequest(
                                listIds = contactListIds,
                                contacts = emailStringList.map(::Contact),
                            )
                            val jsonBody = Json.encodeToString(upsertContactsRequest)
                            val request = Request().apply {
                                method = Method.PUT
                                endpoint = "/marketing/contacts"
                                body = jsonBody
                            }
                            val response = withContext(Dispatchers.IO) {
                                sendGrid.api(request)
                            }
                            val success = response.statusCode in 200..299
                            if (!success) {
                                logger.error("Response for failed upsert contacts: ${response.body}")
                            }
                            success
                        } catch (e: Exception) {
                            logger.error("Error: upsert contacts", e)
                            false
                        }
                    }
                }
                .awaitAll()
                .all { it }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    // https://docs.sendgrid.com/api-reference/lists/remove-contacts-from-a-list
    override suspend fun removeFromMarketingContactLists(
        contactEmails: List<String>,
        contactListId: String,
    ): Boolean {
        val sendGrid = SendGrid(sendGridConfig.apiKey)
        val contactIds = getContactIds(sendGrid, contactEmails) ?: return false
        return try {
            val request = Request().apply {
                method = Method.DELETE
                endpoint = "/marketing/lists/$contactListId/contacts"
                queryParams["contact_ids"] = contactIds.joinToString(separator = ",")
            }
            val response = withContext(Dispatchers.IO) {
                sendGrid.api(request)
            }
            (response.statusCode in 200..299)
        } catch (e: Exception) {
            logger.error("Failed to un-list contacts", e)
            false
        }
    }

    private suspend fun getContactIds(
        sendGrid: SendGrid,
        contactEmails: List<String>,
    ): List<String>? {

        @Serializable
        data class GetContactsRequest(
            val emails: List<String>,
        )

        @Serializable
        data class Contact(
            val id: String,
        )

        @Serializable
        data class OptionalContact(
            val contact: Contact? = null,
        )

        @Serializable
        data class GetContactsResponse(
            val result: Map<String, OptionalContact>,
        )

        return coroutineScope {
            try {
                val getContactsRequest = GetContactsRequest(
                    emails = contactEmails,
                )
                val jsonBody = Json.encodeToString(getContactsRequest)
                val request = Request().apply {
                    method = Method.POST
                    endpoint = "/marketing/contacts/search/emails"
                    body = jsonBody
                }
                val response = withContext(Dispatchers.IO) {
                    sendGrid.api(request)
                }
                val getContactsResponse: GetContactsResponse = json.decodeFromString(response.body)
                getContactsResponse
                    .result
                    .values
                    .mapNotNull(OptionalContact::contact)
                    .map(Contact::id)
            } catch (e: Exception) {
                logger.error("Failed to search contacts by email", e)
                null
            }
        }
    }

    // https://docs.sendgrid.com/api-reference/suppressions-suppressions/retrieve-all-suppression-groups-for-an-email-address
    override suspend fun getSuppressionGroups(
        userEmail: String,
    ) : List<SuppressionGroup>? {

        @Serializable
        data class Suppression(
            val id: Long,
            val name: String,
            val description: String,
            @SerialName("is_default") val isDefault: Boolean,
            val suppressed: Boolean,
        )

        @Serializable
        data class SuppressionList(
            val suppressions: List<Suppression>
        )

        val sendGrid = SendGrid(sendGridConfig.apiKey)
        return try {
            val request = Request().apply {
                method = Method.GET
                endpoint = "/asm/suppressions/$userEmail"
            }
            val response = withContext(Dispatchers.IO) {
                sendGrid.api(request)
            }
            if (response.statusCode !in 200..299) {
                logger.warn(
                    "Received error response from Sendgrid, status code: {}, body: {}",
                    response.statusCode,
                    response.body
                )
                return if (response.statusCode == 404)
                    null
                else
                    emptyList()
            }
            val suppressionList: SuppressionList = json.decodeFromString(response.body)
            suppressionList
                .suppressions
                .map {
                    SuppressionGroup(
                        id = it.id,
                        name = it.name,
                        suppressed = it.suppressed,
                    )
                }
        } catch (e: Exception) {
            logger.error("Failed to un-list contacts", e)
            null
        }
    }

    // https://docs.sendgrid.com/api-reference/suppressions-suppressions/add-suppressions-to-a-suppression-group
    override suspend fun upsertIntoSuppressionGroup(
        userEmail: String,
        suppressionGroupId: Long,
    ): Boolean? {

        @Serializable
        data class Emails(
            @SerialName("recipient_emails") val recipientEmails: List<String>
        )

        val sendGrid = SendGrid(sendGridConfig.apiKey)
        return try {
            val jsonBody = json.encodeToString(Emails(listOf(userEmail)))
            val request = Request().apply {
                method = Method.POST
                endpoint = "/asm/groups/${suppressionGroupId}/suppressions"
                body = jsonBody
            }
            val response = withContext(Dispatchers.IO) {
                sendGrid.api(request)
            }
            if (response.statusCode !in 200..299) {
                logger.error(
                    "Received error response from Sendgrid, status code: {}, body: {}",
                    response.statusCode,
                    response.body
                )
                return if (response.statusCode == 404)
                    null
                else
                    false
            }
            val responseEmails: Emails = json.decodeFromString(response.body)
            responseEmails.recipientEmails.contains(userEmail)
        } catch (e: Exception) {
            logger.error("Failed to upsert to suppression group", e)
            false
        }
    }

    // https://docs.sendgrid.com/api-reference/suppressions-suppressions/delete-a-suppression-from-a-suppression-group
    override suspend fun removeFromSuppressionGroup(
        userEmail: String,
        suppressionGroupId: Long,
    ): Boolean? {

        val sendGrid = SendGrid(sendGridConfig.apiKey)
        return try {
            val request = Request().apply {
                method = Method.DELETE
                endpoint = "/asm/groups/${suppressionGroupId}/suppressions/${userEmail}"
            }
            val response = withContext(Dispatchers.IO) {
                sendGrid.api(request)
            }
            if (response.statusCode !in 200..299) {
                logger.error(
                    "Received error response from Sendgrid, status code: {}, body: {}",
                    response.statusCode,
                    response.body
                )
                return if (response.statusCode == 404)
                    null
                else
                    false
            }
            response.statusCode in 200..299
        } catch (e: Exception) {
            logger.error("Failed to remove from suppression group", e)
            false
        }
    }
}