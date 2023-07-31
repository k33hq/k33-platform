plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation("com.contentful.java:cma-sdk:_")

    implementation(project(":libs:utils:cms:contentful"))
    implementation("net.andreinc:mapneat:_")
    implementation("com.jayway.jsonpath:json-path:_")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:_")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:_")
    implementation("org.apache.logging.log4j:log4j-core:_")
    implementation(KotlinX.serialization.json)

    implementation(kotlin("stdlib"))
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.jdk8)

    implementation(project(":libs:utils:logging"))
    implementation(project(":libs:utils:config"))
}