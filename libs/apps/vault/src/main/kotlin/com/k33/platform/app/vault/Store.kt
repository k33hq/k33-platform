package com.k33.platform.app.vault

import com.k33.platform.user.UserId
import com.k33.platform.user.users
import io.firestore4k.typed.div
import kotlinx.serialization.Serializable

@Serializable
data class VaultApp(
    val vaultAccountId: String,
    val currency: String = "USD",
)

private val apps = users.subCollection<VaultApp, String>("apps")

fun UserId.inVaultAppContext() = users / this / apps / "vault"

val balanceRecords = apps.subCollection<Unit, String>("balanceRecords")
val assets = balanceRecords.subCollection<VaultAssetBalance, String>("assets")

@Serializable
data class VaultAssetBalance(
    val asset: String,
    val total: String? = null,
    val available: String? = null,
    val pending: String? = null,
    val frozen: String? = null,
    val lockedAmount: String? = null,
    val staked: String? = null,
)