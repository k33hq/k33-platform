plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:services:email"))

    implementation(project(":libs:services:identity"))
    implementation(project(":libs:services:user:user-service"))

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)
    implementation(Ktor.plugins.serialization.kotlinx.json)
}
