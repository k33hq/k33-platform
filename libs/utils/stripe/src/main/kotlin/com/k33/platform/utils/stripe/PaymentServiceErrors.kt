package com.k33.platform.utils.stripe

import io.ktor.http.HttpStatusCode

sealed class PaymentServiceError(
    val httpStatusCode: HttpStatusCode,
    override val message: String,
) : Exception(message)

data object AlreadySubscribed : PaymentServiceError(
    httpStatusCode = HttpStatusCode.Conflict,
    message = "Already subscribed",
) {
    private fun readResolve(): Any = AlreadySubscribed
}

class NotFound(message: String) : PaymentServiceError(
    httpStatusCode = HttpStatusCode.NotFound,
    message = message,
)

class BadRequest(message: String) : PaymentServiceError(
    httpStatusCode = HttpStatusCode.BadRequest,
    message = message,
)

class ServiceUnavailable(message: String) : PaymentServiceError(
    httpStatusCode = HttpStatusCode.ServiceUnavailable,
    message = message,
)

data object TooManyRequests: PaymentServiceError(
    httpStatusCode = HttpStatusCode.TooManyRequests,
    message = "Too many requests. Please try again after some time.",
) {
    private fun readResolve(): Any = TooManyRequests
}

data object InternalServerError: PaymentServiceError(
    httpStatusCode = HttpStatusCode.InternalServerError,
    message = "Internal Server Error",
) {
    private fun readResolve(): Any = InternalServerError
}

data object NotEligibleForFreeTrial: PaymentServiceError(
    httpStatusCode = HttpStatusCode.Forbidden,
    message = "Not eligible for free trial",
) {
    private fun readResolve(): Any = NotEligibleForFreeTrial
}