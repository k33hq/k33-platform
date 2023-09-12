package com.k33.platform.identity.auth.gcp

import io.ktor.server.auth.Principal

data class UserInfo(
    val userId: String,
    val email: String,
) : Principal