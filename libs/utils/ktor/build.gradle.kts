plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(Ktor.server.core)
    implementation(Ktor.server.callId)
    implementation(Ktor.server.contentNegotiation)
    implementation(Ktor.server.defaultHeaders)
    implementation(Ktor.plugins.serialization.kotlinx.json)
    implementation(Ktor.server.statusPages)
}