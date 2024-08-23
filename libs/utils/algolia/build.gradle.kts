plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":libs:utils:config"))
    implementation(project(":libs:utils:logging"))

    implementation(Ktor.plugins.serialization.kotlinx.json)

    implementation("com.algolia:algoliasearch-client-kotlin:_")
}