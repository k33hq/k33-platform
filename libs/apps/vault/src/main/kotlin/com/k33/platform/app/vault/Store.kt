package com.k33.platform.app.vault

import com.k33.platform.user.UserId
import com.k33.platform.user.users
import io.firestore4k.typed.div
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.Serializable

data class AppId(val value: String) {
    override fun toString(): String = value
}

val VAULT_APP = AppId("vault")

@Serializable
data class VaultApp(
    val vaultAccountId: String,
    val currency: String = "USD",
)

val apps = users.subCollection<VaultApp, AppId>("apps")

fun UserId.inVaultAppContext() = users / this / apps / VAULT_APP