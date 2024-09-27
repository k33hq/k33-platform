package com.k33.platform.app.vault

import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.slack.ChannelId
import com.k33.platform.utils.slack.SlackClient
import com.k33.platform.utils.slack.app
import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.app_backend.views.payload.ViewSubmissionPayload
import com.slack.api.bolt.response.Response
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewClose
import com.slack.api.model.view.Views.viewSubmit
import com.slack.api.model.view.Views.viewTitle
import kotlinx.coroutines.runBlocking
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object SlackRequestHandler {

    private val logger by getLogger()

    private const val VALID_EMAIL_ADDRESS_REGEX = "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}"

    internal val info = Regex("^info (?<email>$VALID_EMAIL_ADDRESS_REGEX)$", RegexOption.IGNORE_CASE)
    internal val register = Regex(
        "^register (?<email>$VALID_EMAIL_ADDRESS_REGEX) (?<vaultAccountId>[0-9]+) (?<currency>[A-Z]{3})$",
        RegexOption.IGNORE_CASE
    )

    private val vaultAdminSlackChannelId by lazy { System.getenv("SLACK_VAULT_ADMIN_CHANNEL_ID") }

    private const val REGISTER_VAULT_USER_CALLBACK_ID = "/vault/user/register"

    private val defaultCurrencyOptions = mapOf(
        "USD" to "🇺🇸 USD",
        "NOK" to "🇳🇴 NOK",
        "EUR" to "💶 EUR",
        "GBP" to "💷 GBP",
        "SEK" to "🇸🇪 SEK",
        "DKK" to "🇩🇰 DKK",
        "CHF" to "🇨🇭 CHF",
    )

    fun registerHandler() {
        app
            .command("/vault") { request, ctx ->
                fun ackWithBlocks(
                    builder: LayoutBlockDsl.() -> Unit,
                ) = ctx.ack(
                    SlashCommandResponse
                        .builder()
                        .blocks(withBlocks(builder))
                        // default: ephemeral - visible to user only
                        // in_channel - visible to all in the channel
                        .responseType("in_channel")
                        .build()
                )

                val payload = request.payload
                logger.info("{}", payload)
                if (payload.channelId != vaultAdminSlackChannelId) {
                    ackWithBlocks {
                        section {
                            markdownText("Access restricted to #vault")
                        }
                    }
                } else {
                    when {
                        payload.text.isNullOrBlank() -> ackWithBlocks { usage() }

                        payload.text == "help" -> ackWithBlocks { usage() }

                        payload.text == "register" -> {
                            val viewsOpenResponse = ctx.client().viewsOpen {
                                it.triggerId(ctx.triggerId)
                                    .view(buildRegisterVaultUserView())
                            }
                            if (viewsOpenResponse.isOk) {
                                ctx.ack()
                            } else {
                                Response.builder().statusCode(500).body(viewsOpenResponse.error).build()
                            }
                        }

                        info.containsMatchIn(payload.text) -> {
                            val (email) = info.matchEntire(payload.text)!!.destructured
                            val vaultUserStatus = runBlocking {
                                VaultUserService.getUserStatus(
                                    email = email,
                                )
                            }
                            ackWithBlocks {
                                header {
                                    text("Vault User Info")
                                }
                                renderVaultUserStatus(
                                    email = email,
                                    vaultUserStatus = vaultUserStatus,
                                    adminUser = payload.userName,
                                )
                            }
                        }

                        register.containsMatchIn(payload.text) -> {
                            val (email, vaultAccountId, currency) = register.matchEntire(payload.text)!!.destructured
                            val vaultUserStatus = runBlocking {
                                VaultUserService.registerUser(
                                    email = email,
                                    vaultAccountId = vaultAccountId,
                                    currency = currency.uppercase(),
                                )
                            }
                            ackWithBlocks {
                                header {
                                    text("Vault User Registered")
                                }
                                renderVaultUserStatus(
                                    email = email,
                                    vaultUserStatus = vaultUserStatus,
                                    adminUser = payload.userName,
                                )
                            }
                        }

                        else -> ackWithBlocks { usage() }
                    }
                }
            }
            .viewSubmission(REGISTER_VAULT_USER_CALLBACK_ID) { request, ctx ->
                runBlocking {
                    handleViewSubmission(request.payload)
                }
                ctx.ack()
            }
    }

    private fun LayoutBlockDsl.renderVaultUserStatus(
        email: String,
        vaultUserStatus: VaultUserStatus,
        adminUser: String,
    ) {
        section {
            markdownText(
                buildString {
                    appendLine("*Email*:  $email")
                    appendLine()
                    if (vaultUserStatus.platformRegistered) {
                        appendLine("✅ *Platform*:  `Registered`")
                    } else {
                        appendLine("⚠️ *Platform*:  `Not registered`")
                    }
                    appendLine()
                    if (vaultUserStatus.vaultAccountId == null) {
                        appendLine("⚠️ *Vault App*:  `Not Registered`")
                    } else {
                        appendLine("✅ *Vault Account ID*:  `${vaultUserStatus.vaultAccountId}`")
                    }
                    appendLine()
                    if (vaultUserStatus.stripeErrors.isEmpty()) {
                        appendLine("✅ *Stripe*:  `Registered`")
                    } else {
                        appendLine("⚠️ *Stripe*:  `Not Registered`")
                    }
                    appendLine()
                    appendLine("*Currency*:  `${vaultUserStatus.currency ?: "-"}`")
                    appendLine()
                }
            )
        }
        if (vaultUserStatus.stripeErrors.isNotEmpty()) {
            section {
                markdownText("*Stripe registration errors*")
                fields {
                    vaultUserStatus.stripeErrors.forEach {
                        markdownText(it)
                    }
                }
            }
        }
        context {
            elements {
                markdownText("Requested by @$adminUser")
                markdownText("At ${utcDateTimeNow()} (UTC)")
            }
        }
        divider()
    }

    enum class Fields {
        EMAIL,
        VAULT_ACCOUNT_ID,
        DEFAULT_CURRENCY,
    }

    private fun buildRegisterVaultUserView(): View = view { thisView ->
        thisView
            .callbackId(REGISTER_VAULT_USER_CALLBACK_ID)
            .type("modal")
            .notifyOnClose(false)
            .title(viewTitle { it.type("plain_text").text("Register Vault User").emoji(false) })
            .submit(viewSubmit { it.type("plain_text").text("Submit").emoji(false) })
            .close(viewClose { it.type("plain_text").text("Cancel").emoji(false) })
            .blocks {
                input {
                    blockId(Fields.EMAIL.name)
                    label("User email", false)
                    emailTextInput {
                        actionId(Fields.EMAIL.name)
                    }
                }
                input {
                    blockId(Fields.VAULT_ACCOUNT_ID.name)
                    label("Vault Account ID", false)
                    numberInput {
                        actionId(Fields.VAULT_ACCOUNT_ID.name)
                    }
                }
                input {
                    blockId(Fields.DEFAULT_CURRENCY.name)
                    label("Default Currency")
                    staticSelect {
                        actionId(Fields.DEFAULT_CURRENCY.name)
                        placeholder("Default Currency", false)
                        options {
                            defaultCurrencyOptions.forEach { (currencyCode, currencyLabel) ->
                                option {
                                    value(currencyCode)
                                    plainText(currencyLabel, emoji = true)
                                }
                            }
                        }
                    }
                }
            }
    }

    private suspend fun handleViewSubmission(
        payload: ViewSubmissionPayload,
    ) {
        val fields = payload.view.state.values
        val email = fields[Fields.EMAIL.name]?.get(Fields.EMAIL.name)?.value ?: run {
            logger.warn("Missing email field")
            return
        }
        if (!email.matches(Regex("^$VALID_EMAIL_ADDRESS_REGEX$", RegexOption.IGNORE_CASE))) {
            logger.warn("Invalid email address")
            return
        }
        val vaultAccountId = fields[Fields.VAULT_ACCOUNT_ID.name]?.get(Fields.VAULT_ACCOUNT_ID.name)?.value ?: run {
            logger.warn("Missing vault account id")
            return
        }
        val currency = fields[Fields.DEFAULT_CURRENCY.name]?.get(Fields.DEFAULT_CURRENCY.name)?.selectedOption?.value ?: "USD"
        val vaultUserStatus = VaultUserService.registerUser(
            email = email,
            vaultAccountId = vaultAccountId,
            currency = currency.uppercase(),
        )
        val adminUser = payload.user.username

        SlackClient.sendRichMessage(
            channel = ChannelId(vaultAdminSlackChannelId),
            altPlainTextMessage =
            "Vault User Registered: $email ($vaultAccountId, $currency) by @$adminUser at ${utcDateTimeNow()} (UTC)",
        ) {
            header {
                text("Vault User Registered")
            }
            renderVaultUserStatus(
                email = email,
                vaultUserStatus = vaultUserStatus,
                adminUser = adminUser,
            )
        }
    }

    private fun utcDateTimeNow(): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        return ZonedDateTime.now(ZoneOffset.UTC).format(formatter)
    }

    private fun LayoutBlockDsl.usage() {
        section {
            markdownText(
                """
                *Usage*
                ```
                /vault info <email>
                ```
                e.g. `/vault info test@k33.com`
                
                ```
                /vault register
                ```
                
                ```
                /vault register <email> <vaultAccountId> <currency>
                ```
                e.g. `/vault register test@k33.com 76 USD`
                
                ```
                /vault help
                ```
                """.trimIndent()
            )
        }
    }
}

