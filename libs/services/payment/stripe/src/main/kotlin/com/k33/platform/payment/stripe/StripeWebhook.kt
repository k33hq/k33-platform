package com.k33.platform.payment.stripe

import com.google.gson.JsonSyntaxException
import com.k33.platform.email.Email
import com.k33.platform.email.MailTemplate
import com.k33.platform.email.getEmailService
import com.k33.platform.identity.auth.gcp.FirebaseAuthService
import com.k33.platform.user.UserId
import com.k33.platform.user.UserService.fetchUser
import com.k33.platform.utils.analytics.Log
import com.k33.platform.utils.config.loadConfigEager
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.logWithMDC
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Application.module() {

    val endpointSecret by lazy { System.getenv("STRIPE_WEBHOOK_ENDPOINT_SECRET") }

    val productMap: Map<String, ProductConfig> by lazy {
        loadConfigEager<Map<String, ProductConfig>>(
            "researchApp",
            "apps.research.products",
        ).values.associateBy { it.stripeProduct.id }
    }

    val authService by lazy { FirebaseAuthService }
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
                            logWithMDC("customerEmail" to customerEmail) {
                                coroutineScope {
                                    val userId = try {
                                        authService.findUserId(customerEmail)
                                    } catch (e: Exception) {
                                        null
                                    }?.let(::UserId)
                                    val userAnalyticsId = userId?.fetchUser()?.analyticsId
                                    val productIds = subscription.items.data.map { subscriptionItem ->
                                        subscriptionItem.plan.product
                                    }
                                    for (productId in productIds) {
                                        val product = productMap[productId]
                                        if (product != null) {
                                            suspend fun proSubscriptionEvent(trail: Boolean = false) {
                                                launch {
                                                    emailService.upsertToMarketingContactLists(
                                                        contactEmails = listOf(customerEmail),
                                                        contactListIds = listOf(product.sendgridContactListId),
                                                    )
                                                }
                                                launch {
                                                    val welcomeEmail = if (trail) {
                                                        product.welcomeEmailForTrial
                                                    } else {
                                                        product.welcomeEmail
                                                    }
                                                    if (welcomeEmail != null) {
                                                        emailService.sendEmail(
                                                            from = Email(
                                                                address = welcomeEmail.from.email,
                                                                label = welcomeEmail.from.label,
                                                            ),
                                                            toList = listOf(Email(customerEmail)),
                                                            mail = MailTemplate(welcomeEmail.sendgridTemplateId),
                                                            unsubscribeSettings = welcomeEmail.unsubscribeSettings,
                                                        )
                                                    }
                                                }
                                            }

                                            suspend fun disableProSubscriptionEvent() {
                                                launch {
                                                    emailService.removeFromMarketingContactLists(
                                                        contactEmails = listOf(customerEmail),
                                                        contactListId = product.sendgridContactListId,
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
                                                            notifySlack("$customerEmail has become a trial subscriber of ${product.name}")
                                                            proSubscriptionEvent(trail = true)
                                                            Log.beginSubscriptionTrial(
                                                                webClientId = subscription.metadata["web-client-id"],
                                                                userAnalyticsId = userAnalyticsId,
                                                                productId = productId,
                                                            )
                                                        }

                                                        Status.incomplete -> {
                                                            // failed to subscribe
                                                            call.application.log.warn(
                                                                NotifySlack.RESEARCH_EVENTS,
                                                                "$customerEmail failed to subscribe to ${product.name}",
                                                            )
                                                            disableProSubscriptionEvent()
                                                        }

                                                        Status.active -> {
                                                            // started an active (paid) subscription
                                                            notifySlack("$customerEmail has become an active subscriber of ${product.name}")
                                                            proSubscriptionEvent()
                                                            Log.beginSubscription(
                                                                webClientId = subscription.metadata["web-client-id"],
                                                                userAnalyticsId = userAnalyticsId,
                                                                productId = productId,
                                                            )
                                                        }

                                                        else -> {
                                                            call.application.log.error("Unexpected status: ${status.name} for Stripe event: customer.subscription.created")
                                                        }
                                                    }
                                                }

                                                "customer.subscription.updated" -> {

                                                    val subscriptionTrialToActive =
                                                        previousStatus == Status.trialing
                                                                && status == Status.active
                                                    val subscriptionIncompleteToExpired = previousStatus ==
                                                        Status.incomplete
                                                                && status == Status.incomplete_expired
                                                    val subscriptionNonProToPro =
                                                        previousStatus != null
                                                                && previousStatus.productSubscriptionStatus != StripeClient.ProductSubscriptionStatus.active
                                                                && status.productSubscriptionStatus == StripeClient.ProductSubscriptionStatus.active
                                                    val subscriptionProToBlocked =
                                                        previousStatus?.productSubscriptionStatus == StripeClient.ProductSubscriptionStatus.active
                                                                && status.productSubscriptionStatus == StripeClient.ProductSubscriptionStatus.blocked

                                                    val subscriptionToBeCanceled =
                                                        event.data.previousAttributes?.get("cancel_at_period_end") == false
                                                                && subscription.cancelAtPeriodEnd == true
                                                    val trialSubscriptionToBeCanceled =
                                                        subscriptionToBeCanceled
                                                                && status == Status.trialing
                                                    val subscriptionUncanceled =
                                                        status.productSubscriptionStatus == StripeClient.ProductSubscriptionStatus.active
                                                                && event.data.previousAttributes?.get("cancel_at_period_end") == true
                                                                && subscription.cancelAtPeriodEnd == false
                                                    val updatedCancellationDetails = setOf("cancellation_details") == event.data.previousAttributes?.keys?.toSet()
                                                    val updatedDefaultPaymentMethod = setOf("default_payment_method") == event.data.previousAttributes?.keys?.toSet()
                                                    val extendedActivePeriod =
                                                        previousStatus == null
                                                                && status == Status.active
                                                                && setOf("latest_invoice", "current_period_end", "current_period_start") == event.data.previousAttributes?.keys?.toSet()
                                                    val usedDiscount =
                                                        previousStatus == null
                                                                && status == Status.active
                                                                && event.data.previousAttributes?.keys?.contains("discount") == true

                                                    when {
                                                        extendedActivePeriod -> {
                                                            notifySlack("$customerEmail has done recurring payment and continued as Pro (active) subscriber of ${product.name}")
                                                        }

                                                        trialSubscriptionToBeCanceled -> {
                                                            notifySlack("$customerEmail has scheduled to unsubscribe from ${product.name} at the end of trial")
                                                            launch {
                                                                product.emailForCancelDuringTrial?.also { emailConfig ->
                                                                    emailService.sendEmail(
                                                                        from = Email(
                                                                            address = emailConfig.from.email,
                                                                            label = emailConfig.from.label,
                                                                        ),
                                                                        toList = listOf(Email(customerEmail)),
                                                                        mail = MailTemplate(emailConfig.sendgridTemplateId),
                                                                        unsubscribeSettings = emailConfig.unsubscribeSettings,
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // has to be after trialSubscriptionToBeCanceled
                                                        subscriptionToBeCanceled -> {
                                                            // subscription is set to be cancelled at the end of billing period
                                                            notifySlack("$customerEmail has scheduled to unsubscribe from ${product.name} at the end of billing period")
                                                        }

                                                        subscriptionTrialToActive -> {
                                                            // changed trial subscription to active (paid) subscription
                                                            notifySlack("$customerEmail upgraded from trial to active subscriber of ${product.name}")
                                                            Log.beginSubscription(
                                                                webClientId = subscription.metadata["web-client-id"],
                                                                userAnalyticsId = userAnalyticsId,
                                                                productId = productId,
                                                            )
                                                        }

                                                        subscriptionIncompleteToExpired -> {
                                                            // changed incomplete subscription to incomplete_expired
                                                            notifySlack("$customerEmail is  from Blocked (incomplete) to ex-Pro (incomplete_expired) subscriber of ${product.name} because first payment was not done within 23hr past of due date.")
                                                        }

                                                        subscriptionProToBlocked -> {
                                                            notifySlack("$customerEmail changed from Pro ($previousStatus) to Blocked ($status) subscriber of ${product.name}")
                                                            disableProSubscriptionEvent()
                                                        }

                                                        // has to be after subscriptionTrialToActive
                                                        subscriptionNonProToPro -> {
                                                            // updated from non-pro to an active (paid) subscription
                                                            notifySlack("$customerEmail changed from non-Pro ($previousStatus) to Pro ($status) subscriber of ${product.name}")
                                                            proSubscriptionEvent()
                                                            Log.beginSubscription(
                                                                webClientId = subscription.metadata["web-client-id"],
                                                                userAnalyticsId = userAnalyticsId,
                                                                productId = productId,
                                                            )
                                                        }

                                                        // has to be after subscriptionToBeCanceled
                                                        updatedCancellationDetails -> {
                                                            logWithMDC(
                                                                *listOfNotNull(
                                                                    subscription.cancellationDetails?.reason?.let { "cancellation_reason" to it },
                                                                    subscription.cancellationDetails?.feedback?.let { "cancellation_feedback" to it },
                                                                    subscription.cancellationDetails?.comment?.let { "cancellation_comment" to it },
                                                                ).toTypedArray()
                                                            ) {
                                                                notifySlack("$customerEmail entered cancellation details")
                                                            }
                                                        }

                                                        updatedDefaultPaymentMethod -> {
                                                            call.application.log.info("Updated default payment method")
                                                        }

                                                        subscriptionUncanceled -> {
                                                            notifySlack("$customerEmail has uncancelled subscription")
                                                        }

                                                        usedDiscount -> {
                                                            call.application.log.info("$customerEmail has updated discount coupon")
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
                                                            logWithMDC(
                                                                *listOfNotNull(
                                                                    subscription.cancellationDetails?.reason?.let { "cancellation_reason" to it },
                                                                    subscription.cancellationDetails?.feedback?.let { "cancellation_feedback" to it },
                                                                ).toTypedArray()
                                                            ) {
                                                                notifySlack("$customerEmail has unsubscribed from ${product.name}")
                                                            }
                                                            disableProSubscriptionEvent()
                                                            Log.endSubscription(
                                                                webClientId = subscription.metadata["web-client-id"],
                                                                userAnalyticsId = userAnalyticsId,
                                                                productId = productId,
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
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}