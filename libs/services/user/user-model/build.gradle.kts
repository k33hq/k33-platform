plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api("io.firestore4k:typed-api:_")  {
        exclude("com.google.guava", "listenablefuture")
    }
}
