plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:services:identity:identity-api"))

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)
    implementation(Ktor.server.callLogging)
    implementation(Ktor.server.callId)

    implementation(KotlinX.serialization.json)
}