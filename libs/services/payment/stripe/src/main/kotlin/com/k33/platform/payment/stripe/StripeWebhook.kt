package com.k33.platform.payment.stripe

import com.google.gson.JsonSyntaxException
import com.k33.platform.email.Email
import com.k33.platform.email.EmailTemplateConfig
import com.k33.platform.email.MailTemplate
import com.k33.platform.email.getEmailService
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getMarker
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
                                        suspend fun sendWelcomeEmail() {
                                            launch {
                                                emailService.sendEmail(
                                                    from = Email(
                                                        address = researchProWelcomeEmail.from.email,
                                                        label = researchProWelcomeEmail.from.label,
                                                    ),
                                                    toList = listOf(Email(customerEmail)),
                                                    mail = MailTemplate(researchProWelcomeEmail.sendgridTemplateId)
                                                )
                                            }
                                        }

                                        suspend fun addToProContactList() {
                                            launch {
                                                emailService.upsertMarketingContacts(
                                                    contactEmails = listOf(customerEmail),
                                                    contactListIds = listOf(contactListId),
                                                )
                                            }
                                        }

                                        fun notifySlack(message: String) {
                                            call.application.log.info(
                                                NotifySlack.NOTIFY_SLACK_RESEARCH.getMarker(),
                                                message,
                                            )
                                        }

                                        suspend fun trialSubscriberEvent() {
                                            notifySlack("$customerEmail has become a trial subscriber of K33 Research Pro")
                                            addToProContactList()
                                            sendWelcomeEmail()
                                        }

                                        suspend fun activeSubscriberEvent() {
                                            notifySlack("$customerEmail has become an active subscriber of K33 Research Pro")
                                            addToProContactList()
                                            sendWelcomeEmail()
                                        }

                                        val status = Status.valueOf(subscription.status)
                                        val previousStatus = event.data.previousAttributes?.get("status")
                                        call.application.log.info("event.type: ${event.type}, status: $status, previousStatus: $previousStatus")

                                        when (event.type) {
                                            "customer.subscription.created" -> {
                                                when (status) {
                                                    Status.trialing -> {
                                                        // started a trial subscription
                                                        trialSubscriberEvent()
                                                    }

                                                    Status.incomplete -> {
                                                        // failed to subscribe
                                                        call.application.log.warn(
                                                            NotifySlack.NOTIFY_SLACK_RESEARCH.getMarker(),
                                                            "$customerEmail failed to subscribe to K33 Research Pro",
                                                        )
                                                    }

                                                    Status.active -> {
                                                        // started an active (paid) subscription
                                                        activeSubscriberEvent()
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

                                                val subscriptionTrialToActive = previousStatus == "trialing"
                                                        && subscription.status == "active"
                                                val subscriptionToActive =
                                                    !setOf("active", "trialing").contains(previousStatus)
                                                            && subscription.status == "active"
                                                val subscriptionToTrial =
                                                    !setOf("active", "trialing").contains(previousStatus)
                                                            && subscription.status == "trialing"

                                                when {
                                                    subscriptionToBeCanceled -> {
                                                        // subscription is set to be cancelled at the end of billing period
                                                        notifySlack("$customerEmail has scheduled to unsubscribe from K33 Research Pro at the end of billing period")
                                                    }

                                                    subscriptionTrialToActive -> {
                                                        // changed trial subscription to active (paid) subscription
                                                        notifySlack("$customerEmail upgraded from trial to active subscriber of K33 Research Pro")
                                                    }

                                                    subscriptionToActive -> {
                                                        // updated to an active (paid) subscription
                                                        notifySlack("$customerEmail changed from $previousStatus to active subscriber of K33 Research Pro")
                                                        activeSubscriberEvent()
                                                    }

                                                    subscriptionToTrial -> {
                                                        // updated to a trial subscription
                                                        notifySlack("$customerEmail changed from $previousStatus to trial subscriber of K33 Research Pro")
                                                        trialSubscriberEvent()
                                                    }

                                                    else -> {
                                                        call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.updated")
                                                    }
                                                }
                                            }

                                            "customer.subscription.deleted" -> {
                                                if (status == Status.canceled) {
                                                    // subscription is cancelled
                                                    notifySlack("$customerEmail has unsubscribed from K33 Research Pro")
                                                    launch {
                                                        emailService.unlistMarketingContacts(
                                                            contactEmails = listOf(customerEmail),
                                                            contactListId = contactListId,
                                                        )
                                                    }
                                                } else {
                                                    call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.deleted")
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