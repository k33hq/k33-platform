package com.k33.platform.tests

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextUInt

private const val SETTINGS_URL = "https://dev.k33.com/research/settings"

class PaymentTest : BehaviorSpec({

    val priceId = System.getenv("STRIPE_PRICE_ID_RESEARCH_PRO")!!
    val productId = System.getenv("STRIPE_PRODUCT_ID_RESEARCH_PRO")!!

    suspend fun getSubscribedProduct(
        email: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "payment/subscribed-products/$productId")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
                expectSuccess = false
            }
        }
    }

    suspend fun getSubscribedProducts(
        email: String,
    ): HttpResponse {
        return apiClient.get {
            url(path = "payment/subscribed-products")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
                expectSuccess = false
            }
        }
    }

    suspend fun createOrFetchCheckoutSession(
        email: String,
    ): HttpResponse {
        return apiClient.post {
            url(path = "payment/checkout-sessions")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
                expectSuccess = false
            }
            contentType(ContentType.Application.Json)
            setBody(
                CheckoutSessionRequest(priceId)
            )
        }
    }

    suspend fun createCustomerPortalSession(
        email: String,
    ): HttpResponse {
        return apiClient.post {
            url(path = "payment/customer-portal-sessions")
            headers {
                appendEndpointsApiUserInfoHeader(email = email)
                expectSuccess = false
            }
            contentType(ContentType.Application.Json)
            setBody(
                CustomerPortalSessionRequest()
            )
        }
    }

    given("a user does not exist in stripe") {
        val email = "delete-me-${Random.nextUInt()}@k33.com"
        `when`("GET /payment/subscribed-products/{productId}") {
            then("response is 404 NOT FOUND") {
                getSubscribedProduct(email = email).status shouldBe HttpStatusCode.NotFound
            }
        }
        `when`("POST /payment/customer-portal-sessions") {
            then("response is 404 NOT FOUND") {
                createCustomerPortalSession(email = email).status shouldBe HttpStatusCode.NotFound
            }
        }
        `when`("POST /payment/checkout-sessions") {
            val response = createOrFetchCheckoutSession(email = email)
            then("response is checkout session url with expiresAt") {
                response.status shouldBe HttpStatusCode.OK
                val checkoutSession = response.body<CheckoutSession>()
                checkoutSession.priceId shouldBe priceId
                checkoutSession.successUrl shouldBe SETTINGS_URL
                checkoutSession.cancelUrl shouldBe SETTINGS_URL
            }
            and("again POST /payment/checkout-sessions") {
                val secondResponse = createOrFetchCheckoutSession(email = email)
                then("response should be same") {
                    secondResponse.status shouldBe HttpStatusCode.OK
                    secondResponse.body<CheckoutSession>().url shouldBe response.body<CheckoutSession>().url
                    secondResponse.body<CheckoutSession>().priceId shouldBe response.body<CheckoutSession>().priceId
                    secondResponse.body<CheckoutSession>().successUrl shouldBe response.body<CheckoutSession>().successUrl
                    secondResponse.body<CheckoutSession>().cancelUrl shouldBe response.body<CheckoutSession>().cancelUrl
                }
            }
        }
    }

    given("user exists in stripe") {
        and("a user is NOT subscribed in stripe") {
            val email = "test.not_subscribed@k33.com"
            `when`("GET /payment/subscribed-products/{productId}") {
                then("response is 404 NOT FOUND") {
                    getSubscribedProduct(email = email).status shouldBe HttpStatusCode.NotFound
                }
            }
            `when`("POST /payment/checkout-sessions") {
                val response = createOrFetchCheckoutSession(email = email)
                then("response is checkout session url with expiresAt") {
                    response.status shouldBe HttpStatusCode.OK
                    val checkoutSession = response.body<CheckoutSession>()
                    checkoutSession.priceId shouldBe priceId
                    checkoutSession.successUrl shouldBe SETTINGS_URL
                    checkoutSession.cancelUrl shouldBe SETTINGS_URL
                }
                and("again POST /payment/checkout-sessions") {
                    val secondResponse = createOrFetchCheckoutSession(email = email)
                    then("response should be same") {
                        secondResponse.status shouldBe HttpStatusCode.OK
                        secondResponse.body<CheckoutSession>() shouldBe response.body<CheckoutSession>()
                    }
                }
            }
            `when`("POST /payment/customer-portal-sessions") {
                val response = createCustomerPortalSession(email = email)
                then("response is customer portal session url") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<CustomerPortalSession>().returnUrl shouldBe SETTINGS_URL
                }
            }
        }
        and("a user is subscribed in stripe") {
            val email = "test.subscribed@k33.com"
            `when`("GET /payment/subscribed-products/{productId}") {
                val response = getSubscribedProduct(email = email)
                then("response is subscribed product with active status") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<SubscribedProduct>() shouldBe SubscribedProduct(
                        productId = productId,
                        status = ProductSubscriptionStatus.active,
                        priceId = priceId,
                    )
                }
            }
            `when`("POST /payment/checkout-sessions") {
                then("response is 409 CONFLICT") {
                    createOrFetchCheckoutSession(email = email).status shouldBe HttpStatusCode.Conflict
                }
            }
            `when`("POST /payment/customer-portal-sessions") {
                val response = createCustomerPortalSession(email = email)
                then("response is customer portal session url") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<CustomerPortalSession>().returnUrl shouldBe SETTINGS_URL
                }
            }
        }
        and("a user is unsubscribed in stripe") {
            val email = "test.unsubscribed@k33.com"
            `when`("GET /payment/subscribed-products/{productId}") {
                val response = getSubscribedProduct(email = email)
                then("response is Subscribed Product with ended status") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<SubscribedProduct>() shouldBe SubscribedProduct(
                        productId = productId,
                        status = ProductSubscriptionStatus.ended,
                        priceId = priceId,
                    )
                }
            }
            `when`("POST /payment/checkout-sessions") {
                val response = createOrFetchCheckoutSession(email = email)
                then("response is checkout session url with expiresAt") {
                    response.status shouldBe HttpStatusCode.OK
                    val checkoutSession = response.body<CheckoutSession>()
                    checkoutSession.priceId shouldBe priceId
                    checkoutSession.successUrl shouldBe SETTINGS_URL
                    checkoutSession.cancelUrl shouldBe SETTINGS_URL
                }
                and("again POST /payment/checkout-sessions") {
                    val secondResponse = createOrFetchCheckoutSession(email = email)
                    then("response should be same") {
                        secondResponse.status shouldBe HttpStatusCode.OK
                        secondResponse.body<CheckoutSession>() shouldBe response.body<CheckoutSession>()
                    }
                }
            }
            `when`("POST /payment/customer-portal-sessions") {
                val response = createCustomerPortalSession(email = email)
                then("response is customer portal session url") {
                    response.status shouldBe HttpStatusCode.OK
                    response.body<CustomerPortalSession>().returnUrl shouldBe SETTINGS_URL
                }
            }
        }
    }

})

@Serializable
data class SubscribedProduct(
    val productId: String,
    val status: ProductSubscriptionStatus,
    val priceId: String,
)

@Suppress("EnumEntryName")
enum class ProductSubscriptionStatus {
    active,
    ended,
}

@Serializable
data class SubscribedProducts(
    val subscribedProducts: Collection<String>
)

@Serializable
data class CheckoutSessionRequest(
    val priceId: String,
    val successUrl: String = SETTINGS_URL,
    val cancelUrl: String = SETTINGS_URL,
)

@Serializable
data class CheckoutSession(
    val url: String,
    val expiresAt: String,
    val priceId: String,
    val successUrl: String,
    val cancelUrl: String,
)

@Serializable
data class CustomerPortalSessionRequest(
    val returnUrl: String = SETTINGS_URL,
)

@Serializable
data class CustomerPortalSession(
    val url: String,
    val returnUrl: String = SETTINGS_URL,
)