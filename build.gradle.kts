import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("co.uzzu.dotenv.gradle")
    id("com.apollographql.apollo") apply false
}

allprojects {
    group = "com.k33.platform"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        // needed for contentful sdk
        maven { url = uri("https://jitpack.io") }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_25.toString()
        targetCompatibility = JavaVersion.VERSION_25.toString()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        environment = env.allVariables()
    }
}
