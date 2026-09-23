plugins {
    kotlin("jvm")
    id("com.apollographql.apollo")
}

apollo {
    service("research") {
        srcDir("src/main/httpx/research")
        packageName.set("com.k33.platform.cms.space.research")
    }
}

dependencies {
    implementation("com.apollographql.apollo:apollo-runtime:_")
}