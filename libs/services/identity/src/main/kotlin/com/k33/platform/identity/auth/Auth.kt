package com.k33.platform.identity.auth

import com.k33.platform.identity.auth.apple.appleJwtAuthConfig
import com.k33.platform.identity.auth.gcp.gcpEndpointsAuthConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication

fun Application.module() {
    install(Authentication) {
        appleJwtAuthConfig()
        gcpEndpointsAuthConfig()
    }
}