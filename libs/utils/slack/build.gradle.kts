plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:utils:config"))
    implementation(project(":libs:utils:logging"))

    api("com.slack.api:slack-api-model-kotlin-extension:_")
    // FIXME: Workaround until com.slack.api:bolt-ktor upgrades ktor dependency from 2 to 3.
    // api("com.slack.api:bolt-ktor:_")
    api("com.slack.api:bolt:_")
    implementation("com.slack.api:slack-api-client-kotlin-extension:_")

    implementation(Ktor.server.core)
    implementation(Ktor.server.auth)

    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.jdk8)

    // test
    testImplementation("io.kotest:kotest-runner-junit5-jvm:_")
}