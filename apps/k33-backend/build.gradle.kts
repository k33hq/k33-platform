plugins {
    application
    kotlin("jvm")
}

dependencies {
    runtimeOnly(kotlin("stdlib"))

    runtimeOnly(project(":libs:services:identity"))
    runtimeOnly(project(":libs:services:user"))
    runtimeOnly(project(":libs:services:payment"))
    runtimeOnly(project(":libs:services:email-subscription"))

    runtimeOnly(project(":libs:utils:cms"))

    runtimeOnly(project(":libs:utils:ktor"))
    runtimeOnly(Ktor.server.netty)
    runtimeOnly(project(":libs:utils:logging:slack-logging"))
    runtimeOnly(project(":libs:utils:logging:gcp-logging"))

    runtimeOnly(project(":libs:apps:invest"))
    runtimeOnly(project(":libs:apps:vault"))
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    applicationName = "k33-backend"
    applicationDefaultJvmArgs = listOf("-Dlogback.configurationFile=logback.gcp.xml")
}