package com.k33.platform.utils

import io.ktor.http.HttpStatusCode

class ClientErrorException(
    val status: HttpStatusCode,
    message: String,
) : Exception(message)