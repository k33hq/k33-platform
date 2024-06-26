plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:utils:firebase-auth"))
    implementation(project(":libs:utils:logging"))

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)

    implementation(KotlinX.serialization.json)
}