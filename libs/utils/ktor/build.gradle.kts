plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(Ktor.server.core)
    implementation(Ktor.server.callId)
    implementation(Ktor.server.contentNegotiation)
    implementation(Ktor.server.defaultHeaders)
    implementation("io.ktor:ktor-serialization-kotlinx-json:_")
    implementation(Ktor.server.statusPages)
}