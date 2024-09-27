plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:utils:fireblocks"))

    implementation("com.github.librepdf:openpdf:_")

    implementation(project(":libs:utils:stripe"))
    implementation(project(":libs:utils:firebase-auth"))
    implementation(project(":libs:utils:file-store"))

    implementation(project(":libs:utils:config"))
    implementation(project(":libs:utils:logging"))
    implementation(project(":libs:utils:slack"))
    implementation(project(":libs:utils:ktor"))

    implementation(project(":libs:services:identity"))
    implementation(project(":libs:services:user:user-model"))

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)

    implementation(Ktor.client.cio)
    implementation(Ktor.client.logging)
    implementation(Ktor.client.contentNegotiation)
    implementation(Ktor.plugins.serialization.kotlinx.json)

    implementation(KotlinX.serialization.core)
    implementation(KotlinX.serialization.json)

    implementation(Arrow.core)

    // test
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
    testImplementation(Testing.kotest.framework.datatest)
}