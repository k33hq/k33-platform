package com.k33.platform.app.vault

import com.k33.platform.app.vault.VaultUserService.validate
import com.k33.platform.app.vault.stripe.StripeService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VaultUserServiceTest : StringSpec({
    "validate user's stripe customer details" {
        StripeService
            .getCustomerDetails(email = "test@k33.com")
            .validate() shouldBe emptyList()
    }
})
