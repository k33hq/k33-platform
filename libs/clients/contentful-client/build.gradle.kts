plugins {
    kotlin("jvm")
    id("com.apollographql.apollo")
}

apollo {
    service("invest") {
        srcDir("src/main/httpx/invest")
        packageName.set("com.k33.platform.cms.space.invest")
    }
    service("research") {
        srcDir("src/main/httpx/research")
        packageName.set("com.k33.platform.cms.space.research")
    }
}

dependencies {
    implementation("com.apollographql.apollo:apollo-runtime:_")
}