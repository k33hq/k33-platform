package com.k33.platform.payment.stripe

import com.k33.platform.utils.analytics.Log
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.IdempotencyException
import com.stripe.exception.PermissionException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import com.stripe.model.Customer
import com.stripe.model.Price
import com.stripe.net.RequestOptions
import com.stripe.param.CustomerCreateParams
import com.stripe.param.CustomerSearchParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionListParams
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.checkout.SessionListLineItemsParams
import com.stripe.param.checkout.SessionListParams
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant
import kotlin.collections.filter
import kotlin.collections.filterNotNull
import kotlin.collections.flatMap
import kotlin.collections.flatten
import kotlin.collections.groupBy
import kotlin.collections.map
import kotlin.collections.sortedByDescending
import com.stripe.model.Subscription as StripeSubscription
import com.stripe.model.billingportal.Session as StripeCustomerPortalSession
import com.stripe.model.checkout.Session as StripeCheckoutSession
import com.stripe.param.billingportal.SessionCreateParams as CustomerPortalSessionCreateParams
import com.stripe.param.checkout.SessionCreateParams as CheckoutSessionCreateParams

object StripeClient {

    private val logger by getLogger()

    private val requestOptions by lazy {
        RequestOptions.builder()
            .setApiKey(System.getenv("STRIPE_API_KEY"))
            .setClientId("k33-backend")
            .build()
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

    val productMap: Map<String, ProductConfig> by loadConfig(
        "researchApp",
        "apps.research.products",
    )

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
        val price = stripeCall {
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
            val customerId = stripeCall {
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
                    if (productMap[productId]?.stripeProduct?.enableTrial == true
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
            // .putMetadata("web-client-id", webClientId)
            .build()
        val checkoutSession = stripeCall {
            StripeCheckoutSession.create(
                params,
                withIdempotencyKey(payload = customerId + priceId),
            )
        } ?: throw NotFound("Resource not found. Failed to create checkout session")
        val lineItems = stripeCall {
            checkoutSession.listLineItems(SessionListLineItemsParams.builder().build(), requestOptions)
        } ?: throw ServiceUnavailable("Missing line items in checkout session")
        Log.beginCheckout(
            webClientId = webClientId,
            userAnalyticsId = userAnalyticsId,
            value = price.unitAmount / 100.0f,
            currency = price.currency,
            productId = productId,
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
        val customerPortalSession = stripeCall {
            StripeCustomerPortalSession.create(params, requestOptions)
        } ?: throw NotFound("customer not found")
        return CustomerPortalSession(
            url = customerPortalSession.url,
            returnUrl = customerPortalSession.returnUrl,
        )
    }

    suspend fun getCustomerEmail(stripeCustomerId: String): String? = stripeCall {
        Customer.retrieve(stripeCustomerId, requestOptions)
    }?.email

    data class CustomerInfo(
        val customerEmail: String,
        val customers: List<Customer>,
    )

    private suspend fun getCustomersByEmail(
        customerEmail: String
    ): CustomerInfo {
        val searchParams = CustomerSearchParams
            .builder()
            .setQuery("email:'$customerEmail'")
            .build()
        val customers = stripeCall {
            Customer.search(searchParams, requestOptions)
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
                        stripeCall {
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
        val sessions = stripeCall {
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
                    val lineItems = stripeCall {
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

    private suspend fun createTrialSubscription(
        customerEmail: String,
        priceId: String,
        promotionCode: String?,
    ) {
        val productId = stripeCall {
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
            val customerId = stripeCall {
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
                    setCoupon(corporatePlanCoupon)
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
                        setPromotionCode(promotionCode)
                    }
                }
            }
            .build()
        val subscription = stripeCall {
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
        val price = stripeCall {
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
    ): Boolean = stripeCall {
        listLineItems(
            SessionListLineItemsParams.builder().build(),
            requestOptions
        )
    }
        ?.data
        ?.map { lineItem -> lineItem.price.id }
        ?.contains(priceId)
        ?: false

    private suspend fun <R> stripeCall(
        block: suspend () -> R
    ): R? {
        return withContext(Dispatchers.IO) {
            try {
                block()
            }
            // we are not catching InvalidRequestException since we want to distinguish between 400 Bad Request and
            // 404 Not Found.

            // https://stripe.com/docs/error-handling?lang=java#payment-errors
            // 402
            // The parameters were valid but the request failed.
            catch (e: CardException) {
                logger.error(NotifySlack.ALERTS, "Stripe Payment Error", e)
                throw BadRequest(e.userMessage)
            }

            // https://stripe.com/docs/error-handling?lang=java#connection-errors
            // This is not an HTTP error
            catch (e: ApiConnectionException) {
                logger.error(NotifySlack.ALERTS, "Stripe API Connection Exception", e)
                throw ServiceUnavailable(e.userMessage)

            }

            // https://stripe.com/docs/error-handling?lang=java#rate-limit-errors
            // 429
            // Too many requests hit the API too quickly. We recommend an exponential backoff of your requests.
            catch (e: RateLimitException) {
                logger.error(NotifySlack.ALERTS, "Received rate limit error from Stripe", e)
                throw TooManyRequests

            }

            // 401
            // No valid API key provided.
            catch (e: AuthenticationException) {
                logger.error(NotifySlack.ALERTS, "Missing Stripe API key", e)
                throw InternalServerError

            }

            // https://stripe.com/docs/error-handling?lang=java#permission-errors
            // 403
            // The API key doesn't have permissions to perform the request.
            catch (e: PermissionException) {
                logger.error(NotifySlack.ALERTS, "API key is missing permissions", e)
                throw InternalServerError

            }

            // https://stripe.com/docs/error-handling?lang=java#idempotency-errors
            // 400 or 404
            // You used an idempotency key for something unexpected,
            // like replaying a request but passing different parameters.
            catch (e: IdempotencyException) {
                logger.error(NotifySlack.ALERTS, "Stripe Idempotency Exception", e)
                throw BadRequest(e.userMessage)

            } catch (e: StripeException) {
                // https://stripe.com/docs/api/errors?lang=java
                when (e.statusCode) {
                    // The request was unacceptable, often due to missing a required parameter.
                    400 -> {
                        throw BadRequest(e.userMessage)
                    }
                    // The requested resource doesn't exist.
                    404 -> null
                    // The request conflicts with another request (perhaps due to using the same idempotent key).
                    409 -> throw InternalServerError
                    // Something went wrong on Stripe's end. (These are rare.)
                    in 500..599 -> {
                        logger.error(NotifySlack.ALERTS, "Stripe error", e)
                        throw ServiceUnavailable(e.userMessage)
                    }

                    else -> {
                        // Something went wrong on Stripe's end. (These are rare.)
                        throw InternalServerError
                    }
                }
            }
        }
    }

    private fun withIdempotencyKey(payload: String): RequestOptions {

        fun hashOf(payload: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(payload.lowercase().toByteArray())
            return md.digest().encodeBase64()
        }

        return requestOptions
            .toBuilderFullCopy()
            .setIdempotencyKey(hashOf(payload = payload))
            .build()
    }
}