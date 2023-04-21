package com.k33.platform.payment

import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.payment.stripe.AlreadySubscribed
import com.k33.platform.payment.stripe.BadRequest
import com.k33.platform.payment.stripe.NotFound
import com.k33.platform.payment.stripe.PaymentServiceError
import com.k33.platform.payment.stripe.StripeClient
import com.k33.platform.user.UserId
import com.k33.platform.utils.logging.logWithMDC
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun Application.module() {

    routing {
        authenticate("esp-v2-header") {
            route("/payment") {
                route("/subscribed-products") {
                    get {
                        val userId = UserId(call.principal<UserInfo>()!!.userId)
                        logWithMDC("userId" to userId.value) {
                            try {
                                val userEmail = call.principal<UserInfo>()!!.email
                                val subscribedProducts = StripeClient.getCurrentSubscribedProducts(
                                    customerEmail = userEmail,
                                )
                                if (subscribedProducts.isNullOrEmpty()) {
                                    call.respond(HttpStatusCode.NotFound)
                                } else {
                                    call.respond(SubscribedProducts(subscribedProducts = subscribedProducts))
                                }
                            } catch (e: PaymentServiceError) {
                                call.application.log.error("Payment service error in fetching subscribed products", e)
                                call.respond(e.httpStatusCode, e.message)
                            } catch (e: Exception) {
                                call.application.log.error("Exception in fetching subscribed products", e)
                                call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                    }
                    get("{product-id}") {
                        val userId = UserId(call.principal<UserInfo>()!!.userId)
                        logWithMDC("userId" to userId.value) {
                            val productId: String = call.parameters["product-id"]
                                ?: throw BadRequest("Path param: product-id is mandatory")
                            try {
                                val userEmail = call.principal<UserInfo>()!!.email
                                val status = StripeClient.getSubscriptionStatus(
                                    customerEmail = userEmail,
                                    productId = productId,
                                )
                                if (status == null) {
                                    call.respond(HttpStatusCode.NotFound)
                                } else {
                                    call.respond(
                                        SubscribedProduct(
                                            productId = productId,
                                            status = status,
                                        )
                                    )
                                }
                            } catch (e: PaymentServiceError) {
                                call.application.log.error("Payment service error in fetching subscribed products", e)
                                call.respond(e.httpStatusCode, e.message)
                            } catch (e: Exception) {
                                call.application.log.error("Exception in fetching subscribed products", e)
                                call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                    }
                }
                suspend fun PipelineContext<Unit, ApplicationCall>.createCheckoutSession() {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        try {
                            val userEmail = call.principal<UserInfo>()!!.email
                            val request = call.receive<CheckoutSessionRequest>()
                            val checkoutSession = StripeClient.createOrFetchCheckoutSession(
                                customerEmail = userEmail,
                                priceId = request.priceId,
                                successUrl = request.successUrl,
                                cancelUrl = request.cancelUrl,
                            )
                            call.respond(
                                CheckoutSessionResponse(
                                    url = checkoutSession.url,
                                    expiresAt = checkoutSession.expiresAt,
                                    priceId = checkoutSession.priceId,
                                    successUrl = checkoutSession.successUrl,
                                    cancelUrl = checkoutSession.cancelUrl,
                                )
                            )
                        } catch (e: AlreadySubscribed) {
                            call.respond(e.httpStatusCode, e.message)
                        } catch (e: PaymentServiceError) {
                            call.application.log.error("Payment service error in create/fetch checkout session", e)
                            call.respond(e.httpStatusCode, e.message)
                        } catch (e: Exception) {
                            call.application.log.error("Exception in create/fetch checkout session", e)
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                }
                post("checkout-session") {
                    createCheckoutSession()
                }
                post("checkout-sessions") {
                    createCheckoutSession()
                }
                suspend fun PipelineContext<Unit, ApplicationCall>.createCustomerPortal() {
                    val userId = UserId(call.principal<UserInfo>()!!.userId)
                    logWithMDC("userId" to userId.value) {
                        try {
                            val userEmail = call.principal<UserInfo>()!!.email
                            val request = call.receive<CustomerPortalSessionRequest>()
                            val customerPortalSession = StripeClient.createCustomerPortalSession(
                                customerEmail = userEmail,
                                returnUrl = request.returnUrl,
                            )
                            call.respond(
                                CustomerPortalSessionResponse(
                                    url = customerPortalSession.url,
                                    returnUrl = customerPortalSession.returnUrl,
                                )
                            )
                        } catch (e: NotFound) {
                            call.respond(e.httpStatusCode, e.message)
                        } catch (e: PaymentServiceError) {
                            call.application.log.error("Payment service error in create customer portal session", e)
                            call.respond(e.httpStatusCode, e.message)
                        } catch (e: Exception) {
                            call.application.log.error("Exception in create customer portal session", e)
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                }
                // deprecated
                post("customer-portal-session") {
                    createCustomerPortal()
                }
                post("customer-portal-sessions") {
                    createCustomerPortal()
                }
            }
        }
    }
}


@Serializable
data class SubscribedProducts(
    val subscribedProducts: Collection<String>
)

@Serializable
data class SubscribedProduct(
    @SerialName("product_id") val productId: String,
    val status: StripeClient.ProductSubscriptionStatus
)

@Serializable
data class CheckoutSessionRequest(
    @SerialName("price_id") val priceId: String,
    @SerialName("success_url") val successUrl: String,
    @SerialName("cancel_url") val cancelUrl: String,
)

@Serializable
data class CheckoutSessionResponse(
    val url: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("price_id") val priceId: String,
    @SerialName("success_url") val successUrl: String,
    @SerialName("cancel_url") val cancelUrl: String,
)

@Serializable
data class CustomerPortalSessionRequest(
    @SerialName("return_url") val returnUrl: String,
)

@Serializable
data class CustomerPortalSessionResponse(
    val url: String,
    @SerialName("return_url") val returnUrl: String,
)