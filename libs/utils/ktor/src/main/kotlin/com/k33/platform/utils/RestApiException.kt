package com.k33.platform.utils

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

data class RestApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : Exception(message)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)