package com.k33.platform.app.vault

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

data class CommandSpec(
    val commandName: String,
    val commandRegex: Regex,
    val expectedParsedWords: List<String>,
)

class SlackCommandHandlerTest : BehaviorSpec({
    val info = CommandSpec(
        "info",
        SlackRequestHandler.info,
        expectedParsedWords = listOf("test@k33.com"),
    )
    val register = CommandSpec(
        "register",
        SlackRequestHandler.register,
        expectedParsedWords = listOf("test@k33.com", "76", "USD"),
    )
    val all = listOf(
        info,
        register,
    )
    context("Matching commands with command patterns") {
        forAll(
            // valid info
            row("info test@k33.com", info, listOf(register)),
            // valid register
            row("register test@k33.com 76 USD", register, listOf(info)),
            // invalid command
            row("invalid", null, all),
            // invalid email
            row("info k33.com 76", null, all),
            // invalid account number
            row("register test@k33.com abc USD", null, all),
            // invalid currency
            row("register test@k33.com 76 US8", null, all),
        ) { command, match, unMatchList ->
            given("command = $command") {
                if (match != null) {
                    `when`("$command is matched to '${match.commandName}' command pattern") {
                        val matchResult = match.commandRegex.matchEntire(command)
                        then("matches") {
                            matchResult shouldNotBe null
                        }
                        then("match result has expected parsed words") {
                            matchResult?.destructured?.toList() shouldBe match.expectedParsedWords
                        }
                    }
                }
                unMatchList.forEach { (commandName, commandRegex) ->
                    `when`("$command is matched to '$commandName' command pattern") {
                        val matchResult = commandRegex.matchEntire(command)
                        then("does not match") {
                            matchResult shouldBe null
                        }
                    }
                }
            }
        }
    }
})