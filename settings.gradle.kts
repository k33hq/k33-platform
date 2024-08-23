plugins {
    id("de.fayard.refreshVersions") version "0.51.0"
}

refreshVersions {
    rejectVersionIf {
        candidate.stabilityLevel != de.fayard.refreshVersions.core.StabilityLevel.Stable
    }
    extraArtifactVersionKeyRules(file("refreshVersions-extra-rules.txt"))
}

rootProject.name = "k33-platform"

include(
    // apps
    "apps:acceptance-tests",
    "apps:k33-backend",

    "apps:contentful-admin-tool",
    "apps:firestore-admin",
    "apps:stripe-admin-tool",

    "apps:oauth2-provider-emulator",
    "apps:oauth2-provider-emulator:oauth2-provider-api",

    // libs

    // libs / apps
    "libs:apps:invest",
    "libs:apps:vault",

    // libs / clients
    "libs:clients:contentful-client",
    "libs:clients:google-analytics-client",
    "libs:clients:k33-backend-client",

    // libs / services

    "libs:services:email",
    "libs:services:email:email-api",
    "libs:services:email:sendgrid",

    "libs:services:email-subscription",

    "libs:services:identity",
    "libs:services:identity:apple",
    "libs:services:identity:identity-api",
    "libs:services:identity:gcp",

    "libs:services:payment",
    "libs:services:payment:payment-endpoint",
    "libs:services:payment:stripe-service",

    "libs:services:user",
    "libs:services:user:user-analytics",
    "libs:services:user:user-endpoint",
    "libs:services:user:user-graphql",
    "libs:services:user:user-model",
    "libs:services:user:user-service",

    // libs / utils

    "libs:utils:analytics",
    "libs:utils:analytics:google-analytics",

    "libs:utils:algolia",
    "libs:utils:cms",
    "libs:utils:cms:contentful",

    "libs:utils:config",
    "libs:utils:file-store",
    "libs:utils:firebase-auth",
    "libs:utils:fireblocks",
    "libs:utils:google-coroutine-ktx",
    "libs:utils:graphql",

    "libs:utils:ktor",

    "libs:utils:logging",
    "libs:utils:logging:gcp-logging",
    "libs:utils:logging:marker-api",
    "libs:utils:logging:slack-logging",

    "libs:utils:metrics",
    "libs:utils:slack",
    "libs:utils:stripe",
)
