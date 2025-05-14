package com.k33.platform.payment.stripe

import com.k33.platform.email.Email
import com.k33.platform.email.EmailTemplateConfig
import com.k33.platform.email.MailTemplate
import com.k33.platform.email.getEmailService
import com.k33.platform.utils.analytics.Log
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import com.k33.platform.utils.stripe.AlreadySubscribed
import com.k33.platform.utils.stripe.NotEligibleForFreeTrial
import com.k33.platform.utils.stripe.NotFound
import com.k33.platform.utils.stripe.ServiceUnavailable
import com.k33.platform.utils.stripe.StripeClient
import com.stripe.model.Customer
import com.stripe.model.Price
import com.stripe.param.CustomerCreateParams
import com.stripe.param.CustomerListParams
import com.stripe.param.CustomerSearchParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionListParams
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.checkout.SessionListLineItemsParams
import com.stripe.param.checkout.SessionListParams
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.collections.filter
import kotlin.collections.filterNotNull
import kotlin.collections.flatMap
import kotlin.collections.flatten
import kotlin.collections.groupBy
import kotlin.collections.map
import kotlin.collections.sortedByDescending
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import com.stripe.model.Subscription as StripeSubscription
import com.stripe.model.billingportal.Session as StripeCustomerPortalSession
import com.stripe.model.checkout.Session as StripeCheckoutSession
import com.stripe.param.billingportal.SessionCreateParams as CustomerPortalSessionCreateParams
import com.stripe.param.checkout.SessionCreateParams as CheckoutSessionCreateParams

object StripeService {

    private val logger by getLogger()

    private val stripeClient by lazy {
        StripeClient(System.getenv("STRIPE_API_KEY"))
    }

    private val corporatePlanCoupon by lazy { System.getenv("STRIPE_COUPON_CORPORATE_PLAN") }

    data class CheckoutSession(
        val url: String,
        val expiresAt: String,
        val priceId: String,
        val successUrl: String,
        val cancelUrl: String,
    )

    private val isProd by lazy { System.getenv("GCP_PROJECT_ID").endsWith("prod", ignoreCase = true) }

    private val productMap: Map<String, ProductConfig> by loadConfig(
        "researchApp",
        "apps.research.products",
    )

    private val newUserOfferEmail: EmailTemplateConfig by loadConfig(
        "researchApp",
        "apps.research.newUserOfferEmail",
    )

    private val emailService by getEmailService()

