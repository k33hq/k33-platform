rootProject.name = "arcane-platform"

include(
    // apps
    "apps:acceptance-tests",
    "apps:arcane-platform-app",
    "apps:arcane-gcp-platform-app",
    "apps:tools",

    // libs
    // libs / clients
    "libs:clients:arcane-platform-client",

    // libs / services

    "libs:services:email",
    "libs:services:email:email-api",
    "libs:services:email:sendgrid",

    "libs:services:identity",
    "libs:services:identity:apple",
    "libs:services:identity:identity-api",
    "libs:services:identity:gcp",

    "libs:services:payments",
    "libs:services:terms-and-conditions",

    // libs / utils

    "libs:utils:appleid-auth",
    "libs:utils:cms",
    "libs:utils:cms:cms-api",
    "libs:utils:cms:contentful",

    "libs:utils:config",
    "libs:utils:firebase-auth",

    "libs:utils:firestore",
    "libs:utils:firestore:firestore-api",
    "libs:utils:firestore:firestore-impl",

    "libs:utils:ktor",
    "libs:utils:logging",
    "libs:utils:metrics",
)
