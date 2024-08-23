package com.k33.platform.utils.algolia

object Algolia {

    object Key {
        const val ObjectID = "objectID"
    }

    @JvmInline
    value class ApplicationId(val value: String)

    @JvmInline
    value class ApiKey(val value: String)

    @Suppress("EnumEntryName")
    enum class Index {
        articles,
    }

    @JvmInline
    value class ObjectID(val value: String)
}
