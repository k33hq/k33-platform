plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api("com.stripe:stripe-java:_")
    implementation(project(":libs:utils:logging"))
    implementation(Ktor.server.core)
}