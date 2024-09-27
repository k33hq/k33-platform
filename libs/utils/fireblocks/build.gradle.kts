plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.nimbusds:nimbus-jose-jwt:_")
    implementation("org.bouncycastle:bcpkix-jdk18on:_")

    implementation(project(":libs:utils:config"))
    implementation(project(":libs:utils:logging"))
    implementation(project(":libs:utils:slack"))

    implementation(Ktor.client.auth)
    implementation(Ktor.client.cio)
    implementation(Ktor.client.logging)
    implementation(Ktor.client.serialization)
    implementation(Ktor.client.contentNegotiation)
    implementation(Ktor.plugins.serialization.jackson)

    implementation(Ktor.server.core)

    // test
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
}