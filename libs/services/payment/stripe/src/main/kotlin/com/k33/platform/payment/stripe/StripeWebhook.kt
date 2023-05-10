package com.k33.platform.payment.stripe

import com.google.gson.JsonSyntaxException
import com.k33.platform.email.Email
import com.k33.platform.email.EmailTemplateConfig
import com.k33.platform.email.MailTemplate
import com.k33.platform.email.getEmailService
import com.k33.platform.utils.analytics.Log
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.logWithMDC
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

fun Application.module() {

    val product by lazy { System.getenv("STRIPE_PRODUCT_ID_RESEARCH_PRO") }
    val endpointSecret by lazy { System.getenv("STRIPE_WEBHOOK_ENDPOINT_SECRET") }
    val contactListId by lazy { System.getenv("SENDGRID_CONTACT_LIST_ID_K33_RESEARCH_PRO") }
    val researchProWelcomeEmail by loadConfig<EmailTemplateConfig>(
        "researchApp",
        "apps.research.researchProWelcomeEmail"
    )
    val emailService by getEmailService()

    routing {
        route("/webhooks/stripe") {
            post {
                val payload = call.receiveText()
                val sigHeader = call.request.header("Stripe-Signature")
                val event = try {
                    Webhook.constructEvent(
                        payload,
                        sigHeader,
                        endpointSecret,
                    )
                } catch (e: JsonSyntaxException) {
                    call.application.log.error("Failed to parse stripe webhook event", e)
                    throw BadRequestException("Failed to json parse Stripe webhook event json", e)
                }
                // https://stripe.com/docs/error-handling?lang=java#signature-verification-errors
                catch (e: SignatureVerificationException) {
                    call.application.log.error("Failed to verify signature of stripe webhook event", e)
                    throw BadRequestException("Failed to verify signature of stripe webhook event", e)
                }
                logWithMDC("stripe_event_id" to event.id) {
                    val stripeObject = event.dataObjectDeserializer.deserializeUnsafe()
                    // https://stripe.com/docs/api/events/types
                    if (event.type.startsWith("customer.subscription.", ignoreCase = true)) {
                        val subscription = stripeObject as Subscription
                        val customerEmail = StripeClient.getCustomerEmail(subscription.customer)
                        if (customerEmail != null) {
                            logWithMDC("customer_email" to customerEmail) {
                                val products = subscription.items.data.map { subscriptionItem ->
                                    subscriptionItem.plan.product
                                }.toSet()
                                if (products.contains(product)) {
                                    coroutineScope {
                                        suspend fun proSubscriptionEvent() {
                                            launch {
                                                emailService.upsertMarketingContacts(
                                                    contactEmails = listOf(customerEmail),
                                                    contactListIds = listOf(contactListId),
                                                )
                                            }
                                            launch {
                                                emailService.sendEmail(
                                                    from = Email(
                                                        address = researchProWelcomeEmail.from.email,
                                                        label = researchProWelcomeEmail.from.label,
                                                    ),
                                                    toList = listOf(Email(customerEmail)),
                                                    mail = MailTemplate(researchProWelcomeEmail.sendgridTemplateId),
                                                    unsubscribeSettings = researchProWelcomeEmail.unsubscribeSettings,
                                                )
                                            }
                                        }

                                        suspend fun disableProSubscriptionEvent() {
                                            launch {
                                                emailService.unlistMarketingContacts(
                                                    contactEmails = listOf(customerEmail),
                                                    contactListId = contactListId,
                                                )
                                            }
                                        }

                                        fun notifySlack(message: String) {
                                            call.application.log.info(NotifySlack.RESEARCH_EVENTS, message)
                                        }

                                        val status = Status.valueOf(subscription.status)
                                        val previousStatus = event.data.previousAttributes?.get("status")
                                            ?.let { string -> Status.valueOf(string as String) }
                                        call.application.log.info("event.type: ${event.type}, status: $status, previousStatus: $previousStatus")

                                        when (event.type) {
                                            "customer.subscription.created" -> {
                                                when (status) {
                                                    Status.trialing -> {
                                                        // started a trial subscription
                                                        notifySlack("$customerEmail has become a trial subscriber of K33 Research Pro")
                                                        proSubscriptionEvent()
                                                        Log.beginSubscriptionTrial(
                                                            webClientId = subscription.metadata["web-client-id"] ?: UUID.randomUUID().toString(),
                                                            userAnalyticsId = null,
                                                            productId = product,
                                                        )
                                                    }

                                                    Status.incomplete -> {
                                                        // failed to subscribe
                                                        call.application.log.warn(
                                                            NotifySlack.RESEARCH_EVENTS,
                                                            "$customerEmail failed to subscribe to K33 Research Pro",
                                                        )
                                                        disableProSubscriptionEvent()
                                                    }

                                                    Status.active -> {
                                                        // started an active (paid) subscription
                                                        notifySlack("$customerEmail has become an active subscriber of K33 Research Pro")
                                                        proSubscriptionEvent()
                                                        Log.beginSubscription(
                                                            webClientId = subscription.metadata["web-client-id"] ?: UUID.randomUUID().toString(),
                                                            userAnalyticsId = null,
                                                            productId = product,
                                                        )
                                                    }

                                                    else -> {
                                                        call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.created")
                                                    }
                                                }
                                            }

                                            "customer.subscription.updated" -> {

                                                val subscriptionToBeCanceled =
                                                    event.data.previousAttributes?.get("cancel_at_period_end") == false
                                                            && subscription.cancelAtPeriodEnd == true
                                                val subscriptionTrialToActive =
                                                    previousStatus == Status.trialing
                                                            && status == Status.active
                                                val subscriptionIncompleteToExpired = previousStatus ==
                                                    Status.incomplete
                                                            && status == Status.incomplete_expired
                                                val subscriptionNonProToPro =
                                                    previousStatus != null
                                                            && !proStatusSet.contains(previousStatus)
                                                            && proStatusSet.contains(status)
                                                val subscriptionProToBlocked =
                                                    proStatusSet.contains(previousStatus)
                                                            && blockedStatusSet.contains(status)
                                                val subscriptionActive =
                                                    previousStatus == null
                                                            && proStatusSet.contains(status)

                                                when {
                                                    subscriptionToBeCanceled -> {
                                                        // subscription is set to be cancelled at the end of billing period
                                                        notifySlack("$customerEmail has scheduled to unsubscribe from K33 Research Pro at the end of billing period")
                                                    }

                                                    subscriptionTrialToActive -> {
                                                        // changed trial subscription to active (paid) subscription
                                                        notifySlack("$customerEmail upgraded from trial to active subscriber of K33 Research Pro")
                                                        Log.beginSubscription(
                                                            webClientId = subscription.metadata["web-client-id"] ?: UUID.randomUUID().toString(),
                                                            userAnalyticsId = null,
                                                            productId = product,
                                                        )
                                                    }

                                                    subscriptionIncompleteToExpired -> {
                                                        // changed incomplete subscription to incomplete_expired
                                                        notifySlack("$customerEmail is  from Blocked (incomplete) to ex-Pro (incomplete_expired) subscriber of K33 Research Pro because first payment was not done within 23hr past of due date.")
                                                    }

                                                    subscriptionProToBlocked -> {
                                                        notifySlack("$customerEmail changed from Pro ($previousStatus) to Blocked ($status) subscriber of K33 Research Pro")
                                                        disableProSubscriptionEvent()
                                                    }

                                                    subscriptionNonProToPro -> {
                                                        // updated from non-pro to an active (paid) subscription
                                                        notifySlack("$customerEmail changed from non-Pro ($previousStatus) to Pro ($status) subscriber of K33 Research Pro")
                                                        proSubscriptionEvent()
                                                        Log.beginSubscription(
                                                            webClientId = subscription.metadata["web-client-id"] ?: UUID.randomUUID().toString(),
                                                            userAnalyticsId = null,
                                                            productId = product,
                                                        )
                                                    }

                                                    subscriptionActive -> {
                                                        notifySlack("$customerEmail has done recurring payment and continued as Pro ($status) subscriber of K33 Research Pro")
                                                    }

                                                    else -> {
                                                        call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.updated")
                                                    }
                                                }
                                            }

                                            "customer.subscription.deleted" -> {
                                                when (status) {
                                                    Status.canceled -> {
                                                        // subscription is cancelled
                                                        notifySlack("$customerEmail has unsubscribed from K33 Research Pro")
                                                        disableProSubscriptionEvent()
                                                        Log.endSubscription(
                                                            webClientId = subscription.metadata["web-client-id"] ?: UUID.randomUUID().toString(),
                                                            userAnalyticsId = null,
                                                            productId = product,
                                                        )
                                                    }
                                                    else -> call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.deleted")
                                                }
                                            }

                                            else -> {
                                                call.application.log.error("Received unexpected event of type: ${event.type}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}