    /**
     * [Stripe API - Create Checkout Session](https://stripe.com/docs/api/checkout/sessions/create)
     */
    suspend fun createOrFetchCheckoutSession(
        customerEmail: String,
        priceId: String,
        successUrl: String,
        cancelUrl: String,
        webClientId: String?,
        userAnalyticsId: String?,
    ): CheckoutSession {
        val price = stripeClient.call {
            Price.retrieve(priceId, requestOptions)
        } ?: throw NotFound("price: [$priceId] not found")
        val productId = price.product
        val customerInfo = getCustomersByEmail(customerEmail = customerEmail)
        val (customerId, hasCurrentOrPriorSubscription) = if (customerInfo.customers.isNotEmpty()) {
            val subscriptionInfo = getSubscriptionInfo(customerInfo = customerInfo)
            if (subscriptionInfo.isCurrentlySubscribedTo(productId = productId)) {
                logger.warn("Already subscribed")
                throw AlreadySubscribed
            }
            val existingSessions = getCheckoutSessions(
                customerEmail = customerEmail,
                priceId = priceId,
            )
            if (existingSessions.isNotEmpty()) {
                val existingSession = existingSessions.singleOrNull() ?: existingSessions.maxBy { it.expiresAt }
                logger.warn("Returning existing checkout session which expires at ${existingSession.expiresAt}")
                return existingSession
            }
            customerInfo.customers.first().id to subscriptionInfo.hasCurrentOrPriorSubscription(productId = productId)
        } else {
            val customerId = stripeClient.call {
                Customer.create(
                    CustomerCreateParams
                        .builder()
                        .setEmail(customerEmail)
                        .build(),
                    withIdempotencyKey(payload = customerEmail),
                )
            }?.id ?: throw ServiceUnavailable("Failed to create Stripe customer")
            customerId to false
        }
        val params = CheckoutSessionCreateParams
            .builder()
            .setMode(CheckoutSessionCreateParams.Mode.SUBSCRIPTION)
            .setLocale(CheckoutSessionCreateParams.Locale.AUTO)
            // in case of duplicates we give priority to one created later
            .setCustomer(customerId)
            // https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-payment_method_collection
            .setPaymentMethodCollection(CheckoutSessionCreateParams.PaymentMethodCollection.ALWAYS)
            .apply {
                if (isProd
                    && customerEmail.endsWith("@k33.com", ignoreCase = true)
                    && productMap["pro"]?.stripeProduct?.id == productId
                ) {
                    // https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-discounts
                    addDiscount(
                        CheckoutSessionCreateParams
                            .Discount
                            .builder()
                            .setCoupon(corporatePlanCoupon)
                            .build()
                    )
                } else {
                    if (productMap.values.any { it.stripeProduct.id == productId && it.stripeProduct.enableTrial }
                        && !hasCurrentOrPriorSubscription
                    ) {
                        setSubscriptionData(
                            CheckoutSessionCreateParams.SubscriptionData.builder()
                                // https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-subscription_data-trial_period_days
                                .setTrialPeriodDays(TRIAL_PERIOD_DAYS.toLong())
                                .setTrialSettings(
                                    SessionCreateParams.SubscriptionData.TrialSettings
                                        .builder()
                                        .setEndBehavior(
                                            SessionCreateParams.SubscriptionData.TrialSettings.EndBehavior
                                                .builder()
                                                // https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-subscription_data-trial_settings-end_behavior-missing_payment_method
                                                .setMissingPaymentMethod(
                                                    SessionCreateParams.SubscriptionData.TrialSettings
                                                        .EndBehavior.MissingPaymentMethod.CANCEL
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    }
                    setAllowPromotionCodes(true)
                }
            }
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                CheckoutSessionCreateParams
                    .LineItem
                    .builder()
                    .setPrice(priceId)
                    .setQuantity(1)
                    .build()
            )
            .apply {
                if (webClientId != null) {
                    putMetadata("web-client-id", webClientId)
                }
            }
            .build()
        val checkoutSession = stripeClient.call {
            StripeCheckoutSession.create(
                params,
                withIdempotencyKey(payload = customerId + priceId),
            )
        } ?: throw NotFound("Resource not found. Failed to create checkout session")
        val lineItems = stripeClient.call {
            checkoutSession.listLineItems(SessionListLineItemsParams.builder().build(), requestOptions)
        } ?: throw ServiceUnavailable("Missing line items in checkout session")
        Log.beginCheckout(
            webClientId = webClientId,
            userAnalyticsId = userAnalyticsId,
            value = price.unitAmount / 100.0f,
            currency = price.currency,
            productId = productId,
            pageUrl = successUrl,
        )
        return CheckoutSession(
            url = checkoutSession.url,
            expiresAt = Instant.ofEpochSecond(checkoutSession.expiresAt).toString(),
            priceId = lineItems.data.single().price.id,
            successUrl = checkoutSession.successUrl,
            cancelUrl = checkoutSession.cancelUrl,
        )
    }

    data class CustomerPortalSession(
        val url: String,
        val returnUrl: String,
    )

    suspend fun createCustomerPortalSession(
        customerEmail: String,
        returnUrl: String,
    ): CustomerPortalSession {
        val customerInfo = getCustomersByEmail(customerEmail = customerEmail)
        val params = CustomerPortalSessionCreateParams
            .builder()
            // in case of duplicates we give priority to one created later
            .setCustomer(customerInfo.customers.firstOrNull()?.id ?: throw NotFound("customer not found"))
            .setReturnUrl(returnUrl)
            .build()
        val customerPortalSession = stripeClient.call {
            StripeCustomerPortalSession.create(params, requestOptions)
        } ?: throw NotFound("customer not found")
        return CustomerPortalSession(
            url = customerPortalSession.url,
            returnUrl = customerPortalSession.returnUrl,
        )
    }

    suspend fun getCustomerEmail(stripeCustomerId: String): String? = stripeClient.call {
        Customer.retrieve(stripeCustomerId, requestOptions)
    }?.email

    data class CustomerInfo(
        val customerEmail: String,
        val customers: List<Customer>,
    )

    private suspend fun getCustomersByEmail(
        customerEmail: String
    ): CustomerInfo {
        val listParams = CustomerListParams
            .builder()
            .setEmail(customerEmail)
            .build()
        val customers = stripeClient.call {
            Customer.list(listParams, requestOptions)
        }
            ?.data
            // in case of duplicates we give priority to one created later
            ?.sortedByDescending { it.created }
            ?: emptyList()
        if (customers.size > 1) {
            logger.error("Found multiple Stripe customers with email: $customerEmail")
        }
        return CustomerInfo(
            customerEmail = customerEmail,
            customers = customers,
        )
    }

    @Suppress("EnumEntryName")
    enum class ProductSubscriptionStatus {
        active,
        blocked,
        ended,
    }

    suspend fun getSubscription(
        customerEmail: String,
        productId: String,
    ): ProductSubscription? {
        val customerInfo = getCustomersByEmail(customerEmail = customerEmail)
        if (customerInfo.customers.isEmpty()) {
            return null
        }
        return getSubscriptionInfo(customerInfo = customerInfo)
            .getProductSubscription(productId = productId)
    }

    data class ProductSubscription(
        val productId: String,
        val status: ProductSubscriptionStatus,
        val priceId: String,
    )

    data class Subscription(
        val productId: String,
        val status: Status,
        val priceId: String,
    )

    data class SubscriptionContext(
        private val productIdToSubscriptionMap: Map<String, List<Subscription>>,
    ) {

        fun isCurrentlySubscribedTo(
            productId: String,
        ): Boolean = setOf(
            ProductSubscriptionStatus.active,
            ProductSubscriptionStatus.blocked,
        ).contains(getProductSubscription(productId)?.status)

        fun getProductSubscription(
            productId: String,
        ): ProductSubscription? = productIdToSubscriptionMap[productId]
            ?.minByOrNull { it.status.productSubscriptionStatus }
            ?.let { subscription ->
                ProductSubscription(
                    productId = productId,
                    status = subscription.status.productSubscriptionStatus,
                    priceId = subscription.priceId,
                )
            }

        fun hasCurrentOrPriorSubscription(
            productId: String,
        ) = productIdToSubscriptionMap
            .keys
            .contains(productId)
    }

    private suspend fun getSubscriptionInfo(
        customerInfo: CustomerInfo,
    ): SubscriptionContext = coroutineScope {
        SubscriptionContext(
            customerInfo
                .customers
                .map { customer ->
                    async {
                        stripeClient.call {
                            StripeSubscription.list(
                                SubscriptionListParams.builder()
                                    .setCustomer(customer.id)
                                    .setStatus(SubscriptionListParams.Status.ALL)
                                    .build(),
                                requestOptions,
                            )
                        }
                            ?.data
                            ?.flatMap { subscription ->
                                subscription
                                    .items
                                    .data
                                    .map { subscriptionItem ->
                                        subscriptionItem.price.product to Subscription(
                                            productId = subscriptionItem.price.product,
                                            status = Status.valueOf(subscription.status),
                                            priceId = subscriptionItem.price.id,
                                        )
                                    }
                            }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .flatten()
                .groupBy({ it.first }, { it.second })
        )
    }

    private suspend fun getCheckoutSessions(
        customerEmail: String,
        priceId: String,
    ): Collection<CheckoutSession> = coroutineScope {
        val details = SessionListParams.CustomerDetails
            .builder()
            .setEmail(customerEmail)
            .build()
        val params = SessionListParams
            .builder()
            .setCustomerDetails(details)
            .build()
        val sessions = stripeClient.call {
            StripeCheckoutSession.list(params, requestOptions)
        }
            ?.data
            ?.filter { session ->
                session.status == "open"
                        && session.hasLineItemWith(priceId = priceId)
            }
            ?.sortedByDescending { it.expiresAt }
            ?.map { checkoutSession ->
                async {
                    val lineItems = stripeClient.call {
                        checkoutSession.listLineItems(SessionListLineItemsParams.builder().build(), requestOptions)
                    } ?: throw ServiceUnavailable("Missing line items in checkout session")
                    CheckoutSession(
                        url = checkoutSession.url,
                        expiresAt = Instant.ofEpochSecond(checkoutSession.expiresAt).toString(),
                        priceId = lineItems.data.single().price.id,
                        successUrl = checkoutSession.successUrl,
                        cancelUrl = checkoutSession.cancelUrl,
                    )
                }
            }
            ?.awaitAll()
            ?: emptyList()
        if (sessions.size > 1) {
            logger.warn("Found ${sessions.size} checkout sessions")
        }
        sessions
    }

    suspend fun emailOfferToCustomersWithoutSubscription(): Boolean {
        val past = 3.days
        val window = 1.hours
        val thisHour = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val createdSince = thisHour.minus((past + window).toJavaDuration())
        val createdBefore = (createdSince + window.toJavaDuration())

        try {
            val emailsOfCustomersWithNoSubscription = getCustomersWithoutSubscription(
                createdSince = createdSince,
                createdBefore = createdBefore,
            )
            if (emailsOfCustomersWithNoSubscription.isEmpty()) {
                return true
            }
            val emailSuccess = emailService.sendEmail(
                from = Email(
                    address = newUserOfferEmail.from.email,
                    label = newUserOfferEmail.from.label,
                ),
                toList = emailsOfCustomersWithNoSubscription,
                mail = MailTemplate(templateId = newUserOfferEmail.sendgridTemplateId),
                unsubscribeSettings = newUserOfferEmail.unsubscribeSettings,
            )
            if (emailSuccess) {
                logger.info("Offer email sent to ${emailsOfCustomersWithNoSubscription.size} stripe users without subscription and created since: $createdSince and before: $createdBefore")
            } else {
                logger.error(
                    NotifySlack.ALERTS,
                    "Error sending offer email sent to ${emailsOfCustomersWithNoSubscription.size} stripe users without subscription and created since: $createdSince and before: $createdBefore"
                )
            }
            return emailSuccess
        } catch (e: Exception) {
            logger.error(
                NotifySlack.ALERTS,
                "Exception in sending offer email to stripe users without subscription and created since: $createdSince and before: $createdBefore",
                e
            )
            return false
        }
    }

    internal suspend fun getCustomersWithoutSubscription(
        createdSince: Instant,
        createdBefore: Instant,
    ): List<Email> {

        val createdSinceEpochSeconds = createdSince.epochSecond
        val createdBeforeEpochSeconds = createdBefore.epochSecond

        logger.info("Fetching stripe users created since: $createdSince and before: $createdBefore")
        val customers = run {
            val customers = mutableListOf<Customer>()
            var page: String? = null
            do {
                val result = stripeClient.call {
                    Customer.search(
                        CustomerSearchParams
                            .builder()
                            .setQuery("created>=$createdSinceEpochSeconds AND created<$createdBeforeEpochSeconds")
                            .addExpand("data.subscriptions")
                            .setLimit(100)
                            .apply {
                                if (page != null) {
                                    setPage(page)
                                }
                            }
                            .build(),
                        requestOptions,
                    )
                }
                page = result?.nextPage
                val fetchedCustomers = result
                    ?.data
                    ?: emptyList<Customer>()
                customers.addAll(fetchedCustomers)
            } while (result?.hasMore == true)
            customers.toList()
        }

        logger.info("Found ${customers.size} stripe users created since: $createdSince and before: $createdBefore")

        val emailsOfCustomersWithNoSubscription = customers
            .sortedBy { it.created }
            .groupBy { it.email }
            .filter { (_, customers) -> customers.flatMap { it.subscriptions.data }.isEmpty() }
            .keys
            .filterNotNull()
            .map(::Email)
            .toList()

        logger.info("Filtered ${emailsOfCustomersWithNoSubscription.size} stripe users without subscription and created since: $createdSince and before: $createdBefore")

        return emailsOfCustomersWithNoSubscription
    }

    private suspend fun createTrialSubscription(
        customerEmail: String,
        priceId: String,
        promotionCode: String?,
    ) {
        val productId = stripeClient.call {
            Price.retrieve(priceId, requestOptions)
        }?.product ?: throw NotFound("price: [$priceId] not found")
        val customerInfo = getCustomersByEmail(customerEmail = customerEmail)
        val customerId = if (customerInfo.customers.isNotEmpty()) {
            val subscriptionInfo = getSubscriptionInfo(customerInfo = customerInfo)
            if (subscriptionInfo.isCurrentlySubscribedTo(productId = productId)) {
                logger.warn("Already subscribed")
                throw AlreadySubscribed
            }
            if (subscriptionInfo.hasCurrentOrPriorSubscription(productId = productId)) {
                logger.warn("Already had a trial before")
                throw NotEligibleForFreeTrial
            }
            customerInfo.customers.first().id
        } else {
            val customerId = stripeClient.call {
                Customer.create(
                    CustomerCreateParams
                        .builder()
                        .setEmail(customerEmail)
                        .build(),
                    withIdempotencyKey(payload = customerEmail),
                )
            }?.id ?: throw ServiceUnavailable("Failed to create Stripe customer")
            customerId
        }
        val params = SubscriptionCreateParams
            .builder()
            // in case of duplicates we give priority to one created later
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams.Item
                    .builder()
                    .setPrice(priceId)
                    .setQuantity(1)
                    .build()
            )
            .apply {
                if (customerEmail.endsWith("@k33.com", ignoreCase = true)) {
                    addDiscount(
                        SubscriptionCreateParams.Discount
                            .builder()
                            .setCoupon(corporatePlanCoupon)
                            .build()
                    )
                } else {
                    setTrialPeriodDays(TRIAL_PERIOD_DAYS.toLong())
                    setTrialSettings(
                        SubscriptionCreateParams.TrialSettings
                            .builder()
                            .setEndBehavior(
                                SubscriptionCreateParams.TrialSettings.EndBehavior
                                    .builder()
                                    .setMissingPaymentMethod(
                                        SubscriptionCreateParams.TrialSettings.EndBehavior.MissingPaymentMethod.CANCEL
                                    )
                                    .build()
                            )
                            .build()
                    )
                    if (promotionCode != null) {
                        addDiscount(
                            SubscriptionCreateParams.Discount
                                .builder()
                                .setPromotionCode(promotionCode)
                                .build()
                        )
                    }
                }
            }
            .build()
        val subscription = stripeClient.call {
            StripeSubscription.create(params, requestOptions)
        } ?: throw ServiceUnavailable("Failed to create Stripe subscription")
        logger.info("subscription.status: ${subscription.status}")
        if (subscription.trialStart != null) {
            logger.info("subscription.trialStart: ${Instant.ofEpochSecond(subscription.trialStart)}")
        }
        if (subscription.trialEnd != null) {
            logger.info("subscription.trialStart: ${Instant.ofEpochSecond(subscription.trialEnd)}")
        }
    }

    private suspend fun updateSubscriptionPrice(
        customerEmail: String,
        priceId: String,
    ) {
        val price = stripeClient.call {
            Price.retrieve(priceId, requestOptions)
        } ?: throw NotFound("price: [$priceId] not found")
        val productId = price.product
        val customerInfo = getCustomersByEmail(customerEmail = customerEmail)
        if (customerInfo.customers.isEmpty()) {
            throw NotFound("Customer does not have active subscription")
        }
        val subscriptionInfo = getSubscriptionInfo(customerInfo = customerInfo)
        if (subscriptionInfo.isCurrentlySubscribedTo(productId = productId)) {
            logger.warn("Already subscribed")
            throw AlreadySubscribed
        }
    }

    private suspend fun StripeCheckoutSession.hasLineItemWith(
        priceId: String
    ): Boolean = stripeClient.call {
        listLineItems(
            SessionListLineItemsParams.builder().build(),
            requestOptions
        )
    }
        ?.data
        ?.map { lineItem -> lineItem.price.id }
        ?.contains(priceId)
        ?: false
}