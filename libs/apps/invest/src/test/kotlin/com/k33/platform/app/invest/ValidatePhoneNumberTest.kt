package com.k33.platform.app.invest

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ValidatePhoneNumberTest: StringSpec({

    "Test valid phone number" {
        PhoneNumber("47", "41234567").validate() shouldBe true
    }
    "Test invalid phone number" {
        PhoneNumber("47", "12345678").validate() shouldBe false
    }

    "Test not a number" {
        PhoneNumber("47", "+4712345678").validate() shouldBe false
    }

    "Test empty number" {
        PhoneNumber("47", "").validate() shouldBe false
    }
})