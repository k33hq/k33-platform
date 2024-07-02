package com.k33.platform.app.vault

import com.k33.platform.utils.logging.getLogger
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import com.slack.api.model.kotlin_extension.block.withBlocks
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object SlackCommandHandler {

    private val logger by getLogger()

    private const val VALID_EMAIL_ADDRESS_REGEX = "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}"

    internal val help = Regex("^help$", RegexOption.IGNORE_CASE)
    internal val info = Regex("^info (?<email>$VALID_EMAIL_ADDRESS_REGEX)$", RegexOption.IGNORE_CASE)
    internal val register = Regex(
        "^register (?<email>$VALID_EMAIL_ADDRESS_REGEX) (?<vaultAccountId>[0-9]+) (?<currency>[A-Z]{3})$",
        RegexOption.IGNORE_CASE
    )

    private val vaultAdminSlackChannelId by lazy { System.getenv("SLACK_VAULT_ADMIN_CHANNEL_ID") }

    suspend fun handleVaultCommand(payload: SlashCommandPayload): List<LayoutBlock> {
        logger.info("{}", payload)
        if (payload.channelId != vaultAdminSlackChannelId) {
            return withBlocks {
                section {
                    markdownText("Access restricted to #vault")
                }
            }
        }
        return when {
            payload.text.isNullOrBlank() -> usage()

            help.containsMatchIn(payload.text) -> usage()

            info.containsMatchIn(payload.text) -> {
                val (email) = info.matchEntire(payload.text)!!.destructured
                val vaultUserStatus = VaultService.getUserStatus(
                    email = email,
                )
                withBlocks {
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
                val vaultUserStatus = VaultService.register(
                    email = email,
                    vaultAccountId = vaultAccountId,
                    currency = currency.uppercase(),
                )
                withBlocks {
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

            else -> usage()
        }
    }

    fun LayoutBlockDsl.renderVaultUserStatus(
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
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                markdownText("At ${ZonedDateTime.now(ZoneOffset.UTC).format(formatter)} (UTC)")
            }
        }
        divider()
    }

    private fun usage(): List<LayoutBlock> {
        return withBlocks {
            section {
                markdownText(
                    """
                    *Usage*
                    ```
                    /vault info <email>
                    ```
                    e.g. `/vault info test@k33.com`
                    
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
}

