plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(Kotlin.stdlib)
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.jdk8)

    implementation(project(":libs:utils:logging"))
    implementation(project(":libs:utils:config"))

    implementation("com.stripe:stripe-java:_")
}