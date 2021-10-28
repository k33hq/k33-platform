plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("io.ktor:ktor-server-core:${Version.ktor}")
    implementation("io.ktor:ktor-auth:${Version.ktor}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinSerialization}")
}