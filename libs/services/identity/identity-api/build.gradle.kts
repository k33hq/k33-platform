plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(Ktor.server.auth)
}