import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("co.uzzu.dotenv.gradle")
    id("com.apollographql.apollo3") apply false
}

allprojects {
    group = "com.k33.platform"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        // needed for contentful sdk
        maven { url = uri("https://jitpack.io") }
        // needed for firestore4k
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        environment = env.allVariables()
    }
}

subprojects {
    // Address https://github.com/gradle/gradle/issues/4823: Force parent project evaluation before subproject evaluation for Kotlin build scripts
    if (gradle.startParameter.isConfigureOnDemand
        && buildscript.sourceFile?.extension?.lowercase() == "kts"
        && parent != rootProject) {
        generateSequence(parent) { project -> project.parent.takeIf { it != rootProject } }
            .forEach { evaluationDependsOn(it.path) }
    }
}