plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(project(":libs:utils:firebase-auth"))
    implementation(project(":libs:services:user:user-model"))
    implementation(project(":libs:apps:invest"))
}