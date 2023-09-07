plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:services:user:user-service"))
    implementation(project(":libs:utils:firebase-auth"))
    implementation(project(":libs:services:email"))
    implementation(project(":libs:utils:analytics"))
    implementation(project(":libs:utils:logging"))
    implementation(project(":libs:utils:config"))
    implementation("com.stripe:stripe-java:_")
    implementation("com.google.code.gson:gson:_")

    implementation(Ktor.server.core)

    // test
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
}