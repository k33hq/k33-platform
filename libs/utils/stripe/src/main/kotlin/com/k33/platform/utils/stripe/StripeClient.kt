package com.k33.platform.utils.stripe

import com.k33.platform.utils.logging.NotifySlack
import com.k33.platform.utils.logging.getLogger
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.IdempotencyException
import com.stripe.exception.PermissionException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import com.stripe.net.RequestOptions
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class StripeClient(
    apiKey: String
) {
    private val logger by getLogger()

    val requestOptions: RequestOptions by lazy {
        RequestOptions.builder()
            .setApiKey(apiKey)
            .setClientId("k33-backend")
            .build()
    }

    suspend fun <R> call(
        block: suspend StripeClient.() -> R
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

    fun withIdempotencyKey(payload: String): RequestOptions {

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
