plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    implementation(project(":libs:services:payment:stripe"))
    implementation(project(":libs:services:identity"))
    implementation(project(":libs:services:user:user-service"))
    implementation(project(":libs:utils:analytics"))
    implementation(project(":libs:utils:logging"))

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)
}