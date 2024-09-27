package com.k33.platform.app.vault

import com.k33.platform.app.vault.stripe.StripeService
import com.k33.platform.identity.auth.gcp.FirebaseAuthService
import com.k33.platform.user.UserId
import com.k33.platform.utils.RestApiException
import com.k33.platform.utils.logging.getLogger
import com.stripe.model.Address
import io.firestore4k.typed.FirestoreClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import java.util.Locale

object VaultUserService {

    private val firestoreClient by lazy { FirestoreClient() }

    suspend fun UserId.getVaultApp() = firestoreClient.get(inVaultAppContext())
        ?: throw RestApiException(
            status = HttpStatusCode.Forbidden,
            code = RestApiErrorCodes.NOT_REGISTERED.name,
            message = "Not registered to K33 Vault service"
        )

    suspend fun UserId.register(
        vaultApp: VaultApp,
    ) {
        firestoreClient.put(inVaultAppContext(), vaultApp)
    }

    private suspend fun UserId.deregister() {
        firestoreClient.delete(inVaultAppContext())
    }

    suspend fun registerUser(
        email: String,
        vaultAccountId: String,
        currency: String,
    ): VaultUserStatus {
        val stripeErrors = StripeService.getCustomerDetails(email = email).validate()
        val userId = FirebaseAuthService.findUserIdOrNull(email)
            ?.let(::UserId)
            ?: return VaultUserStatus(
                platformRegistered = false,
                vaultAccountId = null,
                currency = null,
                stripeErrors = stripeErrors,
            )
        userId.register(
            VaultApp(
                vaultAccountId = vaultAccountId,
                currency = currency,
            )
        )
        return VaultUserStatus(
            platformRegistered = true,
            vaultAccountId = vaultAccountId,
            currency = currency,
            stripeErrors = stripeErrors,
        )
    }

    suspend fun deregister(
        email: String,
    ): VaultUserStatus {
        val stripeErrors = StripeService.getCustomerDetails(email = email).validate()
        val userId = FirebaseAuthService.findUserIdOrNull(email)
            ?.let(::UserId)
            ?: return VaultUserStatus(
                platformRegistered = false,
                vaultAccountId = null,
                currency = null,
                stripeErrors = stripeErrors,
            )
        userId.deregister()
        return VaultUserStatus(
            platformRegistered = true,
            vaultAccountId = null,
            currency = null,
            stripeErrors = stripeErrors,
        )
    }

    suspend fun UserId.getVaultAppSettings(): VaultAppSettings {
        val vaultApp = getVaultApp()
        return VaultAppSettings(
            currency = vaultApp.currency,
        )
    }

    suspend fun UserId.updateVaultAppSettings(settings: VaultAppSettings) {
        val vaultApp = getVaultApp()
        val updatedVaultApp = vaultApp.copy(
            currency = settings.currency,
        )
        firestoreClient.put(inVaultAppContext(), updatedVaultApp)
    }

    suspend fun getUserStatus(
        email: String,
    ): VaultUserStatus {
        val stripeErrors = StripeService.getCustomerDetails(email = email).validate()
        val userId = FirebaseAuthService.findUserIdOrNull(email)
            ?.let(::UserId)
            ?: return VaultUserStatus(
                platformRegistered = false,
                vaultAccountId = null,
                currency = null,
                stripeErrors = stripeErrors,
            )
        val vaultApp = firestoreClient.get(userId.inVaultAppContext())
            ?: return VaultUserStatus(
                platformRegistered = true,
                vaultAccountId = null,
                currency = null,
                stripeErrors = stripeErrors,
            )
        return VaultUserStatus(
            platformRegistered = true,
            vaultAccountId = vaultApp.vaultAccountId,
            currency = vaultApp.currency,
            stripeErrors = stripeErrors,
        )
    }

    internal fun List<StripeService.CustomerDetails>.validate(): List<String> = buildList {
        if (this@validate.isEmpty()) {
            add("No stripe users found with this email")
        } else if (this@validate.size > 1) {
            add("Multiple stripe users found with this email")
        } else {
            this@validate.single().address.validationErrors().forEach {
                add("Missing address field: $it")
            }
        }
    }

    fun Address.validationErrors(): List<String> = buildList {
        if (line1.isNullOrEmpty()) {
            add("line1")
        }
        if (postalCode.isNullOrEmpty()) {
            add("postalCode")
        }
        if (city.isNullOrEmpty()) {
            add("city")
        }
        if (country.isNullOrEmpty()) {
            add("country")
        } else if (Locale.of("", country).displayName.isNullOrEmpty()) {
            add("country locale")
        }
    }
